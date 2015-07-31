package com.monmouth.fayePG;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.Environment;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import com.saulpower.fayeclient.FayeClient;
import android.os.Looper;
import android.os.Handler;
import com.monmouth.monmouthtelecom.MobileCarrier;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.IOException;




public class FayePG extends CordovaPlugin {

    private static final String LOG_TAG                     = "Faye";

    private static final boolean DEBUG_MODE                 = false;
    private static final String LOG_FILE                    = "mttlog";

    private String address;
    private String channel;
    private String command;
    private String user;
    private String sid;
    private FayeService fayeService;
    private Intent intent;
    private boolean fayeIsBound;
    private MobileCarrier carrier;
    private boolean activityAlive;
    private CallbackContext disconnectCallbackContext = null;
    private CallbackContext subscribeCallbackContext = null;


  public FayePG() {
        if (DEBUG_MODE)
            logToFile();
        activityAlive = true;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if (action.equals("init")){
        String address = args.getString(0);
        String credentials = args.getString(1);
        String user = null, sid = null;
        String[] credentialsArray = credentials.split(":");
        if (credentialsArray != null && credentialsArray.length > 1) {
          user = credentialsArray[0];
          sid = credentialsArray[1];
        }
        init(address, user, sid, callbackContext);
      } else if (action.equals("disconnect")) {
        disconnect(callbackContext);
      } else if (action.equals("subscribe")) {
        String channel = args.getString(0);
        String command = args.getString(1);
        subscribe(channel, command, callbackContext);
      } else if (action.equals("sendMessage")) {
        String channel = args.getString(0);
        JSONObject data = args.getJSONObject(1);
        sendMessage(channel, data, callbackContext);
      } else if (action.equals("setNotifTexts")) {
        String title = args.getString(0);
        String text = args.getString(1);
        String ticker = args.getString(2);
        setNotifText(title, text, ticker, callbackContext);
      } else {
        return false;
      }
      return true;
    }

    private void setNotifText(String notifTitle, String notifText, String notifTicker, CallbackContext callbackContext) {
        if (!fayeIsBound) {
            Log.i(LOG_TAG, "FayePG setNotifText: no faye service");
            callbackContext.error("No faye service.");
            return;
        }
        fayeService.setNotificationTexts(notifTitle, notifText, notifTicker);
        callbackContext.success();

    }

    private void init(String address, String user, String sid, CallbackContext callbackContext) {
        if ((address != null && address.length() > 0) && (user != null && user.length() > 0) && (sid != null && sid.length() > 0)) {
            this.address = address;
            this.user = user;
            this.sid= sid;
            Log.i(LOG_TAG, "address: " + this.address + " user: " + this.user + " sid: " + this.sid);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());

            carrier = new MobileCarrier(sharedPrefs.getString("carrierName", null),
                    sharedPrefs.getString("countryCode", null),
                    Integer.parseInt(sharedPrefs.getString("mcc", "-1")),
                    Integer.parseInt(sharedPrefs.getString("mnc", "-1"))
                    );

            Log.i(LOG_TAG, "fayePG init carrierName: " + carrier.getCarrierName()
                    + " countryCode: " + carrier.getCountryCode() + " mcc: " + carrier.getMcc()
                    + " mnc: " + carrier.getMnc());
            callbackContext.success();
        } else {
            callbackContext.error("Invalid argument(s).");
        }
    }

    private void disconnect(CallbackContext callbackContext) {
      Log.i(LOG_TAG, "disconnect");
      fayeService.disconnect();
      doUnbindService();
      this.cordova.getActivity().getApplicationContext().stopService(intent);
      PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
      result.setKeepCallback(true);
      disconnectCallbackContext = callbackContext;
      disconnectCallbackContext.sendPluginResult(result);
    }

    private void subscribe(String channel, String command, CallbackContext callbackContext) {
        if (channel == null || channel.length() == 0 || command == null) {
            callbackContext.error("Expected one non-empty string argument.");
        } else if (address != null && address.length() != 0 && user != null && user.length() != 0 && sid != null && sid.length() != 0) {
            this.channel = channel;
            this.command = command;
            intent = new Intent(this.cordova.getActivity().getApplicationContext(), FayeService.class);
            intent.putExtra("address", address);
            intent.putExtra("user", user);
            intent.putExtra("sid", sid);
            intent.putExtra("channel", this.channel);
            intent.putExtra("command", this.command);
            intent.putExtra("carrierName", carrier.getCarrierName());
            intent.putExtra("countryCode", carrier.getCountryCode());
            intent.putExtra("mcc", carrier.getMcc());
            intent.putExtra("mnc", carrier.getMnc());
            if (!fayeIsBound) {
                doBindService();
                Log.i(LOG_TAG, "subscribe to: " + this.channel + " with command: " + this.command);
            } else {
                callbackContext.success("Already subscribed.");
            }
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            subscribeCallbackContext = callbackContext;
            subscribeCallbackContext.sendPluginResult(result);
        } else {
            callbackContext.error("Need to init before trying to subscribe.");
        }
    }

    private void sendMessage(String channel, JSONObject data, CallbackContext callbackContext) {
        Log.i(LOG_TAG, "sendMessage to: " + channel + " with data: " + data);
        if (!fayeIsBound) {
            Log.i(LOG_TAG, "FayePG sendMsg: no faye service");
        } else {
            fayeService.sendMessage(channel, data);
            callbackContext.success();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            fayeService = ((FayeService.FayeBinder)service).getService();
            fayeService.setFaye(FayePG.this);
            fayeService.setFayePGAlive(activityAlive);
            Log.i(LOG_TAG, "fayeService is bound");
            FayePG.this.cordova.getActivity().getApplicationContext().startService(intent);
        }

        public void onServiceDisconnected(ComponentName className) {
            fayeService.setFaye(null);
            fayeService = null;
            Log.i(LOG_TAG, "fayeService is unbounded");
        }
    };

    void doBindService() {
        fayeIsBound = this.cordova.getActivity().getApplicationContext().bindService(intent,mConnection,
                Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (fayeIsBound) {
            Log.i(LOG_TAG, "FayePG unbind");
            // Detach our existing connection.
            this.cordova.getActivity().getApplicationContext().unbindService(mConnection);
            fayeIsBound = false;
        }
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(true);
        Log.i(LOG_TAG, "fayePG onPause called w/multitasking " + multitasking);
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(true);
        Log.i(LOG_TAG, "fayePG onResume called w/multitasking " + multitasking);
    }

    /**
     * Called when the activity receives a new intent.
     */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "fayePG onDestroy called");
        activityAlive = false;
        if (fayeIsBound)
            fayeService.setFayePGAlive(activityAlive);
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

    public boolean isActivityAlive() {
        return activityAlive;
    }

    public String getCommand() {
        return command;
    }


  public CallbackContext getDisconnectCallbackContext() {
    return disconnectCallbackContext;
  }

  public CallbackContext getSubscribeCallbackContext() {
    return subscribeCallbackContext;
  }


}