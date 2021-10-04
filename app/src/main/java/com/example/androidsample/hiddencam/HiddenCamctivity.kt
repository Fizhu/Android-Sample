package com.example.androidsample.hiddencam

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.androidsample.databinding.ActivityHiddenCamBinding
import com.example.androidsample.hiddencam.listeners.PictureCapturingListener
import com.example.androidsample.hiddencam.services.APictureCapturingService
import com.example.androidsample.hiddencam.services.PictureCapturingServiceImpl
import java.util.*


/**
 * Created by fizhu on 23 September 2021
 * https://github.com/Fizhu
 */
class HiddenCamctivity : AppCompatActivity(), PictureCapturingListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var binding: ActivityHiddenCamBinding
    private val requiredPermissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    private val MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1

    private val uploadBackPhoto: ImageView? = null
    private val uploadFrontPhoto: ImageView? = null

    //The capture service
    private var pictureService: APictureCapturingService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHiddenCamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onInit()
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            if (Settings.canDrawOverlays(this)) {
                startCamera()
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, 5566)
            }
        }
    }

    private fun startCamera() {
        val frontTranslucent = Intent(
            application
                .applicationContext, CameraService::class.java
        )
        frontTranslucent.putExtra("Front_Request", true)
        frontTranslucent.putExtra(
            "Quality_Mode", 100
        )
        application.applicationContext.startService(
            frontTranslucent
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startCamera()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onInit() {
        // getting instance of the Service from PictureCapturingServiceImpl
        pictureService = PictureCapturingServiceImpl.getInstance(this)
        binding.startCaptureBtn.setOnClickListener {
            showToast("Starting capture!")
            pictureService?.startCapturing(this)
        }
    }

    override fun onDoneCapturingAllPhotos(picturesTaken: TreeMap<String, ByteArray>?) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!")
            return
        }
        showToast("No camera detected!")
    }

    override fun onCaptureDone(pictureUrl: String?, pictureData: ByteArray?) {
        if (pictureData != null && pictureUrl != null) {
            runOnUiThread {
                val bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.size)
                val nh = (bitmap.height * (512.0 / bitmap.width)).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, 512, nh, true)
                if (pictureUrl.contains("0_pic.jpg")) {
                    uploadBackPhoto!!.setImageBitmap(scaled)
                } else if (pictureUrl.contains("1_pic.jpg")) {
                    uploadFrontPhoto!!.setImageBitmap(scaled)
                }
            }
            showToast("Picture saved to $pictureUrl")
        }
    }

    private fun showToast(text: String) {
        runOnUiThread { Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_CODE -> {
                if (!(grantResults.isNotEmpty()
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) {
                    checkPermissions()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * checking  permissions at Runtime.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        val neededPermissions: MutableList<String> = ArrayList()
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(permission)
            }
        }
        if (neededPermissions.isNotEmpty()) {
            requestPermissions(
                neededPermissions.toTypedArray(),
                MY_PERMISSIONS_REQUEST_ACCESS_CODE
            )
        }
    }
}