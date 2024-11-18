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
    private lateinit var context: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: here")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historic)
        setupViews()
        populateSpinnerWithFiles(context)

        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFileName = parentView.getItemAtPosition(position) as String
                openFile(selectedFileName)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Handle case where nothing is selected
            }
        }
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
        context = this
    }

    private fun populateSpinnerWithFiles(context: Context) {
        val directoryPath = context.getExternalFilesDir(null)
        Log.d(TAG, "$directoryPath")
        if (directoryPath != null && directoryPath.exists()) {
            val fileNames = directoryPath.listFiles()
            if (fileNames != null) {
                // Convert the File array to a List of strings (file names or paths)
                val files: List<String> = fileNames?.map { it.name } ?: emptyList()

                // If you want full paths instead of just file names, you can use it.path
                val filePaths: List<String> = fileNames?.map { it.path } ?: emptyList()
                val spinner: Spinner = findViewById(R.id.dropdown)
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, files)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            } else {
                println("No files in dir")
            }
        } else {
            println("Dir doesn't exist")
        }

    }

    private fun openFile (fileName: String) {
        val file = File(getExternalFilesDir(null), fileName)
        val activities = listOf(
            "sitting/standing",
            "ascending stairs",
            "descending stairs",
            "lying on back",
            "lying on stomach",
            "lying on left side",
            "lying on right side",
            "miscellaneous movement",
            "normal walking",
            "running",
            "shuffle walking",
            "breathing normally",
            "coughing",
            "hyperventilating",
            "other respiratory condition"
        )
        val tally = mutableMapOf<String, Int>().apply {
            activities.forEach { this[it] = 0 }
        }

        try {
            val content = readFileToString(file)
            val splitData = content.split(",") // Split the content by commas
            splitData.forEach { line ->
                // Assuming each line represents an activity (or a column contains activity labels)
                activities.forEach { activity ->
                    if (line.contains(activity, ignoreCase = true)) {
                        // Increment the count for the activity
                        tally[activity] = tally[activity]!! + 1
                    }
                }
            }
            Toast.makeText(this, "File content split: ${splitData.joinToString(", ")}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show()
        }
        writeToViews(tally)
    }

    private fun writeToViews(tally: MutableMap<String, Int>) {

        runOnUiThread{
            sitting.text = null
            asc.text = null
            desc.text = null
            lb.text = null
            ls.text = null
            ll.text = null
            lr.text = null
            misc.text = null
            walk =  findViewById(R.id.walking)
            run =  findViewById(R.id.running)
            shuffle =  findViewById(R.id.shuffle)
            normal =  findViewById(R.id.normal)
            cough =  findViewById(R.id.coughing)
            hyper =  findViewById(R.id.hyperventilating)
            other =  findViewById(R.id.other)
        }
    }

    private fun readFileToString(file: File): String {
        val stringBuilder = StringBuilder()
        try {
            val bufferedReader = BufferedReader(InputStreamReader(file.inputStream()))
            bufferedReader.use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n") // Add a newline after each line
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

}
