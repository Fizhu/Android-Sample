package com.example.androidsample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.androidsample.databinding.ActivityMainBinding
import com.example.androidsample.work.WorkActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onInit()
    }

    private fun onInit() {
        binding.btnWorkmanager.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    WorkActivity::class.java
                )
            )
        }
    }
}