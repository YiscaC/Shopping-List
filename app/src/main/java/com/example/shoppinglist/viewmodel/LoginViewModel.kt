package com.example.shoppinglist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shoppinglist.data.repository.LoginRepository

class LoginViewModel : ViewModel() {

    private val repository = LoginRepository()

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun login(email: String, password: String) {
        repository.login(
            email,
            password,
            onSuccess = { _loginResult.value = true },
            onError = { errorMessage -> _errorMessage.value = errorMessage }
        )
    }
}
