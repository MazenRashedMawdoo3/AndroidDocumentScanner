/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:17 PM
 *
 */

package com.labters.documentscannerandroid

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kotlinpermissions.KotlinPermissions
import com.labters.documentscanner.ImageCropActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var btnPick: Button
    lateinit var imgBitmap: ImageView
    lateinit var mCurrentPhotoPath: String

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            var selectedImage = data.data
            var btimap: Bitmap? = null
            try {
                val inputStream = selectedImage?.let { contentResolver.openInputStream(it) }
                btimap = BitmapFactory.decodeStream(inputStream)
//                ScannerConstants.selectedImageBitmap = btimap
//                startActivityForResult(
//                    Intent(MainActivity@ this, ImageCropActivity::class.java),
//                    1234
//                )
                ImageCropActivity.startCropping(
                    this,
                    AppUtils.getMediaPath(btimap!!, this)!!,
                    R.string.app_name,
                    R.string.app_name
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
//            ScannerConstants.selectedImageBitmap = MediaStore.Images.Media.getBitmap(
//                this.contentResolver,
//                Uri.parse(mCurrentPhotoPath)
//            )
//            startActivityForResult(Intent(MainActivity@ this, ImageCropActivity::class.java), 1234)
        } else if (requestCode == ImageCropActivity.CROP_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.getStringExtra(ImageCropActivity.RESULT_IMAGE_PATH)?.also {
                imgBitmap.setImageBitmap(BitmapFactory.decodeFile(it))
                imgBitmap.visibility = View.VISIBLE
                btnPick.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnPick = findViewById(R.id.btnPick)
        imgBitmap = findViewById(R.id.imgBitmap)
        askPermission()
    }

    private fun askPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            KotlinPermissions.with(this)
                .permissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                .onAccepted { permissions ->
                    setView()
                }
                .onDenied { permissions ->
                    askPermission()
                }
                .onForeverDenied { permissions ->
                    Toast.makeText(
                        MainActivity@ this,
                        "You have to grant permissions! Grant them from app settings please.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                .ask()
        } else {
            setView()
        }
    }

    fun setView() {
        btnPick.setOnClickListener(View.OnClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Carbon")
            builder.setMessage("Görseli nereden seçmek istesiniz ?")
            builder.setPositiveButton("Galeri") { dialog, which ->
                dialog.dismiss()
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_GALLERY)
            }
            builder.setNegativeButton("Kamera") { dialog, which ->
                dialog.dismiss()
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (cameraIntent.resolveActivity(packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (ex: IOException) {
                        Log.i("Main", "IOException")
                    }
                    if (photoFile != null) {
                        val builder = StrictMode.VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                        startActivityForResult(cameraIntent, REQUEST_CAMERA)
                    }
                }
            }
            builder.setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        })
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val image = File.createTempFile(
            imageFileName, // prefix
            ".jpg", // suffix
            storageDir      // directory
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.absolutePath
        return image
    }

    companion object {
        const val REQUEST_GALLERY = 1111
        const val REQUEST_CAMERA = 1231
    }

}
