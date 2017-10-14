package com.dangerousthings.vivoauth.ui.add

import android.net.Uri
import com.dangerousthings.vivoauth.client.Code
import com.dangerousthings.vivoauth.client.Credential
import com.dangerousthings.vivoauth.protocol.CredentialData
import com.dangerousthings.vivoauth.protocol.OathType
import com.dangerousthings.vivoauth.ui.BaseViewModel
import kotlinx.coroutines.experimental.Deferred

class AddCredentialViewModel : BaseViewModel() {
    private var handledUri: Uri? = null
    var data: CredentialData? = null

    fun handleScanResults(qrUri: Uri) {
        if (qrUri != handledUri) {
            handledUri = qrUri
            data = CredentialData.from_uri(qrUri)
        }
    }

    fun addCredential(credentialData: CredentialData): Deferred<Pair<Credential, Code?>> = requestClient { client ->
        client.addCredential(credentialData).let {
            Pair(it, if (!(it.touch || it.type == OathType.HOTP)) client.calculate(it, System.currentTimeMillis()) else null)
        }
    }
}