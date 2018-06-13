package com.vivokey.vivoauth.ui.password

import com.vivokey.vivoauth.ui.BaseViewModel
import kotlinx.coroutines.experimental.Deferred

class PasswordViewModel : BaseViewModel() {
    fun setPassword(oldPassword: String, newPassword: String, remember: Boolean): Deferred<Boolean> = requestClient(lastDeviceInfo.id) {
        it.setPassword(oldPassword, newPassword, remember)
    }
}