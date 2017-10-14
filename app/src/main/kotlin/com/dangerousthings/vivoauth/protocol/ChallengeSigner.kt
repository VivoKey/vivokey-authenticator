package com.dangerousthings.vivoauth.protocol

interface ChallengeSigner {
    fun sign(input: ByteArray): ByteArray
}