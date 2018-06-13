package com.vivokey.vivoauth.protocol

interface ChallengeSigner {
    fun sign(input: ByteArray): ByteArray
}