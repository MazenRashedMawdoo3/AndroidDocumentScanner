package com.labters.documentscanner

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException

object AppUtils {
    fun uriToBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            val parcelFileDescriptor: ParcelFileDescriptor? =
                contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor: FileDescriptor? = parcelFileDescriptor?.fileDescriptor
            BitmapFactory.decodeFileDescriptor(fileDescriptor)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun getMediaPath(bmp: Bitmap, activity: Activity): String? {
        val bmpUri: Uri
        try {
            val file = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "profile_image" + System.currentTimeMillis() + ".png"
            )
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.close()
            bmpUri = Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return bmpUri?.path ?: "no path"
    }
}