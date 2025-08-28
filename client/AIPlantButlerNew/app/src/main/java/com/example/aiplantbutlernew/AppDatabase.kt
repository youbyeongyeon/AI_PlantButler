package com.example.aiplantbutlernew

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 1. 데이터베이스의 구성요소를 정의하는 어노테이션
@Database(entities = [ChatRoom::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 2. 데이터 관리자(DAO)를 사용할 수 있도록 통로를 열어줍니다.
    abstract fun chatDao(): ChatDao

    // 3. 앱 전체에서 데이터베이스 인스턴스가 단 하나만 존재하도록 보장하는 코드 (싱글턴 패턴)
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "plant_butler_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}