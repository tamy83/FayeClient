package com.monmouth.fayePG;

import com.saulpower.fayeclient.FayeClient;
import com.saulpower.fayeclient.FayeClient.FayeListener;
import android.content.Intent;
import android.util.Log;
import android.app.IntentService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import android.os.Looper;
import android.os.Handler;

public class FayeService extends IntentService implements FayeListener {

    private static final String LOG_TAG = "FayeService";

    FayeClient mClient;

    public FayeService() {
        super("FayeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // SSL bug in pre-Gingerbread devices makes websockets currently unusable
        if (android.os.Build.VERSION.SDK_INT <= 8) return;

        Log.i(LOG_TAG, "Starting Web Socket");

        try {
            String address = intent.getStringExtra("address");
            String channel = intent.getStringExtra("channel");

            URI uri = URI.create(address);

            JSONObject ext = new JSONObject();
            // no need to implement authentication
            ext.put("authToken", "");

            mClient = new FayeClient(new Handler(Looper.getMainLooper()), uri, channel);
            mClient.setFayeListener(this);
            mClient.connectToServer(ext);

        } catch (JSONException ex) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void connectedToServer() {
        Log.i(LOG_TAG, "Connected to Server");
    }

    @Override
    public void disconnectedFromServer() {
        Log.i(LOG_TAG, "Disonnected to Server");
    }

    @Override
    public void subscribedToChannel(String subscription) {
        Log.i(LOG_TAG, String.format("Subscribed to channel %s on Faye", subscription));
    }

    @Override
    public void subscriptionFailedWithError(String error) {
        Log.i(LOG_TAG, String.format("Subscription failed with error: %s", error));
    }

    @Override
    public void messageReceived(JSONObject json) {
        Log.i(LOG_TAG, String.format("Received message %s", json.toString()));
    }
}