package com.example.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.ChatMessage
import com.example.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

class ChatRepository(private val chatDao: ChatDao, private val context: Context? = null) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions().map { entities ->
        entities.map { entity ->
            ChatSession(
                id = entity.id,
                title = entity.title,
                messages = emptyList(), // Messages will be loaded reactively on-demand when selected
                isPinned = entity.isPinned,
                timestamp = entity.timestamp
            )
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { entity ->
                val bitmapFile = context?.let { ctx ->
                    val dir = File(ctx.filesDir, "chat_images")
                    File(dir, "${entity.id}.png")
                }
                val bitmap = if (bitmapFile?.exists() == true) {
                    try {
                        BitmapFactory.decodeFile(bitmapFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } else null

                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    isUser = entity.isUser,
                    bitmap = bitmap,
                    attachedFile = null
                )
            }
        }
    }

    suspend fun getMessagesForSessionSync(sessionId: String): List<ChatMessage> {
        return chatDao.getMessagesForSessionSync(sessionId).map { entity ->
            val bitmapFile = context?.let { ctx ->
                val dir = File(ctx.filesDir, "chat_images")
                File(dir, "${entity.id}.png")
            }
            val bitmap = if (bitmapFile?.exists() == true) {
                try {
                    BitmapFactory.decodeFile(bitmapFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else null

            ChatMessage(
                id = entity.id,
                text = entity.text,
                isUser = entity.isUser,
                bitmap = bitmap,
                attachedFile = null
            )
        }
    }

    suspend fun saveSession(session: ChatSession) {
        chatDao.insertSession(
            ChatSessionEntity(
                id = session.id,
                title = session.title,
                isPinned = session.isPinned,
                timestamp = session.timestamp
            )
        )
    }

    suspend fun updateSessionPin(sessionId: String, isPinned: Boolean) {
        chatDao.updatePinStatus(sessionId, isPinned)
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) {
        chatDao.updateSessionTitle(sessionId, title)
    }

    suspend fun saveMessage(sessionId: String, message: ChatMessage) {
        chatDao.insertMessage(
            ChatMessageEntity(
                id = message.id,
                sessionId = sessionId,
                text = message.text,
                isUser = message.isUser,
                timestamp = System.currentTimeMillis()
            )
        )
        message.bitmap?.let { bmp ->
            context?.let { ctx ->
                try {
                    val dir = File(ctx.filesDir, "chat_images")
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = File(dir, "${message.id}.png")
                    FileOutputStream(file).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
        chatDao.deleteMessagesForSession(sessionId)
    }
}
