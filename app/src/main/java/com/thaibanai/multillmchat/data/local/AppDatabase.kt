package com.thaibanai.multillmchat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thaibanai.multillmchat.data.local.dao.AttachmentDao
import com.thaibanai.multillmchat.data.local.dao.ConversationDao
import com.thaibanai.multillmchat.data.local.dao.MessageDao
import com.thaibanai.multillmchat.data.local.entity.AttachmentEntity
import com.thaibanai.multillmchat.data.local.entity.ConversationEntity
import com.thaibanai.multillmchat.data.local.entity.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
}
