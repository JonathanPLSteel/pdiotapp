package com.specknet.pdiotapp
import android.Manifest // For accessing the Manifest class
import android.content.pm.PackageManager // For checking permissions
import android.os.Environment // For accessing the file system
import android.widget.ArrayAdapter // For setting up the spinner adapter
import android.widget.Spinner // For working with Spinner views
import androidx.core.app.ActivityCompat // For requesting permissions
import androidx.core.content.ContextCompat // For checking permissions
import java.io.File // For working with files
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.CountUpTimer
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import java.io.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.lang.StringBuilder


class HistoricActivity : AppCompatActivity() {
    private val TAG = "HistoricActivity"
    private val REQUEST_CODE_STORAGE_PERMISSION = 100
    private lateinit var dropdown: Spinner
    private lateinit var sitting: TextView
    private lateinit var asc: TextView
    private lateinit var desc: TextView
    private lateinit var lb: TextView
    private lateinit var ls: TextView
    private lateinit var ll: TextView
    private lateinit var lr: TextView
    private lateinit var misc: TextView
    private lateinit var walk: TextView
    private lateinit var run: TextView
    private lateinit var shuffle: TextView
    private lateinit var normal: TextView
    private lateinit var cough: TextView
    private lateinit var hyper: TextView
    private lateinit var other: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: here")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historic)
        checkAndRequestPermissions()
        setupViews()
    }

    private fun setupViews() {
        dropdown = findViewById(R.id.dropdown)
        sitting = findViewById(R.id.sitting_standing)
        asc =  findViewById(R.id.ascending)
        desc =  findViewById(R.id.descending)
        lb =  findViewById(R.id.lying_b)
        ls =  findViewById(R.id.lying_s)
        ll =  findViewById(R.id.lying_l)
        lr =  findViewById(R.id.lying_r)
        misc =  findViewById(R.id.misc)
        walk =  findViewById(R.id.walking)
        run =  findViewById(R.id.running)
        shuffle =  findViewById(R.id.shuffle)
        normal =  findViewById(R.id.normal)
        cough =  findViewById(R.id.coughing)
        hyper =  findViewById(R.id.hyperventilating)
        other =  findViewById(R.id.other)
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        } else {
            populateSpinnerWithFiles()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            populateSpinnerWithFiles()
        }
    }

    private fun getFileNamesFromDirectory(directoryPath: String): List<String> {
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            return directory.listFiles()?.map { it.name } ?: emptyList()
        }
        return emptyList()
    }

    private fun populateSpinnerWithFiles() {
        val directoryPath = Environment.getExternalStorageDirectory().absolutePath + "/MyFolder"
        val fileNames = getFileNamesFromDirectory(directoryPath)

        val spinner: Spinner = findViewById(R.id.dropdown)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fileNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
}
