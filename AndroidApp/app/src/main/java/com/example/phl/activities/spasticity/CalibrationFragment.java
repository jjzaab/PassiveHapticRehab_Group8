package com.example.phl.activities.spasticity;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.phl.R;
import com.example.phl.activities.SpasticityDiagnosisActivity;
import com.example.phl.data.spasticity.data_collection.RawDataset;
import com.example.phl.data.spasticity.data_collection.SensorData;
import com.example.phl.databinding.FragmentCalibrationBinding;
import com.example.phl.utils.UnitConverter;

/**
 * A simple {@link Fragment} subclass.
 */
public class CalibrationFragment extends Fragment {
    private  Context mContext;
    private FragmentCalibrationBinding binding;

    CountDownTimer countDownTimer1;
    CountDownTimer countDownTimer2;
    CountDownTimer countDownTimer3;

    SensorData sensorData;

    double weight;

    boolean isGrams = true;

    private boolean isOnLegacyWorkflow;


    public CalibrationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        countDownTimer1.cancel();
        countDownTimer2.cancel();
        countDownTimer3.cancel();
        ((SpasticityDiagnosisActivity) requireActivity()).stopVibration();
        if (sensorData != null) {
            sensorData.stopCollectingData();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("weight")) {
            weight = getArguments().getDouble("weight");
        } else {
            throw new IllegalArgumentException("Must pass weight");
        }
        this.isOnLegacyWorkflow = ((SpasticityDiagnosisActivity) requireActivity()).isOnLegacyWorkflow();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalibrationBinding.inflate(inflater, container, false);
        binding.continueButton.setVisibility(View.INVISIBLE);
        binding.textviewCountingDown.setText(R.string.counting_down);
        binding.continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CalibrationFragment.this.isOnLegacyWorkflow) {
                    NavHostFragment.findNavController(CalibrationFragment.this)
                            .navigate(R.id.action_CalibrationFragment_to_calibrationObjectListFragment);
                } else {
                    NavHostFragment.findNavController(CalibrationFragment.this)
                            .navigate(R.id.nav1);
                }
            }
        });

        countDownTimer1 = new CountDownTimer(3000, 100) {
            public void onTick(long millisUntilFinished) {
                binding.textviewTimer.setText(
                        String.valueOf(millisUntilFinished / 1000)
                );
            }
            public void onFinish() {
                ((SpasticityDiagnosisActivity) requireActivity()).startVibration();
                binding.textviewTimer.setText("");
                countDownTimer2.start();
            }
        };

        countDownTimer2 = new CountDownTimer(1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                sensorData.startCollectingData();
                countDownTimer3.start();
            }
        };

        countDownTimer3 = new CountDownTimer(5000, 100) {
            public void onTick(long millisUntilFinished) {
                binding.textviewTimer.setText(
                        String.valueOf(millisUntilFinished / 1000)
                );
            }

            public void onFinish() {
                sensorData.stopCollectingData();
                RawDataset.getInstance().add(sensorData.getDatapoint(), isGrams ? UnitConverter.gramsToNewtons(weight) : UnitConverter.ouncesToNewtons(weight));
                ((SpasticityDiagnosisActivity) requireActivity()).stopVibration();
                binding.textviewCountingDown.setText(R.string.continue_to_next);
                binding.continueButton.setVisibility(View.VISIBLE);
                binding.textviewTimer.setText("");
            }
        };
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sensorData = new SensorData((SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE));
        binding.ripple.startRippleAnimation();
        countDownTimer1.start();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}