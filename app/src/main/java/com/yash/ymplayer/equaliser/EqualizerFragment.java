package com.yash.ymplayer.equaliser;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.media.audiofx.PresetReverb;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.google.gson.Gson;
import com.yash.ymplayer.R;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.EqualizerUtil;

import java.util.ArrayList;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class EqualizerFragment extends Fragment {

    private static final String TAG = "EqualizerFragment";
    public static final String ARG_AUDIO_SESSION_ID = "audio_session_id";

    ImageView backBtn;
    TextView fragTitle;
    SwitchCompat equalizerSwitch;

    LineSet dataset;
    LineChartView chart;
    Paint paint;
    float[] points;

    int y = 0;
    int z = 0;

    ImageView spinnerDropDownIcon;

    short numberOfFrequencyBands;
    LinearLayout mLinearLayout;

    SeekBar[] seekBarFinal = new SeekBar[5];

    AnalogController bassController, reverbController, loudnessController;

    Spinner presetSpinner;

    FrameLayout equalizerBlocker;


    Context ctx;

    public EqualizerFragment() {
        // Required empty public constructor
    }

    public Equalizer mEqualizer;
    public BassBoost bassBoost;
    public PresetReverb presetReverb;
    public LoudnessEnhancer loudnessEnhancer;

    static int themeColor = Color.parseColor("#B24242");
    static boolean showBackButton = true;

    public static EqualizerFragment newInstance(int audioSessionId) {

        Bundle args = new Bundle();
        args.putInt(ARG_AUDIO_SESSION_ID, audioSessionId);

        EqualizerFragment fragment = new EqualizerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.isEditing = true;

        mEqualizer = EqualizerUtil.getInstance(getContext()).getEqualizer().getValue();
        bassBoost = EqualizerUtil.getInstance(getContext()).getBassBoost().getValue();
        presetReverb = EqualizerUtil.getInstance(getContext()).getPresetReverb().getValue();
        loudnessEnhancer = EqualizerUtil.getInstance(getContext()).getLoudnessEnhancer().getValue();

        EqualizerUtil.getInstance(getContext()).getEqualizer().observe(requireActivity(), equalizer -> mEqualizer = equalizer);
        EqualizerUtil.getInstance(getContext()).getBassBoost().observe(requireActivity(), bassBoost -> this.bassBoost = bassBoost);
        EqualizerUtil.getInstance(getContext()).getPresetReverb().observe(requireActivity(), presetReverb -> this.presetReverb = presetReverb);
        EqualizerUtil.getInstance(getContext()).getLoudnessEnhancer().observe(requireActivity(), loudnessEnhancer -> this.loudnessEnhancer = loudnessEnhancer);

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ctx = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_equalizer, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        backBtn = view.findViewById(R.id.equalizer_back_btn);
        backBtn.setVisibility(showBackButton ? View.VISIBLE : View.GONE);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        fragTitle = view.findViewById(R.id.equalizer_fragment_title);


        equalizerSwitch = view.findViewById(R.id.equalizer_switch);
        equalizerSwitch.setChecked(Settings.isEqualizerEnabled);
        equalizerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EqualizerUtil.getInstance(buttonView.getContext()).setEqualizerEnabled(isChecked);
            }
        });

        spinnerDropDownIcon = view.findViewById(R.id.spinner_dropdown_icon);
        spinnerDropDownIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presetSpinner.performClick();
            }
        });

        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner);

        equalizerBlocker = view.findViewById(R.id.equalizerBlocker);


        chart = view.findViewById(R.id.lineChart);
        paint = new Paint();
        dataset = new LineSet();

        bassController = view.findViewById(R.id.controllerBass);
        reverbController = view.findViewById(R.id.controller3D);
        loudnessController = view.findViewById(R.id.controllerLoudness);

        bassController.setLabel("BASS");
        reverbController.setLabel("3D");
        loudnessController.setLabel("LOUDNESS");

        bassController.circlePaint2.setColor(themeColor);
        bassController.linePaint.setColor(themeColor);
        bassController.invalidate();
        reverbController.circlePaint2.setColor(themeColor);
        reverbController.linePaint.setColor(themeColor);
        reverbController.invalidate();
        loudnessController.circlePaint2.setColor(themeColor);
        loudnessController.linePaint.setColor(themeColor);
        loudnessController.invalidate();


        if (!Settings.isEqualizerReloaded) {
            int x = 0;
            if (bassBoost != null) {
                try {
                    x = ((bassBoost.getRoundedStrength() * 19) / 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (presetReverb != null) {
                try {
                    y = (presetReverb.getPreset() * 19) / 6;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (loudnessEnhancer != null) {
                try {
                    z = (int) ((Settings.loudnessGain * 18) / Settings.TargetLoudnessGain) + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }

            if (z == 0) {
                loudnessController.setProgress(1);
            } else {
                loudnessController.setProgress(z);
            }
        } else {
            int x = ((Settings.bassStrength * 19) / 1000);
            y = (Settings.reverbPreset * 19) / 6;
            z = Math.round((Settings.loudnessGain * 18.0f) / Settings.TargetLoudnessGain) + 1;
            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }

            if (z == 0) {
                loudnessController.setProgress(1);
            } else {
                loudnessController.setProgress(z);
            }
        }

        bassController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.bassStrength = (short) (((float) 1000 / 19) * (progress));
                try {
                    bassBoost.setStrength(Settings.bassStrength);
                    Settings.equalizerModel.setBassStrength(Settings.bassStrength);
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        reverbController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.reverbPreset = (short) ((progress * 6) / 19);
                Settings.equalizerModel.setReverbPreset(Settings.reverbPreset);
                try {
                    presetReverb.setPreset(Settings.reverbPreset);
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                y = progress;
            }
        });

        loudnessController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.loudnessGain = ((progress - 1) * Settings.TargetLoudnessGain) / 18;
                Settings.equalizerModel.setLoudnessGain(Settings.loudnessGain);
                try {
                    loudnessEnhancer.setTargetGain(Settings.loudnessGain);
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                z = progress;
            }
        });

        mLinearLayout = view.findViewById(R.id.equalizerContainer);

        TextView equalizerHeading = new TextView(getContext());
        equalizerHeading.setText(R.string.eq);
        equalizerHeading.setTextSize(20);
        equalizerHeading.setGravity(Gravity.CENTER_HORIZONTAL);

        numberOfFrequencyBands = 5;

        points = new float[numberOfFrequencyBands];

        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
        final short upperEqualizerBandLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < numberOfFrequencyBands; i++) {
            final short equalizerBandIndex = i;
            final TextView frequencyHeaderTextView = new TextView(getContext());
            frequencyHeaderTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            frequencyHeaderTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frequencyHeaderTextView.setTextColor(Color.parseColor("#FFFFFF"));
            frequencyHeaderTextView.setText((mEqualizer.getCenterFreq(equalizerBandIndex) / 1000) + "Hz");

            LinearLayout seekBarRowLayout = new LinearLayout(getContext());
            seekBarRowLayout.setOrientation(LinearLayout.VERTICAL);

            TextView lowerEqualizerBandLevelTextView = new TextView(getContext());
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            lowerEqualizerBandLevelTextView.setTextColor(Color.parseColor("#FFFFFF"));
            lowerEqualizerBandLevelTextView.setText((lowerEqualizerBandLevel / 100) + "dB");

            TextView upperEqualizerBandLevelTextView = new TextView(getContext());
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            upperEqualizerBandLevelTextView.setTextColor(Color.parseColor("#FFFFFF"));
            upperEqualizerBandLevelTextView.setText((upperEqualizerBandLevel / 100) + "dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.weight = 1;

            SeekBar seekBar = new SeekBar(getContext());
            TextView textView = new TextView(getContext());
            switch (i) {
                case 0:
                    seekBar = view.findViewById(R.id.seekBar1);
                    textView = view.findViewById(R.id.textView1);
                    break;
                case 1:
                    seekBar = view.findViewById(R.id.seekBar2);
                    textView = view.findViewById(R.id.textView2);
                    break;
                case 2:
                    seekBar = view.findViewById(R.id.seekBar3);
                    textView = view.findViewById(R.id.textView3);
                    break;
                case 3:
                    seekBar = view.findViewById(R.id.seekBar4);
                    textView = view.findViewById(R.id.textView4);
                    break;
                case 4:
                    seekBar = view.findViewById(R.id.seekBar5);
                    textView = view.findViewById(R.id.textView5);
                    break;
            }
            seekBarFinal[i] = seekBar;
            seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN));
            seekBar.getThumb().setColorFilter(new PorterDuffColorFilter(themeColor, PorterDuff.Mode.SRC_IN));
            seekBar.setId(i);
//            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(upperEqualizerBandLevel - lowerEqualizerBandLevel);

            textView.setText(frequencyHeaderTextView.getText());
            textView.setTextColor(Color.WHITE);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            if (Settings.isEqualizerReloaded) {
                points[i] = Settings.seekbarpos[i] - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(Settings.seekbarpos[i] - lowerEqualizerBandLevel);
            } else {
                points[i] = mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel);
                Settings.seekbarpos[i] = mEqualizer.getBandLevel(equalizerBandIndex);
                Settings.isEqualizerReloaded = true;
            }

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mEqualizer.setBandLevel(equalizerBandIndex, (short) (progress + lowerEqualizerBandLevel));
                        Settings.seekbarpos[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                        Settings.equalizerModel.getSeekbarpos()[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                    }
                    points[seekBar.getId()] = progress;
                    dataset.updateValues(points);
                    chart.notifyDataUpdate();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    presetSpinner.setSelection(0);
                    Settings.presetPos = 0;
                    Settings.equalizerModel.setPresetPos(0);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        equalizeSound();

        paint.setColor(Color.parseColor("#555555"));
        paint.setStrokeWidth((float) (1.10 * Settings.ratio));

        dataset.setColor(themeColor);
        dataset.setSmooth(true);
        dataset.setThickness(5);

        chart.setXAxis(false);
        chart.setYAxis(false);

        chart.setYLabels(AxisController.LabelPosition.NONE);
        chart.setXLabels(AxisController.LabelPosition.NONE);
        chart.setGrid(ChartView.GridType.NONE, 7, 10, paint);

        chart.setAxisBorderValues(-300, 3300);

        chart.addData(dataset);
        chart.show();

        Button mEndButton = new Button(getContext());
        mEndButton.setBackgroundColor(themeColor);
        mEndButton.setTextColor(Color.WHITE);


    }

    public void equalizeSound() {
        ArrayList<String> equalizerPresetNames = new ArrayList<>();
        ArrayAdapter<String> equalizerPresetSpinnerAdapter = new ArrayAdapter<>(ctx,
                R.layout.spinner_item,
                equalizerPresetNames);
        equalizerPresetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        equalizerPresetNames.add("Custom");

        for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
            equalizerPresetNames.add(mEqualizer.getPresetName(i));
        }

        presetSpinner.setAdapter(equalizerPresetSpinnerAdapter);
        //presetSpinner.setDropDownWidth((Settings.screen_width * 3) / 4);
        if (Settings.isEqualizerReloaded && Settings.presetPos != 0) {
//            correctPosition = false;
            presetSpinner.setSelection(Settings.presetPos);
        }

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position != 0) {
                        mEqualizer.usePreset((short) (position - 1));
                        Settings.presetPos = position;
                        short numberOfFreqBands = 5;

                        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];

                        for (short i = 0; i < numberOfFreqBands; i++) {
                            ObjectAnimator.ofInt(seekBarFinal[i], "progress", (mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel)).setDuration(400).start();
                            //seekBarFinal[i].setProgress(mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel);
                            //points[i] = mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel;
                            Settings.seekbarpos[i] = mEqualizer.getBandLevel(i);
                            Settings.equalizerModel.getSeekbarpos()[i] = mEqualizer.getBandLevel(i);
                        }
                        dataset.updateValues(points);
                        chart.notifyDataUpdate();
                    }
                } catch (Exception e) {
                    Toast.makeText(ctx, "Error while updating Equalizer", Toast.LENGTH_SHORT).show();
                }
                Settings.equalizerModel.setPresetPos(position);
                EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Settings.isEditing = false;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int id = -1;

        public Builder setAudioSessionId(int id) {
            this.id = id;
            return this;
        }

        public Builder setAccentColor(int color) {
            themeColor = color;
            return this;
        }

        public Builder setShowBackButton(boolean show) {
            showBackButton = show;
            return this;
        }

        public EqualizerFragment build() {
            return EqualizerFragment.newInstance(id);
        }
    }


}