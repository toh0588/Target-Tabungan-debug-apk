package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SavingsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses data.")
        check(uid == expectedUserId) { "Akun pengguna berubah; operasi dibatalkan." }
        return uid
    }

    fun observeSavingsRows(userId: String): Flow<List<SavingsRow>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/savings_rows"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("savings_rows")
                .orderBy("rowIndex")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavingsRow::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTargetConfig(userId: String): Flow<TargetConfig?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/configs/target"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("configs")
                .document("target")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            TargetConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveRow(
        userId: String,
        rowIndex: Int,
        tabungan: String,
        nominal: Long,
        tanggal: String,
    ): Result<Unit> {
        val rowId = docIdForRow(rowIndex)
        val path = "users/$userId/savings_rows/$rowId"
        return try {
            val uid = requireUserId(userId)
            require(rowIndex in 1..MAX_DATA_ROWS) { "Nomor baris harus 1..$MAX_DATA_ROWS" }
            val cleanTabungan = tabungan.trim().take(120)
            val cleanNominal = nominal.coerceIn(0L, MAX_NOMINAL)
            val cleanTanggal = tanggal.trim().take(20)

            val docRef = db.collection("users")
                .document(uid)
                .collection("savings_rows")
                .document(rowId)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    tx.update(
                        docRef,
                        mapOf(
                            "rowIndex" to rowIndex.toLong(),
                            "tabungan" to cleanTabungan,
                            "nominal" to cleanNominal,
                            "tanggal" to cleanTanggal,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    tx.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "rowIndex" to rowIndex.toLong(),
                            "tabungan" to cleanTabungan,
                            "nominal" to cleanNominal,
                            "tanggal" to cleanTanggal,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearRow(userId: String, rowIndex: Int): Result<Unit> {
        val rowId = docIdForRow(rowIndex)
        val path = "users/$userId/savings_rows/$rowId"
        return try {
            val uid = requireUserId(userId)
            require(rowIndex in 1..MAX_DATA_ROWS) { "Nomor baris tidak valid" }
            db.collection("users")
                .document(uid)
                .collection("savings_rows")
                .document(rowId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun batchFillRows(
        userId: String,
        rows: List<SavingsRowInput>,
    ): Result<Unit> {
        val colPath = "users/$userId/savings_rows"
        return try {
            val uid = requireUserId(userId)
            val colRef = db.collection("users").document(uid).collection("savings_rows")
            val existingIds = colRef.get().await().documents.map { it.id }.toSet()

            val batch = db.batch()
            rows.forEach { input ->
                require(input.rowIndex in 1..MAX_DATA_ROWS) { "Nomor baris tidak valid" }
                val rowId = docIdForRow(input.rowIndex)
                val docRef = colRef.document(rowId)
                val cleanTabungan = input.tabungan.trim().take(120)
                val cleanNominal = input.nominal.coerceIn(0L, MAX_NOMINAL)
                val cleanTanggal = input.tanggal.trim().take(20)

                if (rowId in existingIds) {
                    batch.update(
                        docRef,
                        mapOf(
                            "rowIndex" to input.rowIndex.toLong(),
                            "tabungan" to cleanTabungan,
                            "nominal" to cleanNominal,
                            "tanggal" to cleanTanggal,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    batch.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "rowIndex" to input.rowIndex.toLong(),
                            "tabungan" to cleanTabungan,
                            "nominal" to cleanNominal,
                            "tanggal" to cleanTanggal,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun clearAllRows(userId: String): Result<Unit> {
        val colPath = "users/$userId/savings_rows"
        return try {
            val uid = requireUserId(userId)
            val colRef = db.collection("users").document(uid).collection("savings_rows")
            val snapshot = colRef.get().await()
            if (!snapshot.isEmpty) {
                val batch = db.batch()
                snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colPath)
            Result.failure(e)
        }
    }

    suspend fun saveTargetConfig(
        userId: String,
        title: String,
        subtitle: String,
        targetAmount: Long,
        totalDataRows: Int,
    ): Result<Unit> {
        val path = "users/$userId/configs/target"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim().ifBlank { "Target Tabungan" }.take(100)
            val cleanSubtitle = subtitle.trim().take(150)
            val cleanTarget = targetAmount.coerceIn(0L, MAX_NOMINAL)
            val cleanRows = totalDataRows.coerceIn(1, MAX_DATA_ROWS)

            val docRef = db.collection("users")
                .document(uid)
                .collection("configs")
                .document("target")

            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                if (snap.exists()) {
                    tx.update(
                        docRef,
                        mapOf(
                            "title" to cleanTitle,
                            "subtitle" to cleanSubtitle,
                            "targetAmount" to cleanTarget,
                            "totalDataRows" to cleanRows.toLong(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                } else {
                    tx.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "title" to cleanTitle,
                            "subtitle" to cleanSubtitle,
                            "targetAmount" to cleanTarget,
                            "totalDataRows" to cleanRows.toLong(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
