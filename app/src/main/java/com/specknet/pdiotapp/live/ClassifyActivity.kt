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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.RESpeckLiveData
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random
import org.json.JSONArray
import java.lang.StringBuilder

class ClassifyActivity : AppCompatActivity() {
    private val TAG = "ClassifyingActivity"
    // Define variables for the model and live data here
    private lateinit var resultTextView: TextView
    private lateinit var classifyButton: Button
    private lateinit var fakeDataTextView: TextView
    private lateinit var tflite: Interpreter

    // global graph variables
    lateinit var dataSet_res_accel_x: LineDataSet
    lateinit var dataSet_res_accel_y: LineDataSet
    lateinit var dataSet_res_accel_z: LineDataSet

    lateinit var dataSet_thingy_accel_x: LineDataSet
    lateinit var dataSet_thingy_accel_y: LineDataSet
    lateinit var dataSet_thingy_accel_z: LineDataSet

    lateinit var allRespeckData: LineData

    lateinit var allThingyData: LineData

    lateinit var respeckChart: LineChart
    lateinit var thingyChart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var respeckReceiver: BroadcastReceiver
    lateinit var thingyReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper
    lateinit var looperThingy: Looper

    private lateinit var respeckOutputData: StringBuilder
    private lateinit var thingyOutputData: StringBuilder

    private lateinit var respeckAccel: TextView
    private lateinit var respeckWindows: TextView
    private lateinit var thingyAccel: TextView
    private lateinit var outerArray: MutableList<Array<Float>>

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

    private val handler = Handler(Looper.getMainLooper());
    private var isGeneratingData = false;

    private fun updateRespeckData(liveData: RESpeckLiveData) {
        val output = "[" + liveData.accelX.toString() + "," + liveData.accelY + "," + liveData.accelZ + "]"
        val innerArray = arrayOf(liveData.accelX, liveData.accelY, liveData.accelZ)
        outerArray.add(innerArray)
        respeckOutputData.append(output)
        Log.d(TAG, "updateRespeckData: appended to respeckoutputdata = " + output)
        println(respeckOutputData)
//        if (respeckOutputData.length == 100) {
//            jsonString[window] = respeckOutputData
//        }

//        if (respeckOutputData.length >= 100) {
//            println(respeckOutputData)
//            jsonString = respeckOutputData.toString()
//            var mlInput = parseJsonToFloatArray2(jsonString)
//            runOnUiThread {
//                respeckWindows.text =
//                    getString(R.string.respeck_windows, respeckOutputData.length.floorDiv(100))
//            }
//            respeckOutputData.setLength(0)
//        }

        // update UI thread
        runOnUiThread {
            respeckAccel.text = getString(R.string.respeck_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
        }
        respeckOutputData.append(", ")
    }
    private fun updateThingyData(liveData: ThingyLiveData) {
        val output = liveData.phoneTimestamp.toString() + "," +
                    liveData.accelX + "," + liveData.accelY + "," + liveData.accelZ + "," +
                    liveData.gyro.x + "," + liveData.gyro.y + "," + liveData.gyro.z + "," +
                    liveData.mag.x + "," + liveData.mag.y + "," + liveData.mag.z + "\n"

            thingyOutputData.append(output)
            Log.d(TAG, "updateThingyData: appended to thingyOutputData = " + output)
        // update UI thread
        runOnUiThread {
            thingyAccel.text = getString(R.string.thingy_accel, liveData.accelX, liveData.accelY, liveData.accelZ)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)
        // Initialize views
        resultTextView = findViewById(R.id.result_text_view)
        classifyButton = findViewById(R.id.classify_button)
        fakeDataTextView = findViewById(R.id.fake_data_text_view)
        respeckAccel = findViewById(R.id.respeck_accel)
        respeckWindows = findViewById(R.id.respeck_windows)
        thingyAccel = findViewById(R.id.thingy_accel)
        outerArray = mutableListOf()

        tflite = Interpreter(loadModelFile());

        respeckOutputData = StringBuilder()
        thingyOutputData = StringBuilder()

        respeckReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action = intent.action

                if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {

                    val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    updateRespeckData(liveData)

                    if (outerArray.size == 50) {
                        //println(respeckOutputData)
//                        var jsonString = respeckOutputData.toString()
//                        var jsonStringArray = "[" + jsonString + "]"
//                        var mlinput = parseJsonToFloatArray(jsonStringArray)
                            // Initialize the output array [1,11] (11 classes)
                        println(outerArray)
                            val outputArray = Array(1) { FloatArray(11) }
                            val inputArray: Array<FloatArray> = outerArray.map { it.toFloatArray() }.toTypedArray()
                        println(inputArray)
                        println(inputArray.size)
                            // Run the input data through the model
                            tflite.run(inputArray, outputArray)

                            Log.d("TensorFlow Lite", "Output Array: ${outputArray.contentDeepToString()}")

                            // Get the index of the class with the highest probability
                            val predictedClass = outputArray[0].indices.maxByOrNull { outputArray[0][it] };

                            Log.d("TensorFlow Lite", "Predicted Class: ${classes[predictedClass]}")

                            resultTextView.text = classes[predictedClass];
                            runOnUiThread {
                                respeckWindows.text =
                                    getString(
                                        R.string.respeck_windows,
                                        outerArray.size.floorDiv(50)
                                    )
                            }
                            outerArray.clear()
                        }


                }

            }
        }



