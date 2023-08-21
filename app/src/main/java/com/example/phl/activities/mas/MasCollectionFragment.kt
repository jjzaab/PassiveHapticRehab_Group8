package com.example.phl.activities.mas

import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.phl.R
import com.example.phl.data.AppDatabase
import com.example.phl.data.mas.MasTestRaw
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.sqrt


class MasCollectionFragment : Fragment(), SensorEventListener {

    companion object {
        const val MAXIMUM_TEST_DURATION = 20000L // The maximum duration of the test in milliseconds. If time limit exceeded, the test is invalid.
        const val MIN_MOVEMENT_THRESHOLD = 0.2 // The minimum acceleration value to be considered movement.
        const val HAND_STILL_TIME_WARMUP = 1000L // The amount of time the hand must be still during the WARMUP stage to move to the next stage.
        const val HAND_STILL_TIME_INITIAL = 1000L // The amount of time the hand must be still during the INITIAL_1 stage for the test to be valid.
        const val HAND_MOVING_TIME_INITIAL = 1000L // The amount of time the hand must be moving during the INITIAL_2 stage to move to the next stage.
        const val HAND_MOVING_TIME_MIDDLE = 1000L // The amount of time the hand must be moving during the MIDDLE stage for the test to be valid.
        const val HAND_STILL_TIME_FINAL = 2000L // The amount of time the hand must be still at the FINAL stage to move to the next stage.

        enum class Stage {
            NONE,
            // When the acceleration is too large during warmup,
            // the phone would not be able to accurately estimate the rotation.
            // That is, the phone does not know the "up" direction correctly and may give wrong results.
            // Therefore, we need to restart the warmup process.
            WARMUP, // User hand is still. Only the accelerometer data is collected. The data is not saved. We perform a state check to see if the hand is still. If it is, we move to the next stage.
            INITIAL_1, // User hand is still. The data is not saved. We perform a state check to see if the hand is still for some time. If it is, we move to the next stage.
            INITIAL_2, // User hand is still but may start moving at any time. The data is saved. When the user moves their hand for some time, we move to the next stage.
            MIDDLE, // User hand is moving. The data is saved. When the user moves their hand for a certain amount of time, we move to the next stage.
            FINAL, // User hand is moving and may stop at any time. The data is saved. When the hand stops moving for a certain amount of time, we move to the next stage.
            WRAPUP, // User hand is still. The end. The test is complete. We turn off the sensors and save the data.
            INVALID, // The test is invalid. This could be because the user does not move their hand, moves their hand for too short a time, or the test takes too long.
        }
    }

    private var dataBuffer = ArrayList<MasTestRaw>() // The buffer of data to be saved. We copy the data to dataToSave when we move to the next stage.
    private var accelerationDataBufferWindow = ArrayDeque<Pair<Long, Float>>() // The window of acceleration values to be used for calculations of stage transitions.
    private var accelerationDataBufferWindowSum = 0.0 // The sum of the acceleration values in the dataBufferWindow. Used for calculating the average acceleration.
    private var accelerationDataBufferWindowSizeMillis = 0L // The size of the dataBufferWindow in milliseconds.
    private var isAccelerationDataBufferWindowFull = false // Whether the accelerationDataBufferWindow is full.
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var rotationSensor: Sensor
    private lateinit var gyroScope: Sensor
    private lateinit var gyroScopeData: FloatArray
    private lateinit var accelerometerData: FloatArray
    private lateinit var rotationSensorData: FloatArray
    private var isGyroScopeDataAvailable = false
    private var isAccelerometerDataAvailable = false
    private var isRotationSensorDataAvailable = false
    private var dataToSave = ArrayList<MasTestRaw>()
    private val sessionId = UUID.randomUUID().toString() // TODO: get from args
    private var currentStage = Stage.NONE
    private fun needToSaveData(): Boolean {
        return currentStage == Stage.INITIAL_2 || currentStage == Stage.MIDDLE || currentStage == Stage.FINAL
    }

