package com.thaibanai.multillmchat.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thaibanai.multillmchat.data.local.AppDatabase
import com.thaibanai.multillmchat.data.local.SecureStorage
import com.thaibanai.multillmchat.data.local.dao.AttachmentDao
import com.thaibanai.multillmchat.data.local.dao.ConversationDao
import com.thaibanai.multillmchat.data.local.dao.MessageDao
import com.thaibanai.multillmchat.data.remote.StreamingService
import com.thaibanai.multillmchat.data.repository.ChatRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "multillm_chat_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    @Named("openai")
    fun provideOpenAiHttpClient(): OkHttpClient = createBaseHttpClient()

    @Provides
    @Singleton
    @Named("anthropic")
    fun provideAnthropicHttpClient(): OkHttpClient = createBaseHttpClient()

    @Provides
    @Singleton
    @Named("deepseek")
    fun provideDeepSeekHttpClient(): OkHttpClient = createBaseHttpClient()

    @Provides
    @Singleton
    fun provideStreamingService(
        gson: Gson,
        @Named("openai") openAiClient: OkHttpClient,
        @Named("anthropic") anthropicClient: OkHttpClient,
        @Named("deepseek") deepSeekClient: OkHttpClient
    ): StreamingService = StreamingService(gson, openAiClient, anthropicClient, deepSeekClient)

    @Provides
    @Singleton
    fun provideChatRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        streamingService: StreamingService,
        secureStorage: SecureStorage
    ): ChatRepository = ChatRepository(
        conversationDao,
        messageDao,
        streamingService,
        secureStorage
    )

    private fun createBaseHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }
}
