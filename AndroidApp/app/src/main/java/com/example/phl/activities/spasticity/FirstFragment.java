package com.example.phl.activities.spasticity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.phl.R;
import com.example.phl.activities.SpasticityDiagnosisActivity;
import com.example.phl.data.spasticity.data_collection.RawDataset;
import com.example.phl.data.spasticity.data_collection.SensorData;
import com.example.phl.databinding.FragmentFirstBinding;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment implements SensorEventListener {

    private FragmentFirstBinding binding;
    private Context mContext;
    GraphView graphView;
    GraphView graphView2;
    GraphView graphView3;
    Toast toast;
    List<Float> pressures = new ArrayList<>();
    List<Float> gyroscopeX = new ArrayList<>();
    List<Float> gyroscopeY = new ArrayList<>();
    List<Float> gyroscopeZ = new ArrayList<>();
    List<Float> gyroscopeMagnitude = new ArrayList<>();
    List<Float> accelerometerX = new ArrayList<>();
    List<Float> accelerometerY = new ArrayList<>();
    List<Float> accelerometerZ = new ArrayList<>();
    List<Float> accelerometerMagnitude = new ArrayList<>();

    boolean isVibrating = false;
    LineGraphSeries<DataPoint> screenSensorDataSeries;
    LineGraphSeries<DataPoint> gyroscopeXDataSeries;
    LineGraphSeries<DataPoint> gyroscopeYDataSeries;
    LineGraphSeries<DataPoint> gyroscopeZDataSeries;
    LineGraphSeries<DataPoint> gyroscopeMagnitudeDataSeries;
    LineGraphSeries<DataPoint> accelerometerXDataSeries;
    LineGraphSeries<DataPoint> accelerometerYDataSeries;
    LineGraphSeries<DataPoint> accelerometerZDataSeries;
    LineGraphSeries<DataPoint> accelerometerMagnitudeDataSeries;


    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;

//    boolean showGyroscope = false;


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        binding.buttonFirst
                .setText("Switch to Vibration-based Tests");
        graphView =  binding.graph;
        graphView2 = binding.graph2;
        graphView3 = binding.graph3;

        screenSensorDataSeries = new LineGraphSeries<>();
        gyroscopeXDataSeries = new LineGraphSeries<>();
        gyroscopeYDataSeries = new LineGraphSeries<>();
        gyroscopeZDataSeries = new LineGraphSeries<>();
        gyroscopeMagnitudeDataSeries = new LineGraphSeries<>();
        accelerometerXDataSeries = new LineGraphSeries<>();
        accelerometerYDataSeries = new LineGraphSeries<>();
        accelerometerZDataSeries = new LineGraphSeries<>();
        accelerometerMagnitudeDataSeries = new LineGraphSeries<>();
        gyroscopeXDataSeries.setColor(Color.RED);
        gyroscopeYDataSeries.setColor(Color.GREEN);
        gyroscopeZDataSeries.setColor(Color.BLUE);
        accelerometerXDataSeries.setColor(Color.RED);
        accelerometerYDataSeries.setColor(Color.GREEN);
        accelerometerZDataSeries.setColor(Color.BLUE);


        graphView.addSeries(screenSensorDataSeries);

        graphView.getViewport().setXAxisBoundsManual(true);
//        graphView.getViewport().setMinX(0);
//        graphView.getViewport().setMaxX(100);

        // set manual X bounds
        graphView2.getViewport().setXAxisBoundsManual(true);
//        graphView2.addSeries(gyroscopeXDataSeries);
//        graphView2.addSeries(gyroscopeYDataSeries);
//        graphView2.addSeries(gyroscopeZDataSeries);
        graphView2.addSeries(gyroscopeMagnitudeDataSeries);

        graphView3.getViewport().setXAxisBoundsManual(true);
//        graphView3.addSeries(accelerometerXDataSeries);
//        graphView3.addSeries(accelerometerYDataSeries);
        graphView3.addSeries(accelerometerZDataSeries);
//        graphView3.addSeries(accelerometerMagnitudeDataSeries);

        updateLayouts();

        return binding.getRoot();

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.getRoot().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isVibrating) {
                    return true;
                }
                int numFingers = motionEvent.getPointerCount();
                int historySize = motionEvent.getHistorySize();
                float pressure = 0;
                for (int i = 0; i < numFingers; i++) {
                    float currentFingerPressureSum = Math.min(motionEvent.getPressure(i), 2.0f);
                    for (int  j = 0; j < historySize; j++) {
                        currentFingerPressureSum += Math.min(motionEvent.getHistoricalPressure(i, j), 2.0f);
                    }
                    pressure += currentFingerPressureSum / (historySize + 1);
                }
                pressure /= 4; // We have 4 fingers on the screen.
