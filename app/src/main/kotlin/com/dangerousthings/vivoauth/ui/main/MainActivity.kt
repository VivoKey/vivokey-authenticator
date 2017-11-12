package com.dangerousthings.vivoauth.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.ProgressBar
import com.dangerousthings.vivoauth.R
import com.dangerousthings.vivoauth.R.id.progressBar
import com.dangerousthings.vivoauth.ui.BaseActivity
import com.dangerousthings.vivoauth.ui.password.PasswordActivity
import com.dangerousthings.vivoauth.ui.settings.SettingsActivity
import org.jetbrains.anko.toast

class MainActivity : BaseActivity<OathViewModel>(OathViewModel::class.java) {
    companion object {
        const private val REQUEST_PASSWORD = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Clear storage from older version of app
        getSharedPreferences("NEO_STORE", Context.MODE_PRIVATE).edit().clear().apply()

        // Custom colours for progress bar
        var green = ContextCompat.getColor(this, R.color.luridGreen)
        var progressBar = findViewById<ProgressBar>(R.id.progressBar)

        progressBar.progressDrawable.setColorFilter(green, PorterDuff.Mode.SRC_IN)
        progressBar.indeterminateDrawable.setColorFilter(green, PorterDuff.Mode.SRC_IN)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchView = menu.findItem(R.id.menu_main_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean = false

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.searchFilter = newText
                return true
            }
        })

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_main_password).isEnabled = viewModel.lastDeviceInfo.version.compare(0, 0, 0) > 0

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_password -> startActivityForResult(Intent(this, PasswordActivity::class.java), REQUEST_PASSWORD)
            R.id.menu_main_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PASSWORD -> if (resultCode == Activity.RESULT_OK) {
                toast(R.string.password_updated)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