    private fun needToGatherCompleteData(): Boolean {
        return currentStage == Stage.INITIAL_1 || needToSaveData()
    }

    private fun needToGatherPartialData(): Boolean {
        return currentStage == Stage.WARMUP
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mas_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        gyroScope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    override fun onResume() {
        super.onResume()
        if (currentStage == Stage.NONE) {
            currentStage = Stage.WARMUP
        }
        configureSensorsIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        configureSensorsIfNeeded()
    }

    private fun configureSensorsIfNeeded() {
        if (needToGatherCompleteData()) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(this, gyroScope, SensorManager.SENSOR_DELAY_FASTEST)
        } else if (needToGatherPartialData()) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    fun saveAllData() {
        Toast.makeText(requireContext(), "Saving ${dataToSave.size} data points", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(requireActivity().applicationContext)
            db.masTestRawDao().insertAll(dataToSave)
        }
    }

    fun navigateToNextStep() {
        //TODO: Perform filtering and calculate higher level features: rotation radius, PROM, magnitude of catch, etc.
        findNavController().navigate(R.id.action_masCollectionFragment_to_masResultFragment)
    }

    fun startCountdown() {
        // Create a CountdownTimer
        object : CountDownTimer(MAXIMUM_TEST_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // TODO: update UI
            }

            override fun onFinish() {
                // The hand has not stopped moving. The test is invalid.
                // TODO: Alert the user and restart the workflow
            }
        }.start()
    }

    private fun resetDataBuffer() {
        dataBuffer = ArrayList()
        accelerationDataBufferWindow = ArrayDeque()
        accelerationDataBufferWindowSum = 0.0
        isAccelerationDataBufferWindowFull = false
        accelerationDataBufferWindowSizeMillis = when (currentStage) {
            Stage.WARMUP -> HAND_STILL_TIME_WARMUP
            Stage.INITIAL_1 -> HAND_STILL_TIME_INITIAL
            Stage.INITIAL_2 -> HAND_MOVING_TIME_INITIAL
            Stage.MIDDLE -> HAND_MOVING_TIME_MIDDLE
            Stage.FINAL -> HAND_STILL_TIME_FINAL
            else -> 0L
        }
    }

