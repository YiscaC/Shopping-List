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
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var currentMenu: Int = R.menu.bottom_nav_main

    // ✅ משתנים לזיכרון של הרשימה הפעילה
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
        bottomNavView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment -> {
                    bottomNavView.visibility = View.GONE
                }

                R.id.shoppingItemsFragment, R.id.participantsFragment -> {
                    bottomNavView.visibility = View.VISIBLE
                    bottomNavView.menu.clear()
                    bottomNavView.inflateMenu(R.menu.bottom_nav_list)
                    currentMenu = R.menu.bottom_nav_list

                    // ✅ סימון ידני במקום selectedItemId
                    markMenuItemAsSelected(bottomNavView, destination.id)

                    bottomNavView.setOnItemSelectedListener { item ->
                        if (item.itemId == navController.currentDestination?.id) {
                            return@setOnItemSelectedListener true
                        }

                        when (item.itemId) {
                            R.id.shoppingItemsFragment -> {
                                val listId = activeListId
                                val listName = activeListName

                                if (listId != null && listName != null) {
                                    val bundle = Bundle().apply {
                                        putString("listId", listId)
                                        putString("listName", listName)
                                    }
                                    navController.navigate(R.id.shoppingItemsFragment, bundle)
                                } else {
                                    Toast.makeText(this, "⚠️ אין רשימה פעילה", Toast.LENGTH_SHORT).show()
                                }
                                true
                            }

                            R.id.participantsFragment,
                            R.id.profileFragment -> {
                                NavigationUI.onNavDestinationSelected(item, navController)
                                true
                            }

                            else -> false
                        }
                    }
                }

                R.id.shoppingListFragment, R.id.partnerFragment -> {
                    bottomNavView.visibility = View.VISIBLE
                    bottomNavView.menu.clear()
                    bottomNavView.inflateMenu(R.menu.bottom_nav_main)
                    currentMenu = R.menu.bottom_nav_main

                    // ✅ סימון ידני במקום selectedItemId
                    markMenuItemAsSelected(bottomNavView, destination.id)

                    bottomNavView.setOnItemSelectedListener { item ->
                        if (item.itemId == navController.currentDestination?.id) {
                            return@setOnItemSelectedListener true
                        }

                        when (item.itemId) {
                            R.id.shoppingListFragment,
                            R.id.profileFragment -> {
                                NavigationUI.onNavDestinationSelected(item, navController)
                                true
                            }

                            else -> false
                        }
                    }
                }

                R.id.profileFragment -> {
                    bottomNavView.visibility = View.VISIBLE
                    bottomNavView.menu.clear()
                    bottomNavView.inflateMenu(currentMenu)

                    // ✅ סימון ידני גם כאן
                    markMenuItemAsSelected(bottomNavView, destination.id)
                }

                else -> {
                    bottomNavView.visibility = View.VISIBLE
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

    // ✅ פונקציה שמסמנת ידנית את הפריט הפעיל בתפריט
    private fun markMenuItemAsSelected(bottomNavView: BottomNavigationView, destinationId: Int) {
        val menu = bottomNavView.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            item.isChecked = item.itemId == destinationId
        }
    }
}
