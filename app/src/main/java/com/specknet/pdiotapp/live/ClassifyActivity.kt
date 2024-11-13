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
import android.widget.Button
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.json.JSONArray

class ClassifyActivity : AppCompatActivity() {
    private val TAG = "ClassifyingActivity"
    // Define variables for the model and live data here
    private lateinit var resultTextView: TextView
    private lateinit var classifyButton: Button
    private lateinit var tflite: Interpreter

    // global broadcast receiver so we can unregister it
    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var thingyReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    private lateinit var respeckAccel: TextView
    private lateinit var respeckWindows: TextView
    private lateinit var thingyAccel: TextView

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)
    val filterTestThingy = IntentFilter(Constants.ACTION_THINGY_BROADCAST)

    private val classes = mapOf(
        0 to "ascending_stairs",
        1 to "descending_stairs",
        2 to "lying_back",
        3 to "lying_left",
        4 to "lying_right",
        5 to "lying_stomach",
        6 to "misc_movement",
        7 to "normal_walking",
        8 to "running",
        9 to "shuffle_walking",
        10 to "sitting_standing"
    )

    private lateinit var slidingWindowBuffer: MutableList<FloatArray>;
    private val windowSize = 50;
    private val activityFeatures = 6;

    private var latestRespeckData: FloatArray? = null;
    private var latestThingyData: FloatArray? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)

        // Initialize views
        resultTextView = findViewById(R.id.result_text_view)
        classifyButton = findViewById(R.id.classify_button)
        respeckAccel = findViewById(R.id.respeck_accel)
        respeckWindows = findViewById(R.id.respeck_windows)
        thingyAccel = findViewById(R.id.thingy_accel)

        tflite = Interpreter(loadModelFile());

        slidingWindowBuffer = mutableListOf();

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
        Log.d(TAG, "updateRespeckData: appended to respeckoutputdata = " + output)
        // update UI thread
        runOnUiThread {
            respeckAccel.text = getString(R.string.respeck_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
        }
    }
    private fun updateThingyData(liveData: ThingyLiveData) {
        val thingyData = floatArrayOf(liveData.accelX, liveData.accelY, liveData.accelZ);
        updateWindow(null, thingyData);

        val output = "[" + liveData.accelX.toString() + "," + liveData.accelY + "," + liveData.accelZ + "]"
        Log.d(TAG, "updateThingyData: appended to thingyOutputData = " + output)
        // update UI thread
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
            slidingWindowBuffer.add(combinedData)

            // Remove oldest data if buffer exceeds the window size
            if (slidingWindowBuffer.size > windowSize) {
                slidingWindowBuffer.removeAt(0)
            }

            // Run classification if buffer has enough data
            if (slidingWindowBuffer.size == windowSize) {
                classifyActivity();
            }

            // Reset the latest data for the next round
            latestRespeckData = null;
            latestThingyData = null;
        }
    }

    private fun classifyActivity() {
        val inputArray = arrayOf(slidingWindowBuffer.toTypedArray());
        val outputArray = Array(1) {FloatArray(classes.size)}

        tflite.run(inputArray, outputArray);

        val predictedClassIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] };
        val predictedClass = classes[predictedClassIndex];

        runOnUiThread {
            resultTextView.text = predictedClass ?: "Unknown Activity"
            Log.d(TAG, "Predicted Activity: $predictedClass")
        }

        slidingWindowBuffer.clear();
    }

    // Load the TensorFlow Lite model from the assets directory as a MappedByteBuffer
    private fun loadModelFile(): MappedByteBuffer {
        // Open the file descriptor for the model file in the assets folder
        val fileDescriptor = assets.openFd("physical-activity-model.tflite")

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

    override fun onDestroy() {
        super.onDestroy()

        // Release model resources if needed
        tflite.close();
    }
}