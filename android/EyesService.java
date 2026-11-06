package com.eyes.monitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.IBinder;

public class EyesService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Background loop for GPS, battery telemetry and command polling
        return START_STICKY;
    }
}
