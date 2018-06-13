package com.vivokey.vivoauth.ui.settings

import com.vivokey.vivoauth.R
import com.vivokey.vivoauth.ui.BaseViewModel
import com.vivokey.vivoauth.ui.main.IconManager
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.toast

class SettingsViewModel : BaseViewModel() {
    fun clearStoredPasswords() {
        services?.let {
            it.keyManager.clearAll()
            launch(UI) {
                it.context.toast(R.string.passwords_cleared)
            }
        }
    }

    fun clearIcons() {
        services?.let {
            IconManager(it.context).clearIcons()
            launch(UI) {
                it.context.toast(R.string.icons_cleared)
            }
        }
    }
}
