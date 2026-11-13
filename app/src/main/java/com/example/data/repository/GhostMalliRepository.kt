package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.AssistantMode
import com.example.data.model.ChatMessage
import com.example.data.model.CodeSnippet
import com.example.data.model.Conversation
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GhostMalliRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun conversationsCollection(uid: String) =
        db.collection("users").document(uid).collection("conversations")

    private fun messagesCollection(uid: String, conversationId: String) =
        conversationsCollection(uid).document(conversationId).collection("messages")

    private fun snippetsCollection(uid: String) =
        db.collection("users").document(uid).collection("snippets")

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/conversations"
        emitAll(
            conversationsCollection(uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        require(conversationId.isNotBlank()) { "conversationId must not be blank" }
        val path = "users/$uid/conversations/$conversationId/messages"
        emitAll(
            messagesCollection(uid, conversationId)
                .orderBy("sequenceOrder", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sequenceOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeSnippets(): Flow<List<CodeSnippet>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/snippets"
        emitAll(
            snippetsCollection(uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CodeSnippet::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun createConversation(
        title: String,
        mode: AssistantMode,
        language: String,
        modelName: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = conversationsCollection(uid).document()
            val safeTitle = title.trim().ifBlank { "New Shadow Session" }.take(120)
            val payload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "mode" to mode.wireName,
                "language" to language.take(40).ifBlank { "Kotlin" },
                "modelName" to modelName.take(80).ifBlank { "gemini-3.8-flash" },
                "lastMessagePreview" to "",
                "messageCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val path = auth.currentUser?.uid?.let { "users/$it/conversations" }
            handleFirestoreError(e, OperationType.CREATE, path, auth)
            Result.failure(e)
        }
    }

    suspend fun appendMessage(
        conversationId: String,
        role: String,
        content: String,
        language: String,
        mode: AssistantMode,
        sequenceOrder: Long,
        newTitleIfFirst: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val convRef = conversationsCollection(uid).document(conversationId)
        val msgRef = messagesCollection(uid, conversationId).document()
        val safeContent = content.take(60000).ifBlank { " " }
        val preview = safeContent.replace('\n', ' ').trim().take(240)

        return try {
            val msgPayload = mapOf(
                "userId" to uid,
                "conversationId" to conversationId,
                "role" to if (role == "model") "model" else "user",
                "content" to safeContent,
                "language" to language.take(40),
                "toolMode" to mode.wireName,
                "sequenceOrder" to sequenceOrder,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(msgPayload).await()

            val updateMap = mutableMapOf<String, Any>(
                "lastMessagePreview" to preview,
                "messageCount" to (sequenceOrder + 1L),
                "mode" to mode.wireName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!newTitleIfFirst.isNullOrBlank()) {
                updateMap["title"] = newTitleIfFirst.trim().take(120)
            }
            convRef.update(updateMap).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgRef = messagesCollection(uid, conversationId).document(messageId)
        return try {
            msgRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, msgRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val convRef = conversationsCollection(uid).document(conversationId)
        return try {
            val messages = messagesCollection(uid, conversationId).get().await()
            if (!messages.isEmpty) {
                val batch = db.batch()
                messages.documents.forEach { doc -> batch.delete(doc.reference) }
                batch.delete(convRef)
                batch.commit().await()
            } else {
                convRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, convRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun clearAllConversations(): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/conversations"
        return try {
            val conversations = conversationsCollection(uid).get().await()
            for (convDoc in conversations.documents) {
                deleteConversation(convDoc.id).getOrThrow()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path, auth)
            Result.failure(e)
        }
    }

    suspend fun saveSnippet(
        snippetId: String? = null,
        title: String,
        fileName: String,
        language: String,
        code: String,
        summary: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = if (snippetId.isNullOrBlank()) {
            snippetsCollection(uid).document()
        } else {
            snippetsCollection(uid).document(snippetId)
        }
        val safeTitle = title.trim().ifBlank { "Untitled Script" }.take(120)
        val safeFile = fileName.trim().ifBlank { "script.txt" }.take(120)
        val safeLang = language.trim().ifBlank { "Kotlin" }.take(40)
        val safeCode = code.take(100000)
        val safeSummary = summary.trim().take(500)

        return try {
            if (snippetId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "title" to safeTitle,
                    "fileName" to safeFile,
                    "language" to safeLang,
                    "code" to safeCode,
                    "summary" to safeSummary,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updatePayload = mapOf(
                    "title" to safeTitle,
                    "fileName" to safeFile,
                    "language" to safeLang,
                    "code" to safeCode,
                    "summary" to safeSummary,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (snippetId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                docRef.path,
                auth
            )
            Result.failure(e)
        }
    }

    suspend fun deleteSnippet(snippetId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = snippetsCollection(uid).document(snippetId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
