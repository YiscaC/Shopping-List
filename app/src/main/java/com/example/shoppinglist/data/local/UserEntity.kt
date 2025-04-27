package com.example.shoppinglist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey var uid: String = "",
    var username: String = "",
    var firstName: String = "",
    var phone: String = "",
    var localProfileImagePath: String? = null,
    val remoteProfileImageUrl: String? = null // ✅ חדש

) {
    // ✅ קונסטרקטור ריק ש־Firebase צריך
    constructor() : this("", "", "", "", null)
}
