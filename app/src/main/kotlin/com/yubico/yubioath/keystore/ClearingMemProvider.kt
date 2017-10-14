package com.yubico.yubioath.keystore

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ClearingMemProvider(private val exec: CoroutineDispatcher) : KeyProvider {
    private val map = mutableMapOf<String, MutableSet<Mac>>()
    private var clearJob: Job? = null

    override fun hasKeys(deviceId: String): Boolean = map.contains(deviceId)

    override fun getKeys(deviceId: String): Sequence<StoredSigner> {
        clearJob?.cancel()
        clearJob = launch(exec) {
            delay(5, TimeUnit.MINUTES)  //Clear stored passwords after inactivity.
            clearAll()
        }

        return map[deviceId].orEmpty().asSequence().map { MemStoredSigner(deviceId, it) }
    }

    override fun addKey(deviceId: String, secret: ByteArray) {
        map.getOrPut(deviceId, { mutableSetOf() }).add(Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(secret, algorithm))
        })
    }

    override fun clearKeys(deviceId: String) {
        map[deviceId]?.clear()
    }

    override fun clearAll() = map.clear()

    private inner class MemStoredSigner(val deviceId: String, val mac: Mac) : StoredSigner {
        override fun sign(input: ByteArray): ByteArray = mac.doFinal(input)

        override fun promote() {
            map[deviceId] = mutableSetOf(mac)
        }
    }
}