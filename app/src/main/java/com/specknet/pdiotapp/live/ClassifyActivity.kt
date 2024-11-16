package com.specknet.pdiotapp.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClassifyActivity : AppCompatActivity() {
    private val TAG = "ClassifyingActivity"
    // Define variables for the model and live data here
    private lateinit var activityResultTextView: TextView
    private lateinit var respiratoryResultTextView: TextView
    private lateinit var activityClassifier: Interpreter
    private lateinit var respiratoryClassifier: Interpreter

    // global broadcast receiver so we can unregister it
    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var thingyReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    private lateinit var activityHistory: StringBuilder


    private lateinit var respeckAccel: TextView
    private lateinit var respeckWindows: TextView
    private lateinit var thingyAccel: TextView

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    private val activityClasses = mapOf(
        0 to "ascending stairs",
        1 to "descending stairs",
        2 to "lying on back",
        3 to "lying on left side",
        4 to "lying on right side",
        5 to "lying on stomach",
        6 to "miscellaneous movement",
        7 to "normal walking",
        8 to "running",
        9 to "shuffle walking",
        10 to "sitting/standing"
    )

    private val respiratoryClasses = mapOf(
        0 to "coughing",
        1 to "hyperventilating",
        2 to "breathing normally",
        3 to "other respiratory condition",
    )

    private val stationaryClasses = arrayOf(2,3,4,5,10);

    private lateinit var activityWindowBuffer: MutableList<FloatArray>;
    private lateinit var respiratoryWindowBuffer: MutableList<FloatArray>;
    private val windowSize = 50;

    private var latestRespeckData: FloatArray? = null;
    private var latestThingyData: FloatArray? = null;

    private var isClassifying = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)

        // Initialize views
        activityResultTextView = findViewById(R.id.activity_result_text_view)
        respiratoryResultTextView = findViewById(R.id.respiratory_result_text_view)
        respeckAccel = findViewById(R.id.respeck_accel)
        respeckWindows = findViewById(R.id.respeck_windows)
        thingyAccel = findViewById(R.id.thingy_accel)

        activityClassifier = Interpreter(loadModelFile("physical-activity-model.tflite"));
        respiratoryClassifier = Interpreter(loadModelFile("respiratory-model.tflite"));

        activityHistory = StringBuilder()

        activityWindowBuffer = mutableListOf();
        respiratoryWindowBuffer = mutableListOf();

        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: respeckliveData = " + liveData)

                    updateRespeckData(liveData)

                }

            }
        }
        thingyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_THINGY_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.THINGY_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: thingyLiveData = " + liveData)

                    updateThingyData(liveData)
                }
            }
        }


        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckReceiver, filterTestRespeck, null, handlerRespeck)

        val thingyHandlerThread = HandlerThread("bgProcThreadThingy")
        thingyHandlerThread.start()
        looperThingy = thingyHandlerThread.looper
        val thingyHandler = Handler(looperThingy)
        this.registerReceiver(thingyReceiver, filterTestThingy, null, thingyHandler)

    }

    private fun updateRespeckData(liveData: RESpeckLiveData) {
        val respeckData = floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ);
        updateWindow(respeckData, null);

        val output = "[" + liveData.accelX.toString() + "," + liveData.accelY + "," + liveData.accelZ + "]"
        runOnUiThread {
            respeckAccel.text = getString(R.string.respeck_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
        }
    }

    private fun updateThingyData(liveData: ThingyLiveData) {
        val thingyData = floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ);
        updateWindow(null, thingyData);

        val output = "[" + liveData.accelX.toString() + "," + liveData.accelY + "," + liveData.accelZ + "]"
        runOnUiThread {
            thingyAccel.text = getString(R.string.thingy_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
        }
    }

    private fun updateWindow(respeckData: FloatArray?, thingyData: FloatArray?) {
        if (respeckData != null) {
            latestRespeckData = respeckData
        }
        if (thingyData != null) {
            latestThingyData = thingyData
        }

        if (latestRespeckData != null && latestThingyData != null) {
            val combinedData = latestRespeckData!! + latestThingyData!!
            activityWindowBuffer.add(combinedData)

            respiratoryWindowBuffer.add(latestRespeckData!!)

            // Remove oldest data if buffer exceeds the window size
            if (activityWindowBuffer.size > windowSize) {
                activityWindowBuffer.removeAt(0)
            }

            // Remove oldest data if buffer exceeds the window size
            if (respiratoryWindowBuffer.size > windowSize) {
                respiratoryWindowBuffer.removeAt(0)
            }

            // Run classification if buffer has enough data
            if (activityWindowBuffer.size == windowSize) {
                isClassifying = true;
                classifyActivity();
                isClassifying = false
            }

            // Reset the latest data for the next round
            latestRespeckData = null;
            latestThingyData = null;
        }
    }

    private fun classifyActivity() {

        if (activityWindowBuffer.size < windowSize || respiratoryWindowBuffer.size < windowSize) {
            Log.e(TAG, "Buffers do not have enough data for classification. Skipping.")
            return
        }

        val activityInputArray = arrayOf(activityWindowBuffer.toTypedArray());
        val activityOutputArray = Array(1) {FloatArray(activityClasses.size)}

        activityClassifier.run(activityInputArray, activityOutputArray);

        val predictedActivityClassIndex = activityOutputArray[0].indices.maxByOrNull { activityOutputArray[0][it] };
        val predictedActivity = activityClasses[predictedActivityClassIndex];
        val currentTimestamp = java.sql.Timestamp(System.currentTimeMillis())
        val activityOutput = currentTimestamp.toString() + "," + predictedActivity +"\n"
        activityHistory.append(activityOutput)

        Log.d(TAG, "Predicted Activity: $predictedActivity")

        var predictedRespiratory = "N/A"

        if (predictedActivityClassIndex in stationaryClasses) {
            Log.d(TAG, "Stationary activity so attempting to predict respiratory...")

            val respiratoryInputArray = arrayOf(respiratoryWindowBuffer.toTypedArray());
            //Log.d(TAG, "Input Array Shape: ${respiratoryInputArray.size},${respiratoryInputArray[0].size},${respiratoryInputArray[0][0].size}")

            val respiratoryOutputArray = Array(1) {FloatArray(respiratoryClasses.size)}
            Log.d(TAG, "Output Array Shape: ${respiratoryOutputArray.size},${respiratoryOutputArray[0].size}")

            respiratoryClassifier.run(respiratoryInputArray, respiratoryOutputArray)

            val predictedRespiratoryClassIndex = respiratoryOutputArray[0].indices.maxByOrNull { respiratoryOutputArray[0][it] };
            predictedRespiratory = respiratoryClasses[predictedRespiratoryClassIndex]!!;
        }

        Log.d(TAG, "Predicted Respiratory: $predictedRespiratory")

        runOnUiThread {
            activityResultTextView.text = predictedActivity ?: "Unknown Activity"
            respiratoryResultTextView.text = predictedRespiratory
        }

        activityWindowBuffer.clear()
        respiratoryWindowBuffer.clear()
    }

    // Load the TensorFlow Lite model from the assets directory as a MappedByteBuffer
    private fun loadModelFile(filename: String): MappedByteBuffer {
        // Open the file descriptor for the model file in the assets folder
        val fileDescriptor = assets.openFd(filename)

        Log.d(TAG, "Loading model file: $filename, Size: ${fileDescriptor.declaredLength}")

        // Create a FileInputStream to read the file using the file descriptor
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

        // Get the file channel from the input stream to enable memory mapping
        val fileChannel = inputStream.channel

        // Retrieve the starting offset in the file where the model data begins
        val startOffset = fileDescriptor.startOffset

        // Retrieve the length of the model file (in bytes) to read it correctly
        val declaredLength = fileDescriptor.declaredLength

        // Map the model file into memory as a read-only MappedByteBuffer
        // This allows efficient access to the model data in memory
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Parse the JSON into a 2D FloatArray
    fun parseJsonToFloatArray(json: String): Array<FloatArray> {
        val jsonArray = JSONArray(json)
        return Array(jsonArray.length()) { i ->
            val innerArray = jsonArray.getJSONArray(i)
            FloatArray(innerArray.length()) { j ->
                innerArray.getDouble(j).toFloat()  // Convert each element to Float
            }
        }
    }
    fun parseJsonToFloatArray2(jsonData: String): Array<FloatArray> {
        val jsonArray = JSONArray(jsonData)
        val floatArray = Array(jsonArray.length()) { FloatArray(3) } // Assuming 50 rows, each with 6 values

        for (i in 0 until jsonArray.length()) {
            val innerArray = jsonArray.getJSONArray(i)
            for (j in 0 until innerArray.length()) {
                floatArray[i][j] = innerArray.getDouble(j).toFloat()  // Convert to Float
            }
        }

        // Wrap it into a new array to get [1, 50, 6] format
        return arrayOf(*floatArray)
    }

    private fun saveRecording() {
        val currentTime = System.currentTimeMillis()
        var formattedDate = ""
        try {
            formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.UK).format(Date())
            Log.i(TAG, "saveRecording: formattedDate = " + formattedDate)
        } catch (e: Exception) {
            Log.i(TAG, "saveRecording: error = ${e.toString()}")
            formattedDate = currentTime.toString()
        }
        val filename = "StrideWise Activity ${formattedDate}.csv" // TODO format this to human readable

        val file = File(getExternalFilesDir(null), filename)

        Log.d(TAG, "saveRecording: filename = " + file.toString())

        val dataWriter: BufferedWriter

        // Create file for current day and append header, if it doesn't exist yet
        try {
            val exists = file.exists()
            dataWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file, true)))

            if (!exists) {
                Log.d(TAG, "saveRecording: filename doesn't exist")

                // the header columns in here
                dataWriter.append("Timestamp" + "," + "Recorded Activity")
                dataWriter.newLine()
                dataWriter.flush()
            }
            else {
                Log.d(TAG, "saveRecording: filename exists")
            }
            if (activityHistory.isNotEmpty()) {
                dataWriter.write(activityHistory.toString())
                dataWriter.flush()

                Log.d(TAG, "saveRecording: recording saved")
            }
            else {
                Log.d(TAG, "saveRecording: no data during recording period")
            }

            dataWriter.close()

            activityHistory = StringBuilder()

            Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
        }
        catch (e: IOException) {
            Toast.makeText(this, "Error while saving recording!", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "saveRespeckRecording: Error while writing to the respeck file: " + e.message )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        looperThingy.quit()
        looperRespeck.quit()

        saveRecording()

        activityClassifier.close();
        respiratoryClassifier.close();
    }
}