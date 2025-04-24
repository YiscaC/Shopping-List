package com.example.shoppinglist.data.local

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.shoppinglist.data.local.converters.ParticipantsConverter
import com.example.shoppinglist.data.local.dao.ShoppingItemDao
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.dao.UserDao
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import androidx.room.migration.Migration
import com.example.shoppinglist.data.local.converters.MessagesConverter


@Database(
    entities = [ShoppingListEntity::class, ShoppingItemEntity::class, UserEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(ParticipantsConverter::class, MessagesConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun userDao(): UserDao

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
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shopping_lists ADD COLUMN ownerId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE shopping_lists ADD COLUMN participants TEXT NOT NULL DEFAULT '{}'")
            }
        }
    }
}
