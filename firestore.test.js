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

function pastDate() {
  return new Date(Date.now() - 1000);
}

function validConversation(uid) {
  const now = pastDate();
  return {
    userId: uid,
    title: "Kotlin Coroutines Help",
    mode: "chat",
    language: "Kotlin",
    modelName: "gemini-3.8-flash",
    lastMessagePreview: "How does StateFlow work?",
    messageCount: 1,
    createdAt: now,
    updatedAt: now,
  };
}

function validMessage(uid, conversationId) {
  return {
    userId: uid,
    conversationId,
    role: "user",
    content: "Explain StateFlow in Kotlin",
    language: "Kotlin",
    toolMode: "chat",
    sequenceOrder: 0,
    createdAt: pastDate(),
  };
}

function validSnippet(uid) {
  const now = pastDate();
  return {
    userId: uid,
    title: "Hello World",
    fileName: "Main.kt",
    language: "Kotlin",
    code: "fun main() { println(\"Hello\") }",
    summary: "Simple Kotlin entry point",
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or write conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set(validConversation(ALICE_UID))
  );
});

test("Authenticated owner can create, read, update, and delete conversation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("conversations")
    .doc("conv_1");

  await assertSucceeds(convRef.set(validConversation(ALICE_UID)));
  await assertSucceeds(convRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
  await assertSucceeds(
    convRef.update({
      title: "Updated Title",
      messageCount: 2,
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(convRef.delete());
});

test("Cross-user access to conversations and snippets is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set(validConversation(ALICE_UID))
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("conversations").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("snippets")
      .doc("snip_1")
      .set(validSnippet(BOB_UID))
  );
});

test("Shadow update with extra ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_ghost")
      .set({
        ...validConversation(ALICE_UID),
        isAdmin: true,
      })
  );
});

test("Spoofed userId on create is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_spoof")
      .set(validConversation(BOB_UID))
  );
});

test("Messages require existing parent conversation and valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const convRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("conversations")
    .doc("conv_1");
  const msgRef = convRef.collection("messages").doc("msg_1");

  // Fails without parent conversation
  await assertFails(msgRef.set(validMessage(ALICE_UID, "conv_1")));

  // Create parent first, then message succeeds
  await assertSucceeds(convRef.set(validConversation(ALICE_UID)));
  await assertSucceeds(msgRef.set(validMessage(ALICE_UID, "conv_1")));
  await assertSucceeds(convRef.collection("messages").get());
});

test("Snippets CRUD succeeds for owner and rejects invalid mode/empty title", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const snipRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("snippets")
    .doc("snip_1");

  await assertSucceeds(snipRef.set(validSnippet(ALICE_UID)));
  await assertSucceeds(
    snipRef.update({
      code: "fun main() { println(\"Updated\") }",
      updatedAt: pastDate(),
    })
  );

  // Empty title fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("snippets")
      .doc("snip_bad")
      .set({
        ...validSnippet(ALICE_UID),
        title: "",
      })
  );
});
