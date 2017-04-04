package net.jzapper.android.compassar;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.widget.TextView;

import net.jzapper.android.compassar.utils.Utils;

public class CompassActivity extends AppCompatActivity implements SensorEventListener
{
    private static final float LOW_PASS_ALPHA = 0.9f;
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor magnetSensor;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] deviceRotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float compassAngle = 0.0f;

    private int screenRotation;

    private TextView compassText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        initViews();
        initSensors();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        updateScreenRotation();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        updateScreenRotation();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        int sensorType = sensorEvent.sensor.getType();
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(
                    sensorEvent.values, 0, accelerometerReading, 0, accelerometerReading.length);
        }
        else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(
                    sensorEvent.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }
        updateOrientationAngles();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {

    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        sensorManager.getRotationMatrix(
                rotationMatrix, null, accelerometerReading, magnetometerReading);

        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Z;

        // re-map with device axis for portrait orientation of device
        sensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, deviceRotationMatrix);

        // get orientaiton Euler angles
        sensorManager.getOrientation(deviceRotationMatrix, orientationAngles);

        // convert from radians to degrees
        for (int i = 0; i < 3; i++) {
            orientationAngles[i] = (float) Math.toDegrees(orientationAngles[i]);
        }

        switch (screenRotation) {
            case Surface.ROTATION_90: {
                orientationAngles[2] += 90.0f;
                break;
            }
            case Surface.ROTATION_180: {
                orientationAngles[2] += 180.0f;
                break;
            }
            case Surface.ROTATION_270: {
                orientationAngles[2] += 270.0f;
                break;
            }
        }

        compassAngle = Utils.lowPass(
                remapCompassToPositive(orientationAngles[0]), compassAngle, LOW_PASS_ALPHA);

        compassText.setText(Integer.toString(Math.round(compassAngle)));
    }

    private void initViews()
    {
        compassText = (TextView) findViewById(R.id.compass_value);
    }

    private void initSensors()
    {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void updateScreenRotation()
    {
        screenRotation = getWindowManager().getDefaultDisplay().getRotation();
    }

    private float remapCompassToPositive(float original) {
        if (original > 0) {
            return original;
        }
        return 360.0f + original;
    }
}
