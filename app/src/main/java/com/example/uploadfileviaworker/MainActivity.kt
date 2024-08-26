package com.example.uploadfileviaworker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        textView.text = uri?.toString() ?: "No File Picked"
        this.uri = uri
    }


    private var uri: Uri? = null
    private val textView by lazy { findViewById<TextView>(R.id.textView) }
    private val workManager by lazy { WorkManager.getInstance(this) }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!hasNotificationPermission()) askPermission()


        findViewById<Button>(R.id.button).setOnClickListener {
            pickerLauncher.launch("*/*")
        }

        findViewById<Button>(R.id.button2).setOnClickListener {
            uploadWorker()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun uploadWorker() {

        if (uri != null) {
            val data = Data.Builder().putString("fileUri", uri!!.toString()).build()
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(Constraints(NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            workManager.enqueue(request)

            workManager
                .getWorkInfoByIdLiveData(request.id)
                .observe(this) { workInfo ->
                    if (workInfo != null) {
                        when (workInfo.state) {

                            WorkInfo.State.RUNNING -> {
                                textView.text = "Uploading"
                            }

                            WorkInfo.State.SUCCEEDED -> {
                                workInfo.outputData.getString("response")?.let {
                                    textView.text = it
                                }
                            }

                            WorkInfo.State.FAILED -> {
                                workInfo.outputData.getString("error")?.let {
                                    textView.text = it
                                }
                            }


                            else -> Unit
                        }
                    }
                }

        }

    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askPermission() {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }


    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
        else ActivityCompat.checkSelfPermission(
            baseContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}