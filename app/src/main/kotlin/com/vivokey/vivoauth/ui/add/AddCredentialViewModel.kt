package com.vivokey.vivoauth.ui.add

import android.net.Uri
import com.vivokey.vivoauth.client.Code
import com.vivokey.vivoauth.client.Credential
import com.vivokey.vivoauth.protocol.CredentialData
import com.vivokey.vivoauth.protocol.OathType
import com.vivokey.vivoauth.ui.BaseViewModel
import kotlinx.coroutines.experimental.Deferred

class AddCredentialViewModel : BaseViewModel() {
    private var handledUri: Uri? = null
    var data: CredentialData? = null

    fun handleScanResults(qrUri: Uri) {
        if (qrUri != handledUri) {
            handledUri = qrUri
            data = CredentialData.fromUri(qrUri)
        }
    }

    fun addCredential(credentialData: CredentialData): Deferred<Pair<Credential, Code?>> = requestClient { client ->
        client.addCredential(credentialData).let {
            Pair(it, if (!(it.touch || it.type == OathType.HOTP)) client.calculate(it, System.currentTimeMillis()) else null)
        }
    }
}