    private fun updateBuffer(event: SensorEvent) : MasTestRaw? {
        val timestamp = event.timestamp
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                rotationSensorData = event.values
                isRotationSensorDataAvailable = true
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroScopeData = event.values
                isGyroScopeDataAvailable = true
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                accelerometerData = event.values
                isAccelerometerDataAvailable = true
                // We only need accelerometer data for the warmup stage
                // No need to set isAccelerometerDataAvailable to false because the same data will be used to create masTestRaw
                val accelerationMagnitude = sqrt(accelerometerData[0] * accelerometerData[0] + accelerometerData[1] * accelerometerData[1] + accelerometerData[2] * accelerometerData[2])
                accelerationDataBufferWindow.add(Pair(timestamp, accelerationMagnitude))
                accelerationDataBufferWindowSum += accelerationMagnitude
                while (accelerationDataBufferWindow.isNotEmpty() && accelerationDataBufferWindow.first.first + accelerationDataBufferWindowSizeMillis < accelerationDataBufferWindow.last.first) {
                    accelerationDataBufferWindow.removeFirst()
                    accelerationDataBufferWindowSum -= accelerationDataBufferWindow.first.second
                    isAccelerationDataBufferWindowFull = true
                }
            }
        }
        if (isGyroScopeDataAvailable && isAccelerometerDataAvailable && isRotationSensorDataAvailable) {
            isGyroScopeDataAvailable = false
            isAccelerometerDataAvailable = false
            isRotationSensorDataAvailable = false

            // Convert nanoseconds to seconds and remaining nanoseconds
            val seconds: Long = timestamp / 1000000000
            // Convert to LocalDateTime
            val instant =
                Instant.ofEpochSecond(seconds, (timestamp % 1000000000))
            val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

            val masTestRaw = MasTestRaw(
                sessionId = sessionId,
                localDateTime,
                accelerometerData[0],
                accelerometerData[1],
                accelerometerData[2],
                gyroScopeData[0],
                gyroScopeData[1],
                gyroScopeData[2],
                rotationSensorData[0],
                rotationSensorData[1],
                rotationSensorData[2],
                rotationSensorData[3],
                rotationSensorData[4]
            )
            dataBuffer.add(masTestRaw) // The data currently does not contain stage information
            return masTestRaw
        }
        return null
    }

    private fun getAverageAcceleration(): Double {
        assert(isAccelerationDataBufferWindowFull)
        return accelerationDataBufferWindowSum / accelerationDataBufferWindow.size
    }

    private fun copyBufferDataToDataToSave() {
        // For all data in dataBuffer,
        dataToSave.addAll(dataBuffer)
    }

    private fun moveToNextStage() {
        when (currentStage) {
            Stage.NONE -> {
                // Should not happen. Raise RuntimeException
                throw RuntimeException("Bug!")
            }
            Stage.WARMUP -> {
                currentStage = Stage.INITIAL_1
            }
            Stage.INITIAL_1 -> {
                currentStage = Stage.INITIAL_2
            }
            Stage.INITIAL_2 -> {
                currentStage = Stage.MIDDLE
            }
            Stage.MIDDLE -> {
                currentStage = Stage.FINAL
            }
            Stage.FINAL -> {
                currentStage = Stage.WRAPUP
            }
            Stage.WRAPUP -> {
                // Should not happen
                throw RuntimeException("Bug!")
            }
            Stage.INVALID -> {
                // Should not happen
                throw RuntimeException("Bug!")
            }
        }
        configureSensorsIfNeeded()

        resetDataBuffer()
    }

    private fun moveToInvalidStage(reason : String) {
        currentStage = Stage.INVALID
        configureSensorsIfNeeded()
        resetDataBuffer()
        // TODO: If there is a timer, cancel it
        // Alert Dialog
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Warning")
        builder.setMessage(reason)
        builder.setPositiveButton("OK") { _, _ ->
            // Restart current fragment
            val fragmentId = findNavController().currentDestination?.id
            findNavController().popBackStack(fragmentId!!,true)
            findNavController().navigate(fragmentId)
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun handleWarmupStage(SensorEvent: SensorEvent, masTestRaw: MasTestRaw?) {
        assert (currentStage == Stage.WARMUP)
        assert (masTestRaw == null) // In this stage, only accelerometer data is collected. masTestRaw should be null
        assert(SensorEvent.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION)
        if (isAccelerationDataBufferWindowFull) {
            if (getAverageAcceleration() < MIN_MOVEMENT_THRESHOLD) {
                moveToNextStage()
            }
        }
    }

    private fun handleInitial1Stage(SensorEvent: SensorEvent, masTestRaw: MasTestRaw?) {
        assert (currentStage == Stage.INITIAL_1)
        if (masTestRaw != null) {
            if (isAccelerationDataBufferWindowFull) {
                if (getAverageAcceleration() >= MIN_MOVEMENT_THRESHOLD) {
                    // The hand moves unexpectedly. Move to invalid stage
                    moveToInvalidStage("Please keep your hand still. Move the hand only when the app asks you to do so.")
                } else {
                    // The hand is still. Move to next stage
                    moveToNextStage()
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val masTestRaw = updateBuffer(it) // might be null
            when (currentStage) {
                Stage.NONE -> throw RuntimeException("Bug!")
                Stage.WARMUP -> handleWarmupStage(it, masTestRaw)
                Stage.INITIAL_1 -> handleInitial1Stage(it, masTestRaw)
                Stage.INITIAL_2 -> TODO()
                Stage.MIDDLE -> TODO()
                Stage.FINAL -> TODO()
                Stage.WRAPUP -> TODO()
                Stage.INVALID -> TODO()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Do nothing
    }
}