/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */
package com.labters.documentscanner

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.labters.documentscanner.base.CropperErrorType
import com.labters.documentscanner.base.DocumentScanActivity
import com.labters.documentscanner.helpers.ScannerConstants
import com.labters.documentscanner.libraries.PolygonView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageCropActivity : DocumentScanActivity() {
    private lateinit var holderImageCrop: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var polygonView: PolygonView
    private var isInverted = false
    private lateinit var progressBar: ProgressBar
    private var cropImage: Bitmap? = null
    private var cancelTextResId: Int? = null
    private var doneTextResId: Int? = null

    private val btnImageEnhanceClick =
        View.OnClickListener {
            showProgressBar()
            disposable.add(
                Observable.just(croppedImage)
                    .map {
                        val cropImage = croppedImage
                        if (ScannerConstants.saveStorage) cropImage.also { saveToInternalStorage(it) }
                        cropImage
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { cropImage ->
                        hideProgressBar()
                        if (cropImage != null) {
                            //ScannerConstants.selectedImageBitmap = cropImage
                            val imgPath = AppUtils.getMediaPath(cropImage, this@ImageCropActivity)
                            val intent = Intent()
                            intent.putExtra(RESULT_IMAGE_PATH, imgPath)

                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    }
            )
        }

    private val btnRebase =
        View.OnClickListener {
            startCropping()
        }

    private val btnCloseClick =
        View.OnClickListener { v: View? -> finish() }
    private val btnInvertColor =
        View.OnClickListener {
            showProgressBar()
            disposable.add(
                Observable.fromCallable {
                    invertColor()
                    false
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        hideProgressBar()
                        val scaledBitmap = scaledBitmap(
                            cropImage,
                            holderImageCrop.width,
                            holderImageCrop.height
                        )
                        imageView.setImageBitmap(scaledBitmap)
                    }
            )
        }
    private val onRotateClick =
        View.OnClickListener {
            showProgressBar()
            disposable.add(
                Observable.fromCallable {
                    if (isInverted) invertColor()
                    cropImage = rotateBitmap(cropImage, 90f)
                    false
                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        hideProgressBar()
                        startCropping()
                    }
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)
        doneTextResId = intent.getIntExtra(DONE_TEXT_RES_ID, R.string.no_name)
        cancelTextResId = intent.getIntExtra(CANCEL_TEXT_RES_ID, R.string.no_name)


        cropImage = BitmapFactory.decodeFile(intent.getStringExtra(IMAGE_PATH))
        isInverted = false
        initView()
    }

    override fun getHolderImageCrop(): FrameLayout {
        return holderImageCrop
    }

    override fun getImageView(): ImageView {
        return imageView
    }

    override fun getPolygonView(): PolygonView {
        return polygonView
    }

    override fun showProgressBar() {
        val rlContainer =
            findViewById<RelativeLayout>(R.id.rlContainer)
        setViewInteract(rlContainer, false)
        progressBar.visibility = View.VISIBLE
    }

    override fun hideProgressBar() {
        val rlContainer =
            findViewById<RelativeLayout>(R.id.rlContainer)
        setViewInteract(rlContainer, true)
        progressBar.visibility = View.GONE
    }

    override fun showError(errorType: CropperErrorType) {
        when (errorType) {
            CropperErrorType.CROP_ERROR -> Toast.makeText(
                this,
                ScannerConstants.cropError,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun getBitmapImage(): Bitmap? {
        return cropImage
    }

    private fun setViewInteract(view: View, canDo: Boolean) {
        view.isEnabled = canDo
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setViewInteract(view.getChildAt(i), canDo)
            }
        }
    }

    private fun initView() {
        val btnImageCrop =
            findViewById<Button>(R.id.btnImageCrop)
        val btnClose =
            findViewById<Button>(R.id.btnClose)
        holderImageCrop =
            findViewById(R.id.holderImageCrop)
        imageView =
            findViewById(R.id.imageView)
        val ivRotate =
            findViewById<ImageView>(R.id.ivRotate)
        val ivInvert =
            findViewById<ImageView>(R.id.ivInvert)
        val ivRebase =
            findViewById<ImageView>(R.id.ivRebase)

        doneTextResId?.also { btnImageCrop.setText(it) }
        cancelTextResId?.also { btnClose.setText(it) }

        polygonView = findViewById(R.id.polygonView)
        progressBar =
            findViewById(R.id.progressBar)
        if (progressBar.indeterminateDrawable != null && ScannerConstants.progressColor != null) progressBar.indeterminateDrawable
            ?.setColorFilter(
                Color.parseColor(ScannerConstants.progressColor),
                PorterDuff.Mode.MULTIPLY
            ) else if (progressBar.progressDrawable != null && ScannerConstants.progressColor != null) progressBar.progressDrawable
            ?.setColorFilter(
                Color.parseColor(ScannerConstants.progressColor),
                PorterDuff.Mode.MULTIPLY
            )
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor))
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor))
        btnImageCrop.setOnClickListener(btnImageEnhanceClick)
        btnClose.setOnClickListener(btnCloseClick)
        ivRotate.setOnClickListener(onRotateClick)
        ivInvert.setOnClickListener(btnInvertColor)
        ivRebase.setOnClickListener(btnRebase)
        startCropping()
    }

    private fun invertColor() {
        if (!isInverted) {
            cropImage?.also { cropImage ->
                val bmpMonochrome = Bitmap.createBitmap(
                    cropImage.width,
                    cropImage.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bmpMonochrome)
                val ma = ColorMatrix()
                ma.setSaturation(0f)
                val paint = Paint()
                paint.colorFilter = ColorMatrixColorFilter(ma)
                canvas.drawBitmap(cropImage, 0f, 0f, paint)
                this.cropImage = bmpMonochrome.copy(bmpMonochrome.config, true)
            }
        } else {
            cropImage = cropImage?.copy(cropImage?.config, true)
        }
        isInverted = !isInverted
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap): String {
        val directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
        val imageFileName = "cropped_$timeStamp.png"
        val myPath = File(directory, imageFileName)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return directory.absolutePath
    }

    companion object {
        const val CROP_IMAGE = 1523
        private const val IMAGE_PATH = "image_path"
        private const val CANCEL_TEXT_RES_ID = "CANCEL_TEXT_RES_ID"
        private const val DONE_TEXT_RES_ID = "DONE_TEXT_RES_ID"
        const val RESULT_IMAGE_PATH = "result_image_path"

        fun startCropping(
            activity: Activity,
            imagePath: String,
            cancelTextResId: Int,
            doneTextResId: Int
        ) {
            activity.startActivityForResult(
                Intent(
                    activity,
                    ImageCropActivity::class.java
                ).apply {
                    this.putExtra(IMAGE_PATH, imagePath)
                    this.putExtra(CANCEL_TEXT_RES_ID, cancelTextResId)
                    this.putExtra(DONE_TEXT_RES_ID, doneTextResId)
                },
                CROP_IMAGE
            )
        }

    }

}