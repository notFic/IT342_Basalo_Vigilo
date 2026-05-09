package com.it342.basalo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.it342.basalo.core.data.RoleUtils
import com.it342.basalo.core.data.SessionManager
import com.it342.basalo.features.admin.AuditLogFragment
import com.it342.basalo.features.admin.LocationManagementFragment
import com.it342.basalo.features.admin.StaffManagementFragment
import com.it342.basalo.features.visitor.VisitorCheckInActivity
import com.it342.basalo.features.visitor.HistoryFragment
import com.it342.basalo.features.visitor.ActiveLogsFragment
import com.it342.basalo.features.auth.ProfileFragment
import com.it342.basalo.features.auth.LoginActivity
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.topToolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fab = findViewById(R.id.fabNewVisitor)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_navigation, R.string.close_navigation)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        configureDrawer()

        fab.setOnClickListener {
            startActivity(Intent(this, VisitorCheckInActivity::class.java))
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_active -> navigateTo(ActiveLogsFragment(), getString(R.string.active_logs_title))
                R.id.menu_history -> navigateTo(HistoryFragment(), getString(R.string.history_title))
                R.id.menu_profile -> navigateTo(ProfileFragment(), getString(R.string.profile_title))
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.menu_active
        }
    }

    private fun configureDrawer() {
        val profile = sessionManager.getProfile()
        val header = navigationView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tvDrawerName).text = profile?.fullName ?: getString(R.string.default_guard_name)
        header.findViewById<TextView>(R.id.tvDrawerEmail).text = profile?.email ?: getString(R.string.default_email)
        header.findViewById<TextView>(R.id.tvDrawerRole).text =
            if (RoleUtils.isAdmin(profile?.role)) getString(R.string.admin_badge) else getString(R.string.staff_badge)

        if (!RoleUtils.isAdmin(profile?.role)) {
            navigationView.menu.findItem(R.id.drawer_staff)?.isVisible = false
            navigationView.menu.findItem(R.id.drawer_locations)?.isVisible = false
            navigationView.menu.findItem(R.id.drawer_audit)?.isVisible = false
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.drawer_active -> bottomNavigation.selectedItemId = R.id.menu_active
                R.id.drawer_history -> bottomNavigation.selectedItemId = R.id.menu_history
                R.id.drawer_profile -> bottomNavigation.selectedItemId = R.id.menu_profile
                R.id.drawer_staff -> navigateTo(StaffManagementFragment(), getString(R.string.staff_management_title))
                R.id.drawer_locations -> navigateTo(LocationManagementFragment(), getString(R.string.location_management_title))
                R.id.drawer_audit -> navigateTo(AuditLogFragment(), getString(R.string.audit_log_title))
                R.id.drawer_logout -> {
                    sessionManager.clearSession()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun navigateTo(fragment: Fragment, title: String) {
        supportActionBar?.title = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
