package com.thaibanai.multillmchat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId")]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val messageId: String,
    val type: String, // "image", "pdf", "txt", "docx"
    val fileName: String,
    val fileSize: Long,
    val localPath: String? = null, // path for images
    val extractedText: String? = null, // for documents
    val mimeType: String,
    val thumbnailPath: String? = null // thumbnail for images
)
