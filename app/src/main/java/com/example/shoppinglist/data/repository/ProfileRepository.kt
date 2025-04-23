package com.example.shoppinglist.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ProfileRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val roomDb = AppDatabase.getDatabase(context)

    fun getCurrentUser() = auth.currentUser

    suspend fun getLocalUser(uid: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            roomDb.userDao().getUserById(uid)
        }
    }

    suspend fun insertUser(user: UserEntity) {
        withContext(Dispatchers.IO) {
            roomDb.userDao().insertUser(user)
        }
    }

    fun getUserData(callback: (String?, String?, String?, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value as? String
                val firstName = snapshot.child("firstName").value as? String
                val phone = snapshot.child("phone").value as? String
                val profileImageUrl = snapshot.child("profileImageUrl").value as? String
                callback(username, firstName, phone, profileImageUrl)
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to load user info", it)
                callback(null, null, null, null)
            }
    }

    fun updateProfile(username: String, firstName: String, phone: String, callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val userMap = mapOf(
            "username" to username,
            "firstName" to firstName,
            "phone" to phone
        )

        db.getReference("users").child(uid).updateChildren(userMap)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateUsername(newUsername: String, callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users").child(uid).child("username").setValue(newUsername)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateFirstName(firstName: String, callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users").child(uid).child("firstName").setValue(firstName)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updatePhone(phone: String, callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.getReference("users").child(uid).child("phone").setValue(phone)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun saveProfileImage(uri: Uri, callback: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_images/${uid}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    db.getReference("users").child(uid).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener {
                            GlobalScope.launch(Dispatchers.IO) {
                                val localUser = getLocalUser(uid)
                                val updatedUser = UserEntity(
                                    uid = uid,
                                    username = localUser?.username.orEmpty(),
                                    firstName = localUser?.firstName.orEmpty(),
                                    phone = localUser?.phone.orEmpty(),
                                    localProfileImagePath = imageUrl
                                )
                                insertUser(updatedUser)
                            }
                            callback(true, imageUrl)
                        }
                        .addOnFailureListener { callback(false, null) }
                }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to upload image", it)
                callback(false, null)
            }
    }

    fun saveProfileImage(bytes: ByteArray, callback: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_images/${uid}.jpg")

        storageRef.putBytes(bytes)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    db.getReference("users").child(uid).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener {
                            GlobalScope.launch(Dispatchers.IO) {
                                val localUser = getLocalUser(uid)
                                val updatedUser = UserEntity(
                                    uid = uid,
                                    username = localUser?.username.orEmpty(),
                                    firstName = localUser?.firstName.orEmpty(),
                                    phone = localUser?.phone.orEmpty(),
                                    localProfileImagePath = imageUrl
                                )
                                insertUser(updatedUser)
                            }
                            callback(true, imageUrl)
                        }
                        .addOnFailureListener { callback(false, null) }
                }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to upload image from bytes", it)
                callback(false, null)
            }
    }

    fun deleteUserAccount(callback: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        db.getReference("users").child(uid).removeValue()
        user.delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    suspend fun saveUserToRoom(uid: String, username: String, firstName: String, phone: String, localImagePath: String?) {
        val user = UserEntity(uid, username, firstName, phone, localImagePath)
        roomDb.userDao().insertUser(user)
    }
}
