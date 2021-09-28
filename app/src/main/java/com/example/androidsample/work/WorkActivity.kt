package com.example.androidsample.work

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.androidsample.databinding.ActivityWorkBinding

/**
 * Created by fizhu on 23 September 2021
 * https://github.com/Fizhu
 */
class WorkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkBinding
    private val workManager by lazy {
        WorkManager.getInstance(applicationContext)
    }
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onInit()
    }

    private fun createOneTimeWorkRequest() {
        val foregroundWorker = OneTimeWorkRequestBuilder<ForegroundWorker>()
            .setConstraints(constraints)
            .addTag("foregroundWorker")
            .build()
        workManager.enqueueUniqueWork(
            "foregroundWorkerTest",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            foregroundWorker
        )
        workManager.getWorkInfoByIdLiveData(foregroundWorker.id)
            .observe(this, { info ->
                if (info != null && info.state.isFinished) {
                    Log.e("TAG", "createOneTimeWorkRequest: WORK FINISHED")
                }
            })

        binding.btnCancel.setOnClickListener {
            workManager.cancelWorkById(foregroundWorker.id)
        }
    }

    private fun onInit() {
        binding.btn.setOnClickListener {
            createOneTimeWorkRequest()
        }
    }
}