        // register receiver on another thread
        val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
        handlerThreadRespeck.start()
        looperRespeck = handlerThreadRespeck.looper
        val handlerRespeck = Handler(looperRespeck)
        this.registerReceiver(respeckReceiver, filterTestRespeck, null, handlerRespeck)


        val inputShape = tflite.getInputTensor(0).shape();
        val inputDataType = tflite.getInputTensor(0).dataType();

        val outputShape = tflite.getOutputTensor(0).shape();
        val outputDataType = tflite.getOutputTensor(0).dataType();

        Log.d("TensorFlow Lite", "Input Shape: ${inputShape.contentToString()}, Data Type: $inputDataType");
        Log.d("TensorFlow Lite", "Output Shape: ${outputShape.contentToString()}, Data Type: $outputDataType")

        // Some fake input data pulled from normal_walking data.
        val jsonData = """
            [[0.020751953, -0.60894775, 0.03289795],
 [0.025878906, -0.6045532, 0.024597168],
 [0.068359375, -0.76031494, 0.10223389],
 [0.005126953, -0.8442993, 0.17108154],
 [-0.056884766, -0.911438, 0.20794678],
 [-0.104003906, -1.0249634, 0.2543335],
 [-0.26708984, -1.2605591, 0.24700928],
 [-0.2512207, -1.4243774, 0.09588623],
 [0.03540039, -1.666565, -0.14141846],
 [-0.08276367, -1.6121216, -0.12677002],
 [0.033447266, -0.8882446, -0.1987915],
 [0.18676758, -0.87164307, -0.13531494],
 [0.025878906, -0.93341064, 0.03656006],
 [-0.28076172, -0.69415283, -0.054992676],
 [-0.083984375, -0.5911255, -0.031555176],
 [-0.08129883, -0.7334595, -0.00079345703],
 [-0.057373047, -0.75372314, 0.0692749],
 [-0.17480469, -0.8179321, 0.11907959],
 [-0.22094727, -0.85040283, 0.18572998],
 [-0.25073242, -0.9692993, 0.13348389],
 [-0.13232422, -1.2559204, 0.2592163],
 [-0.26245117, -1.3877563, 0.418396],
 [-0.34155273, -1.6113892, -0.22589111],
 [0.5703125, -1.567688, -0.0071411133],
 [0.12817383, -0.89801025, -0.17388916],
 [-0.14282227, -0.8569946, -0.00079345703],
 [0.12475586, -1.0010376, 0.10760498],
 [-0.12573242, -0.5864868, -0.101867676],
 [0.0146484375, -0.56573486, 0.06951904],
 [0.088378906, -0.77911377, 0.14202881],
 [-0.048583984, -0.8045044, 0.112976074],
 [0.026855469, -0.8897095, 0.25115967],
 [-0.111328125, -0.8689575, 0.1798706],
 [-0.16210938, -0.958313, 0.24871826],
 [-0.13500977, -1.0579224, 0.22332764],
 [-0.20654297, -1.2857056, 0.38031006],
 [-0.31274414, -1.2896118, 0.27874756],
 [0.1237793, -1.4907837, -0.16583252],
 [0.07763672, -1.378479, -0.39996338],
 [-0.13793945, -0.9209595, -0.16925049],
 [-0.091796875, -1.1836548, 0.21746826],
 [0.064697266, -0.8826294, -0.0032348633],
 [-0.22802734, -0.66656494, 0.0770874],
 [-0.19506836, -0.52593994, -0.0064086914],
 [-0.0126953125, -0.6790161, 0.03363037],
 [-0.19458008, -0.8796997, 0.10345459],
 [-0.25708008, -0.87335205, 0.19500732],
 [-0.13183594, -0.9385376, 0.20697021],
 [0.0017089844, -0.9414673, 0.1449585],
 [-0.22143555, -1.0982056, 0.385437]]"""

