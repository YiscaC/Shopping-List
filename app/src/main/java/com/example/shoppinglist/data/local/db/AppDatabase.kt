package com.example.shoppinglist.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shoppinglist.data.local.converters.ParticipantsConverter
import com.example.shoppinglist.data.local.dao.ShoppingItemDao
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import androidx.room.migration.Migration


@Database(entities = [ShoppingListEntity::class, ShoppingItemEntity::class], version = 4, exportSchema = false)
@TypeConverters(ParticipantsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_list_db"
                )
                    .addMigrations(MIGRATION_3_4) // ✅ הוספת מיגרציה לגרסה 4
                    .fallbackToDestructiveMigration() // ✅ הרס המידע במקרה של בעיה במיגרציה
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // ✅ מיגרציה מגרסה 3 לגרסה 4 - הוספת ownerId + שינוי participants למחרוזת JSON
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shopping_lists ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE shopping_lists ADD COLUMN participants TEXT NOT NULL DEFAULT '{}'")
            }
        }
    }
}
