package com.example.shoppinglist.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun getCurrentUser() = auth.currentUser

    fun getUserData(callback: (String?, String?) -> Unit) {
        val user = auth.currentUser ?: return
        db.getReference("users").child(user.uid).get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").value as? String
                val profileImageUrl = snapshot.child("profileImageUrl").value as? String
                callback(username, profileImageUrl)
            }
            .addOnFailureListener {
                Log.e("ProfileRepository", "Failed to load user data", it)
                callback(null, null)
            }
    }

    fun updateUsername(newUsername: String, callback: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        db.getReference("users").child(user.uid).child("username").setValue(newUsername)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun saveProfileImage(uri: Uri, callback: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "profile_image_${user.uid}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val savedPath = file.absolutePath
            db.getReference("users").child(user.uid).child("profileImageUrl").setValue(savedPath)
                .addOnSuccessListener { callback(true, savedPath) }
                .addOnFailureListener { callback(false, null) }

        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to save image locally", e)
            callback(false, null)
        }
    }

    fun deleteUserAccount(callback: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        db.getReference("users").child(user.uid).removeValue()
        user.delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}
