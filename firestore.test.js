const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write savings_rows or configs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savings_rows").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .doc("row_01")
      .set({
        userId: ALICE_UID,
        rowIndex: 1,
        tabungan: "Tabungan 1",
        nominal: 50000,
        tanggal: "22/11/2026",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("configs")
      .doc("target")
      .get()
  );
});

test("Owner can create, partial-update, list, and delete savings_rows", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const rowRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("savings_rows")
    .doc("row_01");

  await assertSucceeds(
    rowRef.set({
      userId: ALICE_UID,
      rowIndex: 1,
      tabungan: "Setoran Awal",
      nominal: 50000,
      tanggal: "22/11/2026",
      keterangan: "Transfer bank BCA",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List matches repository observeSavingsRows()
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .orderBy("rowIndex")
      .get()
  );

  // Partial update preserving createdAt and userId, including keterangan
  await assertSucceeds(
    rowRef.update({
      rowIndex: 1,
      tabungan: "Setoran Pertama",
      nominal: 100000,
      tanggal: "23/11/2026",
      keterangan: "Setoran tunai",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete allowed for owner
  await assertSucceeds(rowRef.delete());
});

test("Rejects invalid savings_rows payloads, ghost fields, future timestamps, and immutable overrides", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const rowsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("savings_rows");

  // Extra @DocumentId 'id' field must fail
  await assertFails(
    rowsCol.doc("row_02").set({
      id: "row_02",
      userId: ALICE_UID,
      rowIndex: 2,
      tabungan: "Ghost ID",
      nominal: 25000,
      tanggal: "22/11/2026",
      keterangan: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Oversized keterangan (> 200 chars) must fail
  await assertFails(
    rowsCol.doc("row_02").set({
      userId: ALICE_UID,
      rowIndex: 2,
      tabungan: "Panjang",
      nominal: 25000,
      tanggal: "22/11/2026",
      keterangan: "x".repeat(201),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Negative nominal must fail
  await assertFails(
    rowsCol.doc("row_03").set({
      userId: ALICE_UID,
      rowIndex: 3,
      tabungan: "Negatif",
      nominal: -5000,
      tanggal: "22/11/2026",
      keterangan: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Future timestamp must fail
  await assertFails(
    rowsCol.doc("row_04").set({
      userId: ALICE_UID,
      rowIndex: 4,
      tabungan: "Masa Depan",
      nominal: 10000,
      tanggal: "22/11/2026",
      keterangan: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Valid create, then try mutating createdAt or userId
  await assertSucceeds(
    rowsCol.doc("row_05").set({
      userId: ALICE_UID,
      rowIndex: 5,
      tabungan: "Valid",
      nominal: 50000,
      tanggal: "22/11/2026",
      keterangan: "Catatan awal",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    rowsCol.doc("row_05").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    rowsCol.doc("row_05").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation prevents Bob from reading or modifying Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date(Date.now() - 10000);
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .doc("row_01")
      .set({
        userId: ALICE_UID,
        rowIndex: 1,
        tabungan: "Rahasia Alice",
        nominal: 75000,
        tanggal: "22/11/2026",
        createdAt: now,
        updatedAt: now,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .doc("row_01")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .doc("row_01")
      .update({
        tabungan: "Dibajak",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("savings_rows")
      .doc("row_01")
      .delete()
  );
});

test("Owner can create and update TargetConfig; invalid config rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 10000);
  const cfgRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("configs")
    .doc("target");

  await assertSucceeds(
    cfgRef.set({
      userId: ALICE_UID,
      title: "Target Tabungan",
      subtitle: "Dana Liburan",
      targetAmount: 3000000,
      totalDataRows: 60,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    cfgRef.update({
      subtitle: "Dana Darurat",
      targetAmount: 6000000,
      totalDataRows: 60,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    cfgRef.update({
      totalDataRows: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