        // Convert [50,6] window into [1,50,6]
        val inputArray = arrayOf(parseJsonToFloatArray(jsonData));

        // Initialize the output array [1,7] (7 classes)
        val outputArray = Array(1) { FloatArray(11) }

//        // Run the input data through the model
//        tflite.run(inputArray, outputArray)
//
//        Log.d("TensorFlow Lite", "Output Array: ${outputArray.contentDeepToString()}")
//
//        // Get the index of the class with the highest probability
//        val predictedClass = outputArray[0].indices.maxByOrNull { outputArray[0][it] };
//
//        Log.d("TensorFlow Lite", "Predicted Class: ${classes[predictedClass]}")
//
//        resultTextView.text = classes[predictedClass];

//        classifyButton.setOnClickListener {
//            resultTextView.text = targets.random();
//        }

        startGeneratingFakeData();
    }

    // Load the TensorFlow Lite model from the assets directory as a MappedByteBuffer
    private fun loadModelFile(): MappedByteBuffer {
        // Open the file descriptor for the model file in the assets folder
        val fileDescriptor = assets.openFd("respeck-model.tflite")

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

    private fun startGeneratingFakeData() {
        isGeneratingData = true;
        handler.post(fakeDataRunnable);
    }

    private val fakeDataRunnable = object : Runnable {
        override fun run() {
            if (isGeneratingData) {
                // Generate random fake data for accelerometer (accel_x, accel_y, accel_z)
                val accelX = Random.nextFloat() * 20 - 10  // Random float between -10 and +10
                val accelY = Random.nextFloat() * 20 - 10
                val accelZ = Random.nextFloat() * 20 - 10

                // Generate random fake data for gyroscope (gyro_x, gyro_y, gyro_z)
                val gyroX = Random.nextFloat() * 200 - 100  // Random float between -100 and +100
                val gyroY = Random.nextFloat() * 200 - 100
                val gyroZ = Random.nextFloat() * 200 - 100

                // Display the fake data (for experimentation purposes)
                fakeDataTextView.text = """
                    Accel - X: %.2f, Y: %.2f, Z: %.2f
                    Gyro  - X: %.2f, Y: %.2f, Z: %.2f
                """.trimIndent().format(accelX, accelY, accelZ, gyroX, gyroY, gyroZ)

                // Schedule the next update
                handler.postDelayed(this, 100) // Generate fake data every second
            }
            }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop generating data
        isGeneratingData = false
        handler.removeCallbacks(fakeDataRunnable)

        // Release model resources if needed
        tflite.close();
    }
}