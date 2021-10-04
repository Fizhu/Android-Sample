package com.example.androidsample.hiddencam

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.androidsample.databinding.ActivityHiddenCamBinding


/**
 * Created by fizhu on 23 September 2021
 * https://github.com/Fizhu
 */
class HiddenCamctivity : AppCompatActivity() {

    private lateinit var binding: ActivityHiddenCamBinding

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
        binding.btn.setOnClickListener {
            startCamera()
        }
    }
}