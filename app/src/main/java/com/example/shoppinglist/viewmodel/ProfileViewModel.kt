package com.example.shoppinglist.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

    private fun hasInternetConnection(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    fun loadUserData() {
        val uid = repository.getCurrentUser()?.uid ?: return
        _email.postValue(repository.getCurrentUser()?.email)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localUser = repository.getLocalUser(uid)
                localUser?.let {
                    Log.d("ProfileViewModel", "Loaded from Room: $it")
                    _username.postValue(it.username)
                    _firstName.postValue(it.firstName)
                    _phone.postValue(it.phone)
                    _profileImageUrl.postValue(it.localProfileImagePath)
                }

                repository.getUserData { username, firstName, phone, imageUrl ->
                    if (!username.isNullOrBlank()) _username.postValue(username)
                    if (!firstName.isNullOrBlank()) _firstName.postValue(firstName)
                    if (!phone.isNullOrBlank()) _phone.postValue(phone)
                    if (!imageUrl.isNullOrBlank()) _profileImageUrl.postValue(imageUrl)
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

    fun updateProfile(username: String, firstName: String, phone: String) {
        val uid = repository.getCurrentUser()?.uid ?: return
        val localImagePath = profileImageUrl.value?.takeIf { it.startsWith("/data") }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveUserToRoom(uid, username, firstName, phone, localImagePath)
                Log.d("ProfileViewModel", "שמירה ל-Room הצליחה")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "שגיאה בשמירה ל-Room", e)
            }
        }

        repository.updateProfile(username, firstName, phone) { success ->
            _updateSuccess.postValue(success)
            if (success) {
                _username.postValue(username)
                _firstName.postValue(firstName)
                _phone.postValue(phone)
            }
        }
    }

    fun saveProfileImage(uri: Uri) {
        repository.saveProfileImage(uri) { success, imagePath ->
            _updateSuccess.postValue(success)
            if (success) _profileImageUrl.postValue(imagePath)
        }
    }

    fun saveProfileImage(bytes: ByteArray, callback: (Boolean) -> Unit) {
        val uid = repository.getCurrentUser()?.uid ?: return

        val file = File(getApplication<Application>().applicationContext.filesDir, "profile_$uid.jpg")
        val outputStream = FileOutputStream(file)
        outputStream.write(bytes)
        outputStream.flush()
        outputStream.close()
        val localPath = file.absolutePath

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveUserToRoom(
                    uid,
                    username.value.orEmpty(),
                    firstName.value.orEmpty(),
                    phone.value.orEmpty(),
                    localPath
                )
                Log.d("ProfileViewModel", "שמירת תמונה ל-Room הצליחה")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "שגיאה בשמירת תמונה ל-Room", e)
            }
        }

        _profileImageUrl.postValue(localPath)

        repository.saveProfileImage(bytes) { success, imagePath ->
            _updateSuccess.postValue(success)
            if (success && imagePath != null) {
                _profileImageUrl.postValue(imagePath)
            }
            callback(success)
        }
    }

    fun deleteUserAccount() {
        repository.deleteUserAccount { success ->
            _deleteSuccess.postValue(success)
        }
    }

    fun getCurrentUser() = repository.getCurrentUser()
}
