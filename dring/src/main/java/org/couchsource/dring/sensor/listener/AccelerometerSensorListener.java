package org.couchsource.dring.sensor.listener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import org.couchsource.dring.application.AppContextWrapper;
import org.couchsource.dring.application.Constants;
import org.couchsource.dring.service.DeviceStateListener;

import static java.lang.Math.sqrt;

/**
 * author Kunal
 */
public class AccelerometerSensorListener extends DeviceSensorEventListener implements Constants {

    private static final float mAlpha = 0.25f;
    private static final double FLAT_SURFACE_THRESHOLD_FOR_FACE_UP = 0.95;
    private static final double FLAT_SURFACE_THRESHOLD_FOR_FACE_DOWN = 0.97;
    private static final int MAX_INTERVAL = 500;
    private final float[] mFilteredValues = new float[3];
    private long mLastUpdate;


    public AccelerometerSensorListener(AppContextWrapper contextWrapper, DeviceStateListener deviceStateListener) {
        super(contextWrapper,deviceStateListener);
    }


    public void register(){
        super.register(Sensor.TYPE_ACCELEROMETER);
    }

    private static boolean isFaceUp(float[] mFilteredValues) {
        double x = mFilteredValues[X];
        double y = mFilteredValues[Y];
        double z = mFilteredValues[Z];
        return (z / sqrt(x * x + y * y + z * z + 1.0e-6)) > FLAT_SURFACE_THRESHOLD_FOR_FACE_UP;
    }

    private static boolean isFaceDown(float[] mFilteredValues) {
        double x = mFilteredValues[X];
        double y = mFilteredValues[Y];
        double z = mFilteredValues[Z];
        if ((z / sqrt(x * x + y * y + z * z + 1.0e-6)) < -FLAT_SURFACE_THRESHOLD_FOR_FACE_DOWN) {
            return true;
        }
        return false;
    }

    private static float lowPass(float current, float gravity) {
        return Math.round(gravity * mAlpha + current * (1 - mAlpha));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            processSensorEvent(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void processSensorEvent(SensorEvent event) {
        float[] values = event.values;
        long mCurrentTime = System.currentTimeMillis();
        if (mCurrentTime - mLastUpdate > MAX_INTERVAL) {
            mLastUpdate = mCurrentTime;
            float rawX = values[X];
            float rawY = values[Y];
            float rawZ = values[Z];

            // Apply low-pass filter
            mFilteredValues[X] = lowPass(rawX, mFilteredValues[X]);
            mFilteredValues[Y] = lowPass(rawY, mFilteredValues[Y]);
            mFilteredValues[Z] = lowPass(rawZ, mFilteredValues[Z]);

            if (isFaceUp(mFilteredValues)) {
                getDeviceStateListener().registerFaceUp();
            } else if (isFaceDown(mFilteredValues)) {
                getDeviceStateListener().registerFaceDown();
            } else {
                getDeviceStateListener().registerNeitherFaceUpNorFaceDown();
            }
        }
    }
}
