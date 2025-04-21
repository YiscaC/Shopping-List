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

        // ✅ תפריט משתנה לפי היעד הנוכחי
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNavView.menu.clear()

            when (destination.id) {
                R.id.shoppingItemsFragment, R.id.participantsFragment -> {
                    bottomNavView.inflateMenu(R.menu.bottom_nav)
                    bottomNavView.menu.findItem(R.id.shoppingItemsFragment).isVisible = true
                    bottomNavView.menu.findItem(R.id.participantsFragment).isVisible = true
                    bottomNavView.menu.findItem(R.id.shoppingListFragment).isVisible = false
                    bottomNavView.menu.findItem(R.id.profileFragment).isVisible = false
                }

                R.id.shoppingListFragment, R.id.profileFragment, R.id.partnerFragment -> {
                    bottomNavView.inflateMenu(R.menu.bottom_nav)
                    bottomNavView.menu.findItem(R.id.shoppingListFragment).isVisible = true
                    bottomNavView.menu.findItem(R.id.profileFragment).isVisible = true
                    bottomNavView.menu.findItem(R.id.shoppingItemsFragment).isVisible = false
                    bottomNavView.menu.findItem(R.id.participantsFragment).isVisible = false
                }

                R.id.loginFragment, R.id.signUpFragment -> {
                    bottomNavView.visibility = View.GONE
                    return@addOnDestinationChangedListener
                }

                else -> {
                    bottomNavView.inflateMenu(R.menu.bottom_nav)
                }
            }

            bottomNavView.visibility = View.VISIBLE

            // ✅ סימון ידני של הפריט הפעיל
            markMenuItemAsSelected(bottomNavView, destination.id)
        }

        // ✅ ניווט כולל סימון ידני לאחר SafeArgs
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
        if (user != null) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.partnerFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // ✅ סימון ידני של הפריט הפעיל בתפריט – עם תיקון post
    private fun markMenuItemAsSelected(bottomNavView: BottomNavigationView, destinationId: Int) {
        bottomNavView.post {
            val item = bottomNavView.menu.findItem(destinationId)
            item?.isChecked = true
        }
    }
}
