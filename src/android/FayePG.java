package com.monmouth.fayePG;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

import com.saulpower.fayeclient.FayeClient;

public class FayePG extends CordovaPlugin {

    private static final String LOG_TAG = "Faye";

    private String address;
    private String channel;
    private String command;
    private String user;
    private String sid;
    private FayeService fayeService;
    private Intent intent;
    private boolean fayeIsBound;

    public FayePG() {
        //intent = new Intent(this.cordova.getActivity().getApplicationContext(), FayeService.class);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

/*
        switch (action) {
            case "init":
                String address = args.getString(0);
                this.init(address, callbackContext);
                break;
            case "disc":
                break;
            case "subscribe":
                String channel = args.getString(0);
                String command = args.getString(1);
                this.subscribe(channel, command, callbackContext);
                break;
            case "sendMessage":
                break;
            default:
                return false;
        }
*/

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
        } else {
            return false;
        }
        return true;
    }

    private void init(String address, String user, String sid, CallbackContext callbackContext) {
        if ((address != null && address.length() > 0) && (user != null && user.length() > 0) && (sid != null && sid.length() > 0)) {
          //  intent.putExtra("address",address);
            this.address = address;
            this.user = user;
            this.sid= sid;
            Log.i(LOG_TAG, "address: " + this.address + " user: " + this.user + " sid: " + this.sid);
            /*
            if (!fayeIsBound) {
                Log.i(LOG_TAG, "FayePG init: fayeService is null");
                intent = new Intent(this.cordova.getActivity().getApplicationContext(), FayeService.class);
                intent.putExtra("address", address);
                intent.putExtra("user", user);
                intent.putExtra("sid", sid);
                doBindService();
                Log.i(LOG_TAG, "faye address: " + address);
                callbackContext.success();
            } else {
                Log.i(LOG_TAG, "FayePG subscribe: Already subscribed to faye.");
                callbackContext.error("Already subscribed to faye.");
            }
            */
        } else {
            callbackContext.error("Invalid argument(s).");
        }
    }

    private void disconnect(CallbackContext callbackContext) {
        Log.i(LOG_TAG, "disconnect" );
        fayeService.disconnect();
        doUnbindService();
        this.cordova.getActivity().getApplicationContext().stopService(intent);
        callbackContext.success();
    }

    private void subscribe(String channel, String command, CallbackContext callbackContext) {
        if (channel == null || channel.length() == 0 || command == null) {
            callbackContext.error("Expected one non-empty string argument.");
        } else if (address != null && address.length() != 0 && user != null && user.length() != 0 && sid != null && sid.length() != 0) {
            if (!fayeIsBound) {
                this.channel = channel;
                this.command = command;
                Log.i(LOG_TAG, "FayePG subscribe: fayeService is null");
                intent = new Intent(this.cordova.getActivity().getApplicationContext(), FayeService.class);
                intent.putExtra("address", address);
                intent.putExtra("user", user);
                intent.putExtra("sid", sid);
                intent.putExtra("channel", this.channel);
                intent.putExtra("command", this.command);
                doBindService();
                this.cordova.getActivity().getApplicationContext().startService(intent);
                Log.i(LOG_TAG, "faye address: " + address);
                Log.i(LOG_TAG, "subscribe to: " + this.channel + " with command: " + this.command);

                callbackContext.success();
            } else {
                Log.i(LOG_TAG, "FayePG subscribe: Already subscribed to faye.");
                callbackContext.error("Already subscribed to faye.");
            }
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
            Log.i(LOG_TAG, "fayeService is bound");
        }

        public void onServiceDisconnected(ComponentName className) {
            fayeService.setFaye(null);
            fayeService = null;
            Log.i(LOG_TAG, "fayeService is unbounded");
        }
    };

    void doBindService() {
        this.cordova.getActivity().getApplicationContext().bindService(intent,mConnection,
                Context.BIND_AUTO_CREATE);
        fayeIsBound = true;
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
        Log.i(LOG_TAG, "fayePG onPause called");
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
        Log.i(LOG_TAG, "fayePG onNewIntent called w/intent " + intent.toString());
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    public void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "fayePG onDestroy called");
    }


}