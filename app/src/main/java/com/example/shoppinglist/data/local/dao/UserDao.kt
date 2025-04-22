package com.example.shoppinglist.data.local.dao

import androidx.room.*
import com.example.shoppinglist.data.local.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)
}
