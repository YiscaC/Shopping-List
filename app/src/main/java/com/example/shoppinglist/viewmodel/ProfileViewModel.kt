package com.example.shoppinglist.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.UserEntity
import com.example.shoppinglist.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application.applicationContext)

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _firstName = MutableLiveData<String>()
    val firstName: LiveData<String> get() = _firstName

    private val _phone = MutableLiveData<String>()
    val phone: LiveData<String> get() = _phone

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> get() = _profileImageUrl

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> get() = _updateSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> get() = _deleteSuccess

    fun loadUserData() {
        val uid = repository.getCurrentUser()?.uid ?: return
        _email.postValue(repository.getCurrentUser()?.email)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // טוען קודם נתונים מה-ROOM
                val localUser = repository.getLocalUser(uid)
                localUser?.let {
                    Log.d("ProfileViewModel", "Loaded from Room: $it")
                    _username.postValue(it.username)
                    _firstName.postValue(it.firstName)
                    _phone.postValue(it.phone)
                    _profileImageUrl.postValue(it.localProfileImagePath)
                }

                // רק אם יש אינטרנט – נביא גם מהשרת
                if (isInternetAvailable(getApplication())) {
                    repository.getUserData { username, firstName, phone, imageUrl ->
                        if (!username.isNullOrBlank() && username != _username.value) _username.postValue(username)
                        if (!firstName.isNullOrBlank() && firstName != _firstName.value) _firstName.postValue(firstName)
                        if (!phone.isNullOrBlank() && phone != _phone.value) _phone.postValue(phone)

                        val currentPath = _profileImageUrl.value
                        val isRoomImage = currentPath?.startsWith("/data") == true
                        if (!isRoomImage && !imageUrl.isNullOrBlank()) {
                            _profileImageUrl.postValue(imageUrl)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "שגיאה ב-loadUserData", e)
            }
        }
    }

    fun updateUsername(username: String) {
        repository.updateUsername(username) { success ->
            _updateSuccess.postValue(success)
            if (success) _username.postValue(username)
        }
    }

    fun updateFirstName(firstName: String) {
        repository.updateFirstName(firstName) { success ->
            if (success) _firstName.postValue(firstName)
        }
    }

    fun updatePhone(phone: String) {
        repository.updatePhone(phone) { success ->
            if (success) _phone.postValue(phone)
        }
    }

    fun updateProfile(username: String, firstName: String, phone: String, imagePath: String? = profileImageUrl.value) {
        val uid = repository.getCurrentUser()?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveUserToRoom(uid, username, firstName, phone, imagePath)
                Log.d("ProfileViewModel", "שמירה ל-Room הצליחה")
                _username.postValue(username)
                _firstName.postValue(firstName)
                _phone.postValue(phone)
                _profileImageUrl.postValue(imagePath)

                repository.updateProfile(username, firstName, phone) { success ->
                    _updateSuccess.postValue(success)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "שגיאה בשמירה ל-Room", e)
                _updateSuccess.postValue(false)
            }
        }
    }

    fun saveProfileImage(uri: Uri) {
        val uid = repository.getCurrentUser()?.uid ?: return
        val file = File(getApplication<Application>().applicationContext.filesDir, "profile_$uid.jpg")
        val localPath = file.absolutePath

        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            FileOutputStream(file).use { output ->
                inputStream?.copyTo(output)
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "❌ שגיאה בהעתקת קובץ התמונה מהגלריה", e)
            _updateSuccess.postValue(false)
            return
        }

        _profileImageUrl.postValue(localPath)
        updateProfile(username.value ?: "", firstName.value ?: "", phone.value ?: "", localPath)

        repository.saveProfileImage(uri) { success, imagePath ->
            _updateSuccess.postValue(success)
        }
    }

    fun saveProfileImage(bytes: ByteArray, callback: (Boolean) -> Unit) {
        val uid = repository.getCurrentUser()?.uid ?: return

        val file = File(getApplication<Application>().applicationContext.filesDir, "profile_$uid.jpg")
        val localPath = file.absolutePath

        try {
            FileOutputStream(file).use { it.write(bytes) }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "❌ שגיאה בכתיבת קובץ התמונה", e)
            _updateSuccess.postValue(false)
            callback(false)
            return
        }

        _profileImageUrl.postValue(localPath)
        updateProfile(username.value ?: "", firstName.value ?: "", phone.value ?: "", localPath)

        repository.saveProfileImage(bytes) { success, imageUrl ->
            _updateSuccess.postValue(success)
            callback(success)
        }
    }

    fun deleteUserAccount() {
        val uid = repository.getCurrentUser()?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteUserDataFromLists(uid) { success ->
                    if (success) {
                        repository.deleteUserAccount { deleteSuccess ->
                            _deleteSuccess.postValue(deleteSuccess)
                        }
                    } else {
                        _deleteSuccess.postValue(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ שגיאה במחיקת משתמש ורשימות", e)
                _deleteSuccess.postValue(false)
            }
        }
    }
    fun deleteUserAccountWithReAuth(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteUserAccountWithReAuth(email, password) { success ->
                    _deleteSuccess.postValue(success)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ שגיאה במחיקה עם רה-אימות", e)
                _deleteSuccess.postValue(false)
            }
        }
    }


    fun getCurrentUser() = repository.getCurrentUser()

    // פונקציה לבדוק אינטרנט
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
