package com.specknet.pdiotapp.live

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import com.specknet.pdiotapp.R
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random
import org.json.JSONArray

class ClassifyActivity : AppCompatActivity() {

    // Define variables for the model and live data here
    private lateinit var resultTextView: TextView
    private lateinit var classifyButton: Button
    private lateinit var fakeDataTextView: TextView
    private lateinit var tflite: Interpreter

    private val classes = mapOf(
        0 to "ascending_stairs",
        1 to "shuffle_walking",
        2 to "sitting_standing",
        3 to "misc_movement",
        4 to "normal_walking",
        5 to "lying_down",
        6 to "descending_stairs"
    )

    private val handler = Handler(Looper.getMainLooper());
    private var isGeneratingData = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)

        // Initialize views
        resultTextView = findViewById(R.id.result_text_view)
        classifyButton = findViewById(R.id.classify_button)
        fakeDataTextView = findViewById(R.id.fake_data_text_view)

        tflite = Interpreter(loadModelFile());

        val inputShape = tflite.getInputTensor(0).shape();
        val inputDataType = tflite.getInputTensor(0).dataType();

        val outputShape = tflite.getOutputTensor(0).shape();
        val outputDataType = tflite.getOutputTensor(0).dataType();

        Log.d("TensorFlow Lite", "Input Shape: ${inputShape.contentToString()}, Data Type: $inputDataType");
        Log.d("TensorFlow Lite", "Output Shape: ${outputShape.contentToString()}, Data Type: $outputDataType")

        // Some fake input data pulled from normal_walking data.
        val jsonData = """
            [[0.020751953, -0.60894775, 0.03289795, -2.328125, -22.78125, 1.296875],
 [0.025878906, -0.6045532, 0.024597168, 15.96875, -5.484375, -4.625],
 [0.068359375, -0.76031494, 0.10223389, -0.625, -6.75, -2.578125],
 [0.005126953, -0.8442993, 0.17108154, 4.9375, -2.09375, -5.34375],
 [-0.056884766, -0.911438, 0.20794678, -0.953125, -9.0625, -8.234375],
 [-0.104003906, -1.0249634, 0.2543335, 5.359375, -3.328125, -11.1875],
 [-0.26708984, -1.2605591, 0.24700928, -3.859375, 0.734375, -14.4375],
 [-0.2512207, -1.4243774, 0.09588623, -4.03125, -3.578125, -20.609375],
 [0.03540039, -1.666565, -0.14141846, -17.78125, -25.3125, -18.5],
 [-0.08276367, -1.6121216, -0.12677002, 3.375, 31.25, 0.890625],
 [0.033447266, -0.8882446, -0.1987915, -9.125, 16.9375, -27.265625],
 [0.18676758, -0.87164307, -0.13531494, 5.6875, 15.125, -2.53125],
 [0.025878906, -0.93341064, 0.03656006, 1.234375, 12.59375, 7.640625],
 [-0.28076172, -0.69415283, -0.054992676, -7.53125, 14.765625, 2.109375],
 [-0.083984375, -0.5911255, -0.031555176, 0.828125, 13.0625, -2.125],
 [-0.08129883, -0.7334595, -0.00079345703, -7.328125, 3.703125, 4.78125],
 [-0.057373047, -0.75372314, 0.0692749, 4.6875, 1.6875, 6.203125],
 [-0.17480469, -0.8179321, 0.11907959, -7.375, -3.5, 9.59375],
 [-0.22094727, -0.85040283, 0.18572998, 3.390625, 5.53125, 8.53125],
 [-0.25073242, -0.9692993, 0.13348389, -4.015625, 3.1875, 8.203125],
 [-0.13232422, -1.2559204, 0.2592163, 7.15625, 6.59375, 1.53125],
 [-0.26245117, -1.3877563, 0.418396, -12.234375, -0.734375, 9.5625],
 [-0.34155273, -1.6113892, -0.22589111, -47.4375, -34.6875, -5.953125],
 [0.5703125, -1.567688, -0.0071411133, 37.34375, 17.484375, -7.28125],
 [0.12817383, -0.89801025, -0.17388916, -35.515625, -6.578125, 15.578125],
 [-0.14282227, -0.8569946, -0.00079345703, 32.15625, 3.75, -4.8125],
 [0.12475586, -1.0010376, 0.10760498, -22.765625, -21.46875, 1.0],
 [-0.12573242, -0.5864868, -0.101867676, -14.453125, -4.640625, 0.90625],
 [0.0146484375, -0.56573486, 0.06951904, 15.703125, 9.234375, -0.21875],
 [0.088378906, -0.77911377, 0.14202881, 4.3125, -4.640625, 3.65625],
 [-0.048583984, -0.8045044, 0.112976074, 15.515625, -1.640625, 0.0],
 [0.026855469, -0.8897095, 0.25115967, 9.140625, -3.453125, -0.6875],
 [-0.111328125, -0.8689575, 0.1798706, -0.609375, -2.796875, -1.171875],
 [-0.16210938, -0.958313, 0.24871826, -0.71875, 0.484375, -8.046875],
 [-0.13500977, -1.0579224, 0.22332764, 2.03125, -5.109375, -10.6875],
 [-0.20654297, -1.2857056, 0.38031006, 4.890625, 5.09375, -12.3125],
 [-0.31274414, -1.2896118, 0.27874756, -10.765625, 4.84375, -15.296875],
 [0.1237793, -1.4907837, -0.16583252, -37.46875, -4.390625, -19.359375],
 [0.07763672, -1.378479, -0.39996338, -5.8125, -2.453125, 7.234375],
 [-0.13793945, -0.9209595, -0.16925049, 42.75, 24.6875, 4.078125],
 [-0.091796875, -1.1836548, 0.21746826, 0.234375, 2.3125, -2.9375],
 [0.064697266, -0.8826294, -0.0032348633, -0.03125, 6.234375, 4.328125],
 [-0.22802734, -0.66656494, 0.0770874, 2.796875, 12.53125, 13.046875],
 [-0.19506836, -0.52593994, -0.0064086914, -15.671875, 0.40625, 3.46875],
 [-0.0126953125, -0.6790161, 0.03363037, 16.03125, 8.359375, 5.3125],
 [-0.19458008, -0.8796997, 0.10345459, -0.9375, 0.78125, 11.546875],
 [-0.25708008, -0.87335205, 0.19500732, 4.40625, 9.140625, 2.734375],
 [-0.13183594, -0.9385376, 0.20697021, -8.421875, -0.234375, 0.953125],
 [0.0017089844, -0.9414673, 0.1449585, -1.421875, 0.203125, 4.90625],
 [-0.22143555, -1.0982056, 0.385437, 2.890625, -1.484375, 7.09375]]"""

        // Convert [50,6] window into [1,50,6]
        val inputArray = arrayOf(parseJsonToFloatArray(jsonData));

        // Initialize the output array [1,7] (7 classes)
        val outputArray = Array(1) { FloatArray(7) }

        // Run the input data through the model
        tflite.run(inputArray, outputArray)

        Log.d("TensorFlow Lite", "Output Array: ${outputArray.contentDeepToString()}")

        // Get the index of the class with the highest probability
        val predictedClass = outputArray[0].indices.maxByOrNull { outputArray[0][it] };

        Log.d("TensorFlow Lite", "Predicted Class: ${classes[predictedClass]}")

        resultTextView.text = classes[predictedClass];

//        classifyButton.setOnClickListener {
//            resultTextView.text = targets.random();
//        }

        startGeneratingFakeData();
    }

    // Load the TensorFlow Lite model from the assets directory as a MappedByteBuffer
    private fun loadModelFile(): MappedByteBuffer {
        // Open the file descriptor for the model file in the assets folder
        val fileDescriptor = assets.openFd("model.tflite")

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