package com.dangerousthings.vivoauth.ui.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.Base64
import com.dangerousthings.vivoauth.R
import com.dangerousthings.vivoauth.client.Credential
import java.io.ByteArrayOutputStream

class IconManager(context:Context) {
    companion object {
        private const val ICON_STORAGE = "ICON_STORAGE"
        private const val SIZE = 128
        private const val RADIUS = (SIZE / 2).toFloat()
    }

    private val iconStorage = context.getSharedPreferences(ICON_STORAGE, Context.MODE_PRIVATE)

    private val colors = context.resources.obtainTypedArray(R.array.icon_colors).let {
        (0 until it.length()).map { i -> it.getColor(i, 0) }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = RADIUS
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, android.R.color.primary_text_dark)
    }

    fun setIcon(credential: Credential, icon: Bitmap) {
        val baos = ByteArrayOutputStream()
        Bitmap.createScaledBitmap(icon, SIZE, SIZE, true).compress(Bitmap.CompressFormat.PNG, 100, baos)
        val bytes = baos.toByteArray()
        iconStorage.edit().putString(credential.key, Base64.encodeToString(bytes, Base64.DEFAULT)).apply()
    }

    fun removeIcon(credential: Credential) {
        iconStorage.edit().remove(credential.key).apply()
    }

    fun clearIcons() {
        iconStorage.edit().clear().apply()
    }

    fun hasIcon(credential: Credential) = iconStorage.contains(credential.key)

    fun getIcon(credential: Credential): Bitmap {
        return iconStorage.getString(credential.key, null)?.let {
            getSavedIcon(it)
        } ?: getLetterIcon(credential)
    }

    private fun getLetterIcon(credential: Credential): Bitmap {
        return Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).apply {
                drawCircle(RADIUS, RADIUS, RADIUS, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colors[Math.abs(credential.key.hashCode()) % colors.size]
                })
                val letter = (if (credential.issuer.isNullOrEmpty()) credential.name else credential.issuer!!).substring(0, 1).toUpperCase()
                drawText(letter, RADIUS, RADIUS - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
            }
        }
    }

    private fun getSavedIcon(data: String): Bitmap {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return if(bitmap.width == SIZE) bitmap else Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
    }
}