package com.example.androidsample.hiddencam

import android.os.Bundle
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


    private fun onInit() {
        binding.btn.setOnClickListener {
        }
    }
}