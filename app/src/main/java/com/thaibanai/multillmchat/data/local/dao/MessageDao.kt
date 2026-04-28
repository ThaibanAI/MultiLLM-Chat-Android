package com.thaibanai.multillmchat.data.local.dao

import androidx.room.*
import com.thaibanai.multillmchat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY orderIndex ASC, createdAt ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)

    @Query("SELECT MAX(orderIndex) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMaxOrderIndex(conversationId: String): Int?

    @Query("""
        UPDATE messages 
        SET content = :content, isStreaming = :isStreaming, isLoading = :isLoading, error = :error 
        WHERE id = :id
    """)
    suspend fun updateMessageContent(
        id: String,
        content: String,
        isStreaming: Boolean,
        isLoading: Boolean,
        error: String?
    )
}
