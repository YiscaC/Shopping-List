package com.example.shoppinglist.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.shoppinglist.data.repository.ProfileRepository

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application.applicationContext)

    private val _username = MutableLiveData<String>()
    val username: LiveData<String> get() = _username

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> get() = _profileImageUrl

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> get() = _updateSuccess

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> get() = _deleteSuccess

    fun loadUserData() {
        repository.getUserData { username, imageUrl ->
            _username.postValue(username)
            _profileImageUrl.postValue(imageUrl)
        }
    }

    fun updateUsername(newUsername: String) {
        repository.updateUsername(newUsername) { success ->
            _updateSuccess.postValue(success)
            if (success) _username.postValue(newUsername)
        }
    }

    fun saveProfileImage(uri: Uri) {
        repository.saveProfileImage(uri) { success, imagePath ->
            _updateSuccess.postValue(success)
            if (success) _profileImageUrl.postValue(imagePath)
        }
    }

    fun deleteUserAccount() {
        repository.deleteUserAccount { success ->
            _deleteSuccess.postValue(success)
        }
    }
}
