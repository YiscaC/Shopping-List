package com.example.shoppinglist
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShoppingListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView // משתמשים ב-lateinit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        // אתחול RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val shoppingList = listOf(
            ShoppingItem("Milk", 2, false),
            ShoppingItem("Bread", 1, true),
            ShoppingItem("Eggs", 12, false)
        )

        val adapter = ShoppingListAdapter(shoppingList)
        recyclerView.adapter = adapter
    }
}
