package com.saferouteai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saferouteai.data.local.dao.JourneySessionDao
import com.saferouteai.data.local.dao.TrustedContactDao
import com.saferouteai.data.local.dao.UserProfileDao
import com.saferouteai.data.local.entity.JourneySessionEntity
import com.saferouteai.data.local.entity.TrustedContactEntity
import com.saferouteai.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        TrustedContactEntity::class,
        JourneySessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SafeRouteDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun journeySessionDao(): JourneySessionDao

    companion object {
        @Volatile
        private var INSTANCE: SafeRouteDatabase? = null

        fun getInstance(context: Context): SafeRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeRouteDatabase::class.java,
                    "saferoute_ai.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
