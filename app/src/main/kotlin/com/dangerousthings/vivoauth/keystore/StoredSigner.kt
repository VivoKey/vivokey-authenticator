package com.dangerousthings.vivoauth.keystore

import com.dangerousthings.vivoauth.protocol.ChallengeSigner

interface StoredSigner : ChallengeSigner {
    fun promote()
}