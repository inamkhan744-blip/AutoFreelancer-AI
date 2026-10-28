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

const sampleTask = {
  title: "Scouting for SEO clients on LinkedIn",
  category: "Client Acquisition",
  workerName: "Apollo-Scout",
  workerRole: "Lead Generation Specialist",
  status: "PENDING",
  priority: "HIGH",
  client: "LinkedIn Prospects",
  estimatedValue: 1200,
  details: "Search for VP Marketing at Series A SaaS startups and draft personalized audit teasers.",
  rejectionReason: null,
  logs: ["Agent initialized", "Filtered 45 prospect profiles"],
  createdAt: new Date(),
  updatedAt: new Date(),
};

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Authenticated user: cannot read another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("task_1").set(sampleTask);
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("task_1").get());
});

test("Authenticated user: can create a valid task in their own collection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(sampleTask)
  );
});

test("Authenticated user: cannot create task with missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const invalidTask = { ...sampleTask };
  delete invalidTask.title;
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_invalid").set(invalidTask)
  );
});

test("Authenticated user: can update status (approve/reject) of their task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set(sampleTask);
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      status: "APPROVED",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot update another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").set(sampleTask);
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("tasks").doc("task_bob").update({
      status: "APPROVED",
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: can store API keys in their own settings document", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("api_keys").set({
      gemini_api_key: "gemini-test",
      openrouter_api_key: "openrouter-test",
      serper_api_key: "serper-test",
      tavily_api_key: "tavily-test",
      elevenlabs_api_key: "elevenlabs-test",
      brevo_api_key: "brevo-test",
      payment_link_url: "https://example.test/pay",
    })
  );
});

test("Authenticated user: cannot read or write another user's API keys", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobKeys = aliceDb.collection("users").doc(BOB_UID).collection("settings").doc("api_keys");
  await assertFails(bobKeys.get());
  await assertFails(bobKeys.set({ gemini_api_key: "stolen" }));
});

test("Unauthenticated user: cannot access API keys", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("settings").doc("api_keys").get()
  );
});
