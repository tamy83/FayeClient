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
import android.os.Environment;
import java.io.IOException;
import android.os.Looper;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.app.PendingIntent;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import com.monmouth.fayePG.FayePG;
import android.util.LogPrinter;
import android.os.HandlerThread;
import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaActivity;
import com.monmouth.monmouthtelecom.MobileCarrier;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.net.Uri;
import java.util.ArrayList;
import android.content.ContentProviderOperation;
import java.util.List;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.os.RemoteException;
import android.content.OperationApplicationException;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import com.monmouth.monmouthtelecom.MTTMsgExecuter;


import android.telephony.TelephonyManager;
import java.lang.reflect.Method;


public class FayeService extends Service implements FayeListener {

    private static final String LOG_TAG = "Faye";

    private static final boolean DEBUG_MODE                 = true;
    private static final String LOG_FILE                    = "mttlog";
    private static final String APP_PACKAGE                 = "com.monmouth.monmouthtelecom";
    private static final String NOTIF_ICON                  = "icon_notification";
    private static final String NOTIF_ICON_TYPE             = "drawable";
    private static final String NOTIF_TITLE_DEF             = "Monmouth Telecom";
    private static final String NOTIF_TEXT_DEF              = "Touch to open app.";
    private static final int NOTIF_ICON_ID                  = 1;
    private static final String APP_ACTIVITY                = "com.monmouth.monmouthtelecom.MonmouthTelecom";

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private FayeClient mClient;
    private FayePG fayePG;
    private boolean fayePGAlive;
    private MobileCarrier carrier;
    private String notifText;
    private String notifTitle;
    private String notifTicker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // SSL bug in pre-Gingerbread devices makes websockets currently unusable
        if (android.os.Build.VERSION.SDK_INT <= 8) return START_NOT_STICKY;
        if (DEBUG_MODE)
            logToFile();

        Log.i(LOG_TAG, "FayeService onStart Command");

        try {
            String address = intent.getStringExtra("address");
            String channel = intent.getStringExtra("channel");

            URI uri = URI.create(address);

            String user = intent.getStringExtra("user");
            String sid = intent.getStringExtra("sid");

            JSONObject ext = new JSONObject();
            ext.put("user", user);
            ext.put("sid", sid);

            carrier = new MobileCarrier(intent.getStringExtra("carrierName"),
                    intent.getStringExtra("countryCode"),
                    intent.getIntExtra("mcc", -1),
                    intent.getIntExtra("mnc", -1)
                    );

            if (mHandler == null) {
                Log.i(LOG_TAG, "Starting fayeservice thread");
                mHandlerThread = new HandlerThread("fayeService-thread");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());

                mClient = new FayeClient(mHandler, uri, channel);

                JSONObject keepAliveMsg = new JSONObject();
                keepAliveMsg.put("user", user);

                mClient.setKeepAliveChannel("/keepAlive");
                mClient.setKeepAliveMessage(keepAliveMsg);

                mClient.setFayeListener(this);
                mClient.connectToServer(ext);
                startForeground(NOTIF_ICON_ID, getNotification());
            } else {
                Log.i(LOG_TAG, "Fayeservice thread and websockets already exists!");
            }

        } catch (JSONException ex) {
            Log.e(LOG_TAG, "JSONException: " + ex.getMessage());
        }

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
        mHandlerThread.quitSafely();
        mHandler = null;
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
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
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

        MTTMsgExecuter mMttMsgExecuter = new MTTMsgExecuter(this, mHandler, fayePG, carrier, className);
        mMttMsgExecuter.execute(json);

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

    public void setNotificationTexts(String title, String text, String ticker) {
        notifTitle = title;
        notifText = text;
        notifTicker = ticker;
        startForeground(NOTIF_ICON_ID, getNotification());
    }

    public Notification getNotification() {
        Notification notif = null;
        try {
            int icon = this.getResources().getIdentifier(NOTIF_ICON, NOTIF_ICON_TYPE, APP_PACKAGE);
            if (icon == 0)
                Log.i(LOG_TAG, "notification icon not found");

            Intent notifIntent = new Intent(this, Class.forName(APP_ACTIVITY));
            notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (notifTitle == null || notifTitle.length() == 0)
                notifTitle = NOTIF_TITLE_DEF;
            if (notifText == null || notifText.length() == 0)
                notifText = NOTIF_TEXT_DEF;

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(icon)
                            .setOngoing(true)
                            .setContentIntent(pendingIntent)
                            .setContentTitle(notifTitle)
                            .setContentText(notifText);
            if (notifTicker != null && notifTicker.length() > 0)
                mBuilder.setTicker(notifTicker);

            notif = mBuilder.build();
        } catch (ClassNotFoundException ex) {
            Log.e(LOG_TAG, "Error: Can't find class of cordova activity!");
        }
        return notif;
    }

    private void logToFile() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String filePath = Environment.getExternalStorageDirectory() + "/" + LOG_FILE + timeStamp + ".txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, "-v", "time"});
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    public boolean isFayePGAlive() {
        return fayePGAlive;
    }

    public void setFayePGAlive(boolean fayePGAlive) {
        this.fayePGAlive = fayePGAlive;
    }

}