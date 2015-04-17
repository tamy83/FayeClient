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
import android.os.IBinder;
import android.os.Binder;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import com.monmouth.fayePG.FayePG;
import android.util.LogPrinter;

public class FayeService extends IntentService implements FayeListener {

    private static final String LOG_TAG = "Faye";

    private Handler mHandler;
    private FayeClient mClient;
    private FayePG fayePG;
    private String command;


    public FayeService() {
        super("FayeService");
    }

    public class FayeBinder extends Binder {
        FayeService getService() {
            return FayeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new FayeBinder();


    @Override
    protected void onHandleIntent(Intent intent) {

        // SSL bug in pre-Gingerbread devices makes websockets currently unusable
        if (android.os.Build.VERSION.SDK_INT <= 8) return;

        Log.i(LOG_TAG, "Starting Web Socket");

        try {
            String address = intent.getStringExtra("address");
            String channel = intent.getStringExtra("channel");
            command = intent.getStringExtra("command");

            URI uri = URI.create(address);

            String user = intent.getStringExtra("user");
            String sid = intent.getStringExtra("sid");

            JSONObject ext = new JSONObject();
            ext.put("user", user);
            ext.put("sid", sid);

            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
            }
            mClient = new FayeClient(mHandler, uri, channel);

            JSONObject keepAliveMsg = new JSONObject();
            keepAliveMsg.put("user", user);

            mClient.setKeepAliveChannel("/keepAlive");
            mClient.setKeepAliveMessage(keepAliveMsg);

            //mClient.setFayeListener(fayePG);
            mClient.setFayeListener(this);
            mClient.connectToServer(ext);


        } catch (JSONException ex) {}
        Log.i(LOG_TAG, "onHandleIntent returns");

    }

    public void disconnect() {
        Log.i(LOG_TAG, "FayeService disconnect");
        if (mClient != null) {
            mClient.setShouldRetryConnection(false);
            mClient.disconnectFromServer();
        }
    }

    public void subscribe() {
        if (mClient != null)
            mClient.subscribe();
    }

    public void sendMessage(String channel, JSONObject data) {
        if (mClient != null)
            mClient.sendMessage(channel, data);
    }

    void setFayeListener(FayeListener fayeListener) {
        if (fayeListener instanceof FayePG) {
            Log.i(LOG_TAG, "FayeService setfayelistener to fayePG");
            fayePG = (FayePG) fayeListener;
        }
    }

    void setFaye(FayePG fayePG) {
        this.fayePG = fayePG;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy() fayeService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(LOG_TAG, "onStart() fayeService");
    }

    @Override // returned value here affects how android restarts this service
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void connectedToServer() {
        Log.i(LOG_TAG, "Connected to Server");
    }

    @Override
    public void disconnectedFromServer() {
        Log.i(LOG_TAG, "Disconnected from Server");
    }

    @Override
    public void subscribedToChannel(String subscription) {
        Log.i(LOG_TAG, String.format("Subscribed to channel %s.", subscription));
    }

    @Override
    public void subscriptionFailedWithError(String error) {
        Log.e(LOG_TAG, String.format("Subscription failed with error: %s", error));
    }

    @Override
    public void messageReceived(final JSONObject json) {
        Log.i(LOG_TAG, String.format("Received message in fayeService %s", json.toString()));
        // call javascript command and pass json
        Runnable r = new Runnable() {
            @Override
            public void run() {

                fayePG.webView.loadUrl("javascript:" + command + "(" + json.toString() + ");");
            }
        };
        mHandler.post(r);

    }

    @Override
    public void onConnectionChanged(boolean connection) {
        if (fayePG != null) {
            if (connection)
                fayePG.displayNotification();
            else
                fayePG.removeNotification();
        }
    }

}