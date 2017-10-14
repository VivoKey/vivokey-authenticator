package com.dangerousthings.vivoauth.ui.settings

import android.os.Bundle
import com.dangerousthings.vivoauth.R
import com.dangerousthings.vivoauth.ui.BaseActivity

class SettingsActivity : BaseActivity<SettingsViewModel>(SettingsViewModel::class.java) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}