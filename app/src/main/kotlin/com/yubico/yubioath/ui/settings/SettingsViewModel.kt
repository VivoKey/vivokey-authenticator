package com.yubico.yubioath.ui.settings

import com.yubico.yubioath.R
import com.yubico.yubioath.ui.BaseViewModel
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.toast

/**
 * Created by Dain on 2017-08-15.
 */
class SettingsViewModel : BaseViewModel() {
    fun clearStoredPasswords() {
        services!!.let {
            it.keyManager.clearAll()
            launch(UI) {
                it.context.toast(R.string.data_cleared)
            }
        }
    }
}