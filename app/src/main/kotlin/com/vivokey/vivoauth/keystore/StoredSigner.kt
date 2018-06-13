package com.vivokey.vivoauth.keystore

import com.vivokey.vivoauth.protocol.ChallengeSigner

interface StoredSigner : ChallengeSigner {
    fun promote()
}