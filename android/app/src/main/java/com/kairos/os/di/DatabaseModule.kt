package com.kairos.os.di

import android.content.Context
import androidx.room.Room
import com.kairos.os.data.db.KairosDatabase
import com.kairos.os.data.db.LocalNotificationDao
import com.kairos.os.data.db.LocalNoteDao
import com.kairos.os.data.db.LocalAlarmDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KairosDatabase {
        return Room.databaseBuilder(
            context,
            KairosDatabase::class.java,
            "kairos_local.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideLocalNotificationDao(db: KairosDatabase): LocalNotificationDao {
        return db.localNotificationDao()
    }

    @Provides
    fun provideLocalNoteDao(db: KairosDatabase): LocalNoteDao {
        return db.localNoteDao()
    }

    @Provides
    fun provideLocalAlarmDao(db: KairosDatabase): LocalAlarmDao {
        return db.localAlarmDao()
    }
}
