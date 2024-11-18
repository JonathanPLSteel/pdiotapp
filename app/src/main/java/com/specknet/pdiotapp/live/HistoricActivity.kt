package com.specknet.pdiotapp
import android.widget.ArrayAdapter // For setting up the spinner adapter
import android.widget.Spinner // For working with Spinner views
import java.io.File // For working with files
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.*
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
                Log.d(TAG, "No files in dir")
            }

            if (fileNames!=null && fileNames.isEmpty()) {
                Toast.makeText(this, "You have no recorded data.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Directory does not exist")
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
            "shuffle walking"
        )
        val respiratoryConditions = listOf(
            "breathing normally",
            "coughing",
            "hyperventilating",
            "other respiratory condition"
        )

        val activitiesTally = mutableMapOf<String, Int>().apply {
            activities.forEach { this[it] = 0 }
        }

        val respiratoryTally = mutableMapOf<String, Int>().apply {
            respiratoryConditions.forEach { this[it] = 0 }
        }

        Log.d(TAG, "openFile: $file")

        try {
            val content = readFileToString(file)
            val splitData = content.split(",") // Split the content by commas
            splitData.forEach { line ->

                Log.d(TAG, "openFile: $line")
                // Assuming each line represents an activity (or a column contains activity labels)
                activities.forEach { activity ->
                    if (line.contains(activity, ignoreCase = true)) {
                        activitiesTally[activity] = activitiesTally[activity]!! + 1
                    }
                }

                respiratoryConditions.forEach { condition ->
                    if (line.contains(condition, ignoreCase = true)) {
                        respiratoryTally[condition] = respiratoryTally[condition]!! + 1
                    }
                }
            }
            Toast.makeText(this, "Read File!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show()
        }
        writeToViews(activitiesTally, respiratoryTally)
    }

    private fun writeToViews(activitiesTally: MutableMap<String, Int>, respiratoryTally: MutableMap<String, Int>) {

        Log.d(TAG, "writeToViews: here")

        val activityPercentages = calculateActivityPercentages(activitiesTally)
        val respiratoryPercentages = calculateActivityPercentages(respiratoryTally)

        runOnUiThread{
            sitting.text = "Sitting/Standing: ${String.format("%.0f",activityPercentages["sitting/standing"])}%"
            asc.text = "Ascending stairs: ${String.format("%.0f",activityPercentages["ascending stairs"])}%"
            desc.text = "Descending stairs: ${String.format("%.0f",activityPercentages["descending stairs"])}%"
            lb.text = "Lying on back: ${String.format("%.0f",activityPercentages["lying on back"])}%"
            ls.text = "Lying on stomach: ${String.format("%.0f",activityPercentages["lying on stomach"])}%"
            ll.text = "Lying on left: ${String.format("%.0f",activityPercentages["lying on left side"])}%"
            lr.text = "Lying on right ${String.format("%.0f",activityPercentages["lying on right side"])}%"
            misc.text = "Misc movements: ${String.format("%.0f",activityPercentages["miscellaneous movement"])}%"
            walk.text = "Walking: ${String.format("%.0f",activityPercentages["normal walking"])}%"
            run.text =  "Running: ${String.format("%.0f",activityPercentages["running"])}%"
            shuffle.text =  "Shuffle walking: ${String.format("%.0f",activityPercentages["shuffle walking"])}%"
            normal.text =  "Normal Breathing: ${String.format("%.0f",respiratoryPercentages["breathing normally"])}%"
            cough.text =  "Coughing: ${String.format("%.0f",respiratoryPercentages["coughing"])}%"
            hyper.text =  "Hyperventilating: ${String.format("%.0f",respiratoryPercentages["hyperventilating"])}%"
            other.text =  "Other resp condition: ${String.format("%.0f",respiratoryPercentages["other respiratory condition"])}%"
        }
    }

    fun calculateActivityPercentages(tallies: MutableMap<String, Int>): Map<String, Double> {
        // Calculate the total tally
        val totalTallies = tallies.values.sum()

        // Check if the total is greater than 0 to avoid division by zero
        if (totalTallies == 0) {
            return tallies.mapValues { 0.0 } // Map all percentages to 0.0 if no activities were completed
        }

        // Calculate percentages for each activity
        return tallies.mapValues { (activity, tally) ->
            (tally.toDouble() / totalTallies) * 100
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
