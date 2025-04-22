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

    private fun getEmailKey(): String? {
        val email = auth.currentUser?.email ?: return null
        return email.replace(".", ",")
    }

    suspend fun getLocalUser(uid: String): UserEntity? {
        return withContext(Dispatchers.IO) {
            roomDb.userDao().getUserById(uid)
        }
    }

    fun getUserData(callback: (String?, String?, String?, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val emailKey = user.email?.replace(".", ",") ?: return
        val uid = user.uid

        db.getReference("users").child(emailKey).get()
            .addOnSuccessListener { emailSnapshot ->
                val username = emailSnapshot.child("username").value as? String
                val firstName = emailSnapshot.child("firstName").value as? String
                val phone = emailSnapshot.child("phone").value as? String

                db.getReference("users").child(uid).get()
                    .addOnSuccessListener { uidSnapshot ->
                        val profileImageUrl = uidSnapshot.child("profileImageUrl").value as? String
                        callback(username, firstName, phone, profileImageUrl)
                    }
                    .addOnFailureListener {
                        Log.e("ProfileRepository", "Failed to load image", it)
                        callback(username, firstName, phone, null)
                    }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to load user info", it)
                callback(null, null, null, null)
            }
    }

    fun updateProfile(username: String, firstName: String, phone: String, callback: (Boolean) -> Unit) {
        val emailKey = getEmailKey() ?: return

        val userMap = mapOf(
            "username" to username,
            "firstName" to firstName,
            "phone" to phone
        )

        db.getReference("users").child(emailKey).updateChildren(userMap)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateUsername(newUsername: String, callback: (Boolean) -> Unit) {
        val emailKey = getEmailKey() ?: return

        db.getReference("users").child(emailKey).child("username").setValue(newUsername)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateFirstName(firstName: String, callback: (Boolean) -> Unit) {
        val emailKey = getEmailKey() ?: return

        db.getReference("users").child(emailKey).child("firstName").setValue(firstName)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updatePhone(phone: String, callback: (Boolean) -> Unit) {
        val emailKey = getEmailKey() ?: return

        db.getReference("users").child(emailKey).child("phone").setValue(phone)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun saveProfileImage(uri: Uri, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_images/${uid}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    db.getReference("users").child(uid).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener { callback(true, imageUrl) }
                        .addOnFailureListener { callback(false, null) }
                }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to upload image", it)
                callback(false, null)
            }
    }

    fun saveProfileImage(bytes: ByteArray, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid

        val storageRef = FirebaseStorage.getInstance().reference
            .child("profile_images/${uid}.jpg")

        storageRef.putBytes(bytes)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    db.getReference("users").child(uid).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener {
                            db.getReference("users").child(uid).get().addOnSuccessListener { snapshot ->
                                val username = snapshot.child("username").value as? String ?: ""
                                val firstName = snapshot.child("firstName").value as? String ?: ""
                                val phone = snapshot.child("phone").value as? String ?: ""

                                val localPath = saveImageLocally(bytes, uid)
                                val userEntity = UserEntity(uid, username, firstName, phone, localPath)
                                GlobalScope.launch(Dispatchers.IO) {
                                    roomDb.userDao().insertUser(userEntity)
                                }

                                callback(true, imageUrl)
                            }.addOnFailureListener {
                                callback(true, imageUrl)
                            }
                        }
                        .addOnFailureListener { callback(false, null) }
                }
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to upload image from bytes", it)
                callback(false, null)
            }
    }

    private fun saveImageLocally(bytes: ByteArray, uid: String): String {
        val file = File(context.filesDir, "profile_$uid.jpg")
        val outputStream = FileOutputStream(file)
        outputStream.write(bytes)
        outputStream.flush()
        outputStream.close()
        return file.absolutePath
    }

    fun deleteUserAccount(callback: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val emailKey = getEmailKey() ?: return

        db.getReference("users").child(emailKey).removeValue()
        db.getReference("users").child(user.uid).removeValue()
        user.delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    suspend fun saveUserToRoom(uid: String, username: String, firstName: String, phone: String, localImagePath: String?) {
        val user = UserEntity(uid, username, firstName, phone, localImagePath)
        roomDb.userDao().insertUser(user)
    }
}
