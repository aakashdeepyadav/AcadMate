package com.acadmate.ai.tutor

import android.content.Context
import androidx.room.Room
import com.google.ai.client.generativeai.GenerativeModel
import com.acadmate.ai.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiTutorModule {

    @Provides
    @Singleton
    fun provideAiTutorDatabase(@ApplicationContext context: Context): AiTutorDatabase {
        return Room.databaseBuilder(
            context,
            AiTutorDatabase::class.java,
            "ai_tutor_db"
        ).build()
    }

    @Provides
    fun provideChatDao(database: AiTutorDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = "gemini-3.1-flash-lite",
            apiKey = "AIzaSyAvL3Sa8PEN2N4TvN220y3QELJNXbM9W_I"
        )
    }
}
