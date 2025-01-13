package com.example.shoppinglist

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Intent;


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val idNumber = findViewById<EditText>(R.id.idNumber)
        val password = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val id = idNumber.text.toString().trim()
            val pass = password.text.toString().trim()

            if (id.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show()
            } else {
                // כאן אפשר להוסיף לוגיקה לבדוק את פרטי המשתמש
                Toast.makeText(this, "נכנסת בהצלחה!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PartnerSelectionActivity::class.java)
                startActivity(intent);
            }

            }
        }
    }

