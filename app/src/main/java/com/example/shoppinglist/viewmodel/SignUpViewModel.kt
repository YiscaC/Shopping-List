package com.example.shoppinglist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shoppinglist.data.repository.SignUpRepository

class SignUpViewModel : ViewModel() {

    private val repository = SignUpRepository()

    private val _signUpSuccess = MutableLiveData<Boolean>()
    val signUpSuccess: LiveData<Boolean> get() = _signUpSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun register(username: String, email: String, password: String) {
        repository.register(
            username,
            email,
            password,
            onSuccess = { _signUpSuccess.value = true },
            onError = { errorMessage -> _errorMessage.value = errorMessage }
        )
    }
}
