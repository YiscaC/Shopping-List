package com.example.shoppinglist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.shoppinglist.ui.shoppinglist.fragment.ShoppingListFragmentDirections
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    var activeListId: String? = null
    var activeListName: String? = null

    private var navigatedOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment  -> {
                    bottomNavView.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                R.id.participantsFragment -> {
                    bottomNavView.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true) // ✅ חץ חזור
                }
                R.id.shoppingItemsFragment -> {
                    bottomNavView.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true) // ✅ חץ חזור
                }
                R.id.profileFragment, R.id.editProfileFragment , R.id.shoppingListFragment-> {
                    bottomNavView.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) // ❌ אין חץ חזור
                }
                else -> {
                    bottomNavView.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
            }

            markMenuItemAsSelected(bottomNavView, destination.id)
        }

        bottomNavView.setOnItemSelectedListener { item ->
            val navController = findNavController(R.id.nav_host_fragment)

            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.shoppingItemsFragment -> {
                    val listId = activeListId
                    val listName = activeListName
                    if (listId != null && listName != null) {
                        val action = ShoppingListFragmentDirections
                            .actionShoppingListFragmentToShoppingItemsFragment(listId, listName)
                        navController.navigate(action)
                        bottomNavView.selectedItemId = item.itemId
                    } else {
                        Toast.makeText(this, "⚠️ אין רשימה פעילה", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                R.id.participantsFragment -> {
                    val listId = activeListId
                    if (listId != null) {
                        val action = ShoppingListFragmentDirections
                            .actionShoppingListFragmentToParticipantsFragment(listId)
                        navController.navigate(action)
                        bottomNavView.selectedItemId = item.itemId
                    } else {
                        Toast.makeText(this, "⚠️ אין רשימה פעילה", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) {
                        bottomNavView.selectedItemId = item.itemId
                    }
                    handled
                }
            }
        }

        setupActionBarWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()
        val user = auth.currentUser
        if (user != null && !navigatedOnStart) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.shoppingListFragment)
            navigatedOnStart = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun markMenuItemAsSelected(bottomNavView: BottomNavigationView, destinationId: Int) {
        bottomNavView.post {
            val item = bottomNavView.menu.findItem(destinationId)
            item?.isChecked = true
        }
    }

}
