package com.specknet.pdiotapp.live

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import com.specknet.pdiotapp.R
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import kotlin.random.Random

class ClassifyActivity : AppCompatActivity() {

    // Define variables for the model and live data here
    private lateinit var resultTextView: TextView
    private lateinit var classifyButton: Button
    private lateinit var fakeDataTextView: TextView
//    private lateinit var tflite: Interpreter

    private val targets = arrayOf("walking", "running", "sitting", "standing")
    private val handler = Handler(Looper.getMainLooper());
    private var isGeneratingData = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classify)

        // Initialize views
        resultTextView = findViewById(R.id.result_text_view)
        classifyButton = findViewById(R.id.classify_button)
        fakeDataTextView = findViewById(R.id.fake_data_text_view)

//        tflite = Interpreter(loadModelFile());


        classifyButton.setOnClickListener {
            resultTextView.text = targets.random();
        }

        startGeneratingFakeData();
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
        // model.close()
    }
}