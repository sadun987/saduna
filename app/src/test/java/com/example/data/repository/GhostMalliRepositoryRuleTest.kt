package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AssistantMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhostMalliRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createConversationAndAppendMessage_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GhostMalliRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createConversation(
                title = "Kotlin Flow Session",
                mode = AssistantMode.CHAT,
                language = "Kotlin",
                modelName = "gemini-3.8-flash"
            )
        }
        assertTrue(createResult.isSuccess)
        val convId = createResult.getOrThrow()
        assertTrue(convId.isNotBlank())

        val appendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.appendMessage(
                conversationId = convId,
                role = "user",
                content = "Explain StateFlow in Sinhala",
                language = "Kotlin",
                mode = AssistantMode.EXPLAIN,
                sequenceOrder = 0L,
                newTitleIfFirst = "Explain StateFlow"
            )
        }
        assertTrue(appendResult.isSuccess)

        val conversations = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeConversations().first { list -> list.any { it.id == convId } }
        }
        assertTrue(conversations.any { it.id == convId && it.title == "Explain StateFlow" })

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(convId).first { it.isNotEmpty() }
        }
        assertEquals(1, messages.size)
        assertEquals("Explain StateFlow in Sinhala", messages.first().content)
    }

    @Test
    fun saveAndObserveSnippets_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = GhostMalliRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSnippet(
                title = "Binary Search",
                fileName = "binary_search.py",
                language = "Python",
                code = "def binary_search(arr, target):\n    return -1",
                summary = "Python binary search template"
            )
        }
        assertTrue(saveResult.isSuccess)
        val snippetId = saveResult.getOrThrow()

        val snippets = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSnippets().first { list -> list.any { it.id == snippetId } }
        }
        assertTrue(snippets.any { it.id == snippetId && it.fileName == "binary_search.py" })
    }

    @Test
    fun observeConversations_unauthenticatedUser_failsFastWithIllegalState() = runBlocking {
        auth.signOut()
        val repository = GhostMalliRepository(firestore, auth)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repository.observeConversations().first() }
        } catch (t: Throwable) {
            caught = t
        }
        assertTrue(caught is IllegalStateException)
    }

    @Test
    fun directCrossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GhostMalliRepository(firestore, auth)
        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createConversation("Private Session", AssistantMode.CHAT, "Python", "gemini-3.8-flash").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var firestoreException: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("conversations").document(convId)
                    .get()
                    .await()
            }
        } catch (e: Throwable) {
            firestoreException = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_ghost@test.com"
        const val BOB_EMAIL = "bob_ghost@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
