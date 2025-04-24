package com.example.shoppinglist.data.local.dao

import androidx.room.*
import com.example.shoppinglist.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE uid IN (:uids)")
    suspend fun getUsersByUidsOnce(uids: List<String>): List<UserEntity>



}
