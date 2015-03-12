package com.monmouth.fayePG;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
/**
 * This class echoes a string called from JavaScript.
 */
public class FayePG extends CordovaPlugin {

    private static final String LOG_TAG = "FayePG";

    private String address;

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
            this.init(address, callbackContext);
        } else if (action.equals("disc")) {
        } else if (action.equals("subscribe")) {
            String channel = args.getString(0);
            String command = args.getString(1);
            this.subscribe(channel, command, callbackContext);
        } else if (action.equals("sendMessage")) {
        } else {
            return false;
        }


        return true;
    }

    private void init(String address, CallbackContext callbackContext) {
        if (address != null && address.length() > 0) {
            this.address = address;
            Log.i(LOG_TAG, "init, address: " + this.address);
            callbackContext.success();
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    private void disc(CallbackContext callbackContext) {
        Log.i(LOG_TAG, "disconnect" );
        callbackContext.success();

    }
    private void subscribe(String channel, String command, CallbackContext callbackContext) {
        if ((channel != null && channel.length() > 0) /* do want to be able to send in "" commands && (command != null && command.length() > 0)*/) {
            Log.i(LOG_TAG, "subscribe method: address is: " + address);
            Log.i(LOG_TAG, "subscribe to: " + channel + " with command: " + command);
            Intent intent = new Intent(this.cordova.getActivity().getApplicationContext(), FayeService.class);
            intent.putExtra("address",address);
            intent.putExtra("channel",channel);
            this.cordova.getActivity().getApplicationContext().startService(intent);

            callbackContext.success();
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    private void sendMessage(String channel, JSONObject data, CallbackContext callbackContext) {
        Log.i(LOG_TAG, "sendMessage to: " + channel + " with data: " + data);
        callbackContext.success();

    }

}