/*
 * Copyright (c) 2013, Yubico AB.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package com.dangerousthings.vivoauth.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.dangerousthings.vivoauth.R
import com.dangerousthings.vivoauth.model.KeyManager
import kotlinx.android.synthetic.main.require_password_dialog.view.*

/**
 * Created with IntelliJ IDEA.
 * User: dain
 * Date: 8/26/13
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
class RequirePasswordDialog : DialogFragment() {
    companion object {
        const private val DEVICE_ID = "deviceId"
        const private val MISSING = "missing"

        internal fun newInstance(keyManager: KeyManager, id: ByteArray, missing: Boolean): RequirePasswordDialog {
            return RequirePasswordDialog().apply {
                arguments = Bundle().apply {
                    putByteArray(DEVICE_ID, id)
                    putBoolean(MISSING, missing)
                }
                setKeyManager(keyManager)
            }
        }
    }

    private lateinit var keyManager: KeyManager

    private fun setKeyManager(manager: KeyManager) {
        keyManager = manager
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val id = arguments.getByteArray(DEVICE_ID)
        val missing = arguments.getBoolean(MISSING)

        activity.layoutInflater.inflate(R.layout.require_password_dialog, null).let {
            return AlertDialog.Builder(activity).apply {
                setTitle(if (missing) R.string.password_required else R.string.wrong_password)
                setView(it).setPositiveButton(R.string.ok) { dialog, which ->
                    val password = it.editPassword.text.toString().trim()
                    val remember = it.rememberPassword.isChecked
                    keyManager.storeSecret(id, KeyManager.calculateSecret(password, id, false), remember)
                    keyManager.storeSecret(id, KeyManager.calculateSecret(password, id, true), remember)
                }
                setNegativeButton(R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
            }.create()
        }
    }
}
