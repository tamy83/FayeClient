package com.monmouth.fayePG;

import com.saulpower.fayeclient.FayeClient;
import com.saulpower.fayeclient.FayeClient.FayeListener;
import android.content.Intent;
import android.util.Log;
import android.app.Service;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Looper;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import com.monmouth.fayePG.FayePG;
import android.util.LogPrinter;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.content.Context;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaActivity;

//public class FayeService extends IntentService implements FayeListener {
public class FayeService extends Service implements FayeListener {

    private static final String LOG_TAG = "Faye";

    private Handler mHandler;
    private FayeClient mClient;
    private FayePG fayePG;
    private String command;

    private boolean startedForeground = false;
    private Notification notif;
    private int notif_id;

    public Notification getNotif() {
        return notif;
    }

    public void setNotif(int id, Notification notif) {
        this.notif = notif;
        notif_id = id;
        if (!startedForeground) {
            startForeground(id, notif);
            startedForeground = true;
        } else {
            if (fayePG != null) {
                NotificationManager mNotificationManager =
                        (NotificationManager) fayePG.cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(id, notif);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // SSL bug in pre-Gingerbread devices makes websockets currently unusable
        if (android.os.Build.VERSION.SDK_INT <= 8) return START_NOT_STICKY;

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
                mHandler = new Handler(Looper.getMainLooper()); // still references app looper if service restarts?
            }
            mClient = new FayeClient(mHandler, uri, channel);

            JSONObject keepAliveMsg = new JSONObject();
            keepAliveMsg.put("user", user);

            mClient.setKeepAliveChannel("/keepAlive");
            mClient.setKeepAliveMessage(keepAliveMsg);

            //mClient.setFayeListener(fayePG);
            mClient.setFayeListener(this);
            mClient.connectToServer(ext);
            if (notif != null)
                startForeground(notif_id,notif);


        } catch (JSONException ex) {
            Log.e(LOG_TAG, "JSONException: " + ex.getMessage());
        }

        // no need to redeliver intent if restarted services can't process msg if activity/webviews are gone?
        return START_REDELIVER_INTENT;
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


    public void disconnect() {
        Log.i(LOG_TAG, "FayeService disconnect");
        if (mClient != null) {
            mClient.setShouldRetryConnection(false);
            mClient.disconnectFromServer();
        }
        stopForeground(true);
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
        setCtxAndClassName();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy() fayeService");
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
                if (fayePG.isDestroyed()) {
                    Log.i(LOG_TAG, "Activity destroyed");
                    //fayePG.webView = new CordovaWebView(ctx);
                    /*CordovaInterface cordova, CordovaWebViewClient webViewClient, CordovaChromeClient webChromeClient,
                            List<PluginEntry> pluginEntries, Whitelist internalWhitelist, Whitelist externalWhitelist,
                            CordovaPreferences preferences) {
                            */
                    //fayePG.webView.init(fayePG.cordova, fayePG.cordova.getActivity().webViewClient);
                    //fayePG.getActivity() =
                    //CordovaActivity act = new CordovaActivity();
                    //act.init();
                    //CordovaWebView newWebView = new CordovaWebView(ctx);
                    //fayePG.setDestroyed(false);
                    //act.loadUrl("javascript:" + command + "(" + json.toString() + ");");
                    //fayePG.webView.loadUrl("javascript:" + command + "(" + json.toString() + ");");

                    Intent notifIntent = new Intent(ctx,className);
                    notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    ctx.startActivity(notifIntent);

                    Runnable r1 = new Runnable() {
                        @Override
                        public void run() {
                            Log.i(LOG_TAG, "executing r1");
                            fayePG.webView.loadUrl("javascript:" + command + "(" + json.toString() + ");");
                        }
                    };
                    mHandler.postDelayed(r1, 10000);

                } else {
                    fayePG.webView.loadUrl("javascript:" + command + "(" + json.toString() + ");");
                }

            }
        };
        mHandler.post(r);
    }


    private Context ctx;
    private Class<?> className;

    private void setCtxAndClassName() {
        ctx = fayePG.cordova.getActivity().getApplicationContext();
        try {
            className =  Class.forName(fayePG.cordova.getActivity().getComponentName().getClassName());
            Log.i(LOG_TAG, className.toString());
        } catch (ClassNotFoundException ex) {
            Log.e(LOG_TAG, "Error: Can't find class of cordova activity!");
        }
    }


}