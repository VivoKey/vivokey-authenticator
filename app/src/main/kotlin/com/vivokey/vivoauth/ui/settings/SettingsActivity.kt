package com.vivokey.vivoauth.ui.settings

import android.os.Bundle
import com.vivokey.vivoauth.R
import com.vivokey.vivoauth.ui.BaseActivity

class SettingsActivity : BaseActivity<SettingsViewModel>(SettingsViewModel::class.java) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}