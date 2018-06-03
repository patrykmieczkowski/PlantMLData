package com.mieczkowskidev.plantmldata

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    private val TAKE_PHOTO_REQUEST = 101
    private var mCurrentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_button.setOnClickListener { _ -> checkPermissions() }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == TAKE_PHOTO_REQUEST) {
            val cursor = contentResolver.query(Uri.parse(mCurrentPhotoPath),
                    Array(1) { android.provider.MediaStore.Images.ImageColumns.DATA },
                    null, null, null)
            cursor.moveToFirst()
            val photoPath = cursor.getString(0)
            cursor.close()
            val file = File(photoPath)
            val uri = Uri.fromFile(file)

            Log.d(TAG, "uri: ${uri.path}")
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

            checkPicture(bitmap)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPicture(bitmap: Bitmap?) {
        Log.d(TAG, "check picture from bitmap")

        val image = FirebaseVisionImage.fromBitmap(bitmap!!)

        val detector = FirebaseVision.getInstance()
                .visionLabelDetector

        // custom settings for fb
        val detector2 = firebaseSettings()

        detector.detectInImage(image)
                .addOnSuccessListener { data ->
                    run {
                        Log.d(TAG, "list size: ${data.size}")
                        val stringBuilder = StringBuilder()
                        data.forEach { x ->
                            run {
                                stringBuilder.append("entity: ${x.entityId}, label: ${x.label}, confidence: ${x.confidence} \n")
                            }
                        }


                        main_text.text = stringBuilder.toString()

                        Glide.with(this)
                                .load(bitmap)
                                .into(main_image)

                    }
                }
                .addOnFailureListener { fail -> Log.e(TAG, "failed: ", fail.fillInStackTrace()) }

    }

    private fun checkPermissions() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                        launchCamera()
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?,
                                                                    token: PermissionToken?) {
                        AlertDialog.Builder(this@MainActivity)
                                .setTitle("Give permissions")
                                .setMessage("Write storage")
                                .setNegativeButton(android.R.string.cancel,
                                        { dialog, _ ->
                                            dialog.dismiss()
                                            token?.cancelPermissionRequest()
                                        })
                                .setPositiveButton(android.R.string.ok,
                                        { dialog, _ ->
                                            dialog.dismiss()
                                            token?.continuePermissionRequest()
                                        })
                                .setOnDismissListener({ token?.cancelPermissionRequest() })
                                .show()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        Log.e(TAG, "No permissions - denied")
                    }
                })
                .check()

    }

    private fun launchCamera() {
        val values = ContentValues(1)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        val fileUri = contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            mCurrentPhotoPath = fileUri.toString()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        }
    }

    private fun firebaseSettings() = FirebaseVisionLabelDetectorOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

}
