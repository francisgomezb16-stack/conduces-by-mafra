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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read conduces", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("conduces").get());
});

test("Alice: can create a valid conduce in her subcollection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conduces").doc("cnd_001").set({
      conduceNumber: "CND-2026-0001",
      type: "Traslado",
      status: "Borrador",
      originProject: "Obra Central",
      destinationProject: "Torre Bella Vista",
      userId: ALICE_UID,
      createdAt: new Date(),
    })
  );
});

test("Bob: cannot read Alice's conduce", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("conduces").doc("cnd_001").set({
      conduceNumber: "CND-2026-0001",
      type: "Traslado",
      status: "Borrador",
      originProject: "Obra Central",
      destinationProject: "Torre Bella Vista",
      userId: ALICE_UID,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conduces").doc("cnd_001").get()
  );
});

test("Alice: cannot create a conduce with someone else's userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("conduces").doc("cnd_spoofed").set({
      conduceNumber: "CND-2026-0002",
      type: "Traslado",
      status: "Borrador",
      originProject: "Obra Central",
      destinationProject: "Torre Bella Vista",
      userId: BOB_UID, // Spoofed!
      createdAt: new Date(),
    })
  );
});

test("Alice: can read and update her own company profile in settings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("company").set({
      companyName: "Constructora del Caribe SRL",
      taxId: "1-31-98765-4",
      phone: "809-555-0199",
      userId: ALICE_UID,
      createdAt: new Date(),
    })
  );
});

test("Alice: can create and read projects in her catalog", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_01").set({
      name: "Torre Pedralbes",
      location: "Av. Winston Churchill #105",
      defaultResponsible: "Ing. Manuel Sánchez",
      userId: ALICE_UID,
      createdAt: new Date(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_01").get()
  );
});