//                float area = motionEvent.getSize();

                Log.i("MotionEvent", String.valueOf(motionEvent.getPointerCount()));
                pressures.add(pressure);
                screenSensorDataSeries.appendData(new DataPoint(motionEvent.getEventTime()/1000.0, pressure), false, 10000);
                graphView.getViewport().setMaxX(motionEvent.getEventTime()/1000.0 + 0.5);
                graphView.getViewport().setMinX(motionEvent.getEventTime()/1000.0 - 5);
                return true;
            }
        });

        binding.buttonWorkflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SpasticityDiagnosisActivity) requireActivity()).setOnLegacyWorkflow(false);
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_calibrationInstructionFragment);
        }});

        binding.buttonWorkflowLegacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SpasticityDiagnosisActivity) requireActivity()).setOnLegacyWorkflow(true);
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_calibrationInstructionFragment);
            }});

        binding.buttonWorkflowTouchscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((SpasticityDiagnosisActivity) requireActivity()).setOnLegacyWorkflow(false);
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_touchscreenTestInstructionFragment);
            }});

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                if (FirstFragment.this.isVibrating) {
                    ((SpasticityDiagnosisActivity) requireActivity()).stopVibration();
                    FirstFragment.this.isVibrating = false;
                    updateLayouts();
                    // Unregister the gyroscope sensor listener
                    sensorManager.unregisterListener(FirstFragment.this);
//                    FileWriter.writeToCSV("data.csv", Arrays.asList("Gyroscope X", "Gyroscope Y", "Gyroscope Z", "Accelerometer X", "Accelerometer Y", "Accelerometer Z"), Arrays.asList(gyroscopeX, gyroscopeY, gyroscopeZ, accelerometerX, accelerometerY, accelerometerZ));
//                    Toast.makeText(mContext, "Data saved to data.csv", Toast.LENGTH_SHORT).show();
                } else {
                    ((SpasticityDiagnosisActivity) requireActivity()).startVibration();
                    FirstFragment.this.isVibrating = true;
                    updateLayouts();
                    // Register the gyroscope sensor listener
                    sensorManager.registerListener(FirstFragment.this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(FirstFragment.this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);

                    DataPoint[] empty = {};
                    gyroscopeXDataSeries.resetData(empty);
                    gyroscopeYDataSeries.resetData(empty);
                    gyroscopeZDataSeries.resetData(empty);
                    gyroscopeMagnitudeDataSeries.resetData(empty);

                    accelerometerXDataSeries.resetData(empty);
                    accelerometerYDataSeries.resetData(empty);
                    accelerometerZDataSeries.resetData(empty);
                    accelerometerMagnitudeDataSeries.resetData(empty);
                    binding.vibrationLayout.setVisibility(View.VISIBLE);
                    binding.vibrationLayout3.setVisibility(View.VISIBLE);
                    binding.touchLayout.setVisibility(View.GONE);
                    binding.touchLayout2.setVisibility(View.GONE);
                    binding.touchLayout3.setVisibility(View.GONE);
                }
            }
        });

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        // Get an instance of the gyroscope sensor
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        ((SpasticityDiagnosisActivity) requireActivity()).stopVibration();
        FirstFragment.this.isVibrating = false;
        sensorManager.unregisterListener(FirstFragment.this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Check if the sensor event is from the gyroscope sensor
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Get the gyroscope data
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            // Do something with the gyroscope data
            Log.d("Gyroscope", "x: " + x + ", y: " + y + ", z: " + z);
            gyroscopeX.add(x);
            gyroscopeY.add(y);
            gyroscopeZ.add(z);
            gyroscopeMagnitude.add((float) Math.sqrt(x * x + y * y + z * z));
            gyroscopeXDataSeries.appendData(new DataPoint(gyroscopeX.size(), x), false, 500);
            gyroscopeYDataSeries.appendData(new DataPoint(gyroscopeY.size(), y), false, 500);
            gyroscopeZDataSeries.appendData(new DataPoint(gyroscopeZ.size(), z), false, 500);
            gyroscopeMagnitudeDataSeries.appendData(new DataPoint(gyroscopeMagnitude.size(), gyroscopeMagnitude.get(gyroscopeMagnitude.size() - 1)), false, 500);
            graphView2.getViewport().setMinX(Math.max(0, gyroscopeX.size() - 500));
            graphView2.getViewport().setMaxX(gyroscopeX.size());
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Get the accelerometer data
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            // Do something with the gyroscope data
            Log.d("Accelerometer", "x: " + x + ", y: " + y + ", z: " + z);
            accelerometerX.add(x);
            accelerometerY.add(y);
            accelerometerZ.add(z);
            accelerometerMagnitude.add((float) Math.sqrt(x * x + y * y + z * z));
            accelerometerXDataSeries.appendData(new DataPoint(accelerometerX.size(), x), false, 500);
            accelerometerYDataSeries.appendData(new DataPoint(accelerometerY.size(), y), false, 500);
            accelerometerZDataSeries.appendData(new DataPoint(accelerometerZ.size(), z), false, 500);
            accelerometerMagnitudeDataSeries.appendData(new DataPoint(accelerometerMagnitude.size(), accelerometerMagnitude.get(accelerometerMagnitude.size() - 1)), false, 500);
            graphView3.getViewport().setMinX(Math.max(0, accelerometerX.size() - 500));
            graphView3.getViewport().setMaxX(accelerometerX.size());
        }
    }

    public void updateLayouts() {
        if (binding!=null) {
            if (isVibrating) {
                binding.buttonFirst.setText("Switch to Touchscreen-based Tests");
                binding.vibrationLayout.setVisibility(View.VISIBLE);
                binding.vibrationLayout3.setVisibility(View.VISIBLE);
                binding.touchLayout.setVisibility(View.GONE);
                binding.touchLayout2.setVisibility(View.GONE);
                binding.touchLayout3.setVisibility(View.GONE);
            } else {
                binding.buttonFirst.setText("Switch to Vibration-based Tests");
                binding.vibrationLayout.setVisibility(View.GONE);
                binding.vibrationLayout3.setVisibility(View.GONE);
                binding.touchLayout.setVisibility(View.VISIBLE);
                binding.touchLayout2.setVisibility(View.VISIBLE);
                binding.touchLayout3.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        RawDataset.reset(SensorData.includedSensors.length);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}