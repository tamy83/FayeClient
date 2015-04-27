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
    private MobileCarrier carrier;

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

            carrier = new MobileCarrier(intent.getStringExtra("carrierName"),
                    intent.getStringExtra("countryCode"),
                    intent.getIntExtra("mcc", -1),
                    intent.getIntExtra("mnc", -1)
                    );

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
        Log.i(LOG_TAG, "carrierName: " + carrier.getCarrierName()
                + " countryCode: " + carrier.getCountryCode() + " mcc: " + carrier.getMcc()
                + " mnc: " + carrier.getMnc());

        MTTMsgExecuter mMttMsgExecuter = new MTTMsgExecuter(this, carrier);

        mMttMsgExecuter.execute(json);

//        displayContacts();
//        wtf();
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





    private void displayContacts() {

        /*
        ContentResolver cr = getContentResolver();

        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode("9175430921"));
        Cursor cur = cr.query(uri, new String[]{PhoneLookup._ID, PhoneLookup.DISPLAY_NAME}, null, null, null);

        //if (cur.getCount() > 0) {
        try {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.i(LOG_TAG, "ID:" + id + " Name: " + name + " Phone No: " + phoneNo);
                    }
                    pCur.close();
                }
            }
            //}
        } finally {
            cur.close();
        }
        */
        String contactName = "", contactId = "", contactLookupKey = "";
        ContentResolver localContentResolver = getContentResolver();
        Cursor contactLookupCursor =
                localContentResolver.query(
                        Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                Uri.encode("002144")),
                        new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup._ID, PhoneLookup.LOOKUP_KEY},
                        null,
                        null,
                        null);
        try {
            while(contactLookupCursor.moveToNext()){
                contactName = contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
                contactId = contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup._ID));
                contactLookupKey = contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup.LOOKUP_KEY));
                Log.d(LOG_TAG, "contactMatch name: " + contactName);
                Log.d(LOG_TAG, "contactMatch id: " + contactId);
                Log.d(LOG_TAG, "contactMatch lookup key: " + contactLookupKey);
            }
        } finally {
            contactLookupCursor.close();
        }

    }

    private void wtf() {

/*
        Cursor c = getContentResolver().query(Contacts.CONTENT_URI,
                new String[] {Contacts._ID, ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, },
                Phone.NUMBER + "=?" + " AND "
                        + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
                new String[] {String.valueOf("002144")}, null);
        try {
            while(c.moveToNext()){
                String dataID = c.getString(c.getColumnIndexOrThrow(Contacts._ID));
                String sname = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                String fname = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                String dname = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                Log.d(LOG_TAG, "dataid: " + dataID); // id of data row
                Log.d(LOG_TAG, "phonelookup display name: " + name);
                Log.d(LOG_TAG, "given name: " + sname);
                Log.d(LOG_TAG, "family name: " + fname);
                Log.d(LOG_TAG, "structured name display name: " + dname);
                Log.d(LOG_TAG, "number: " + number);
                Log.d(LOG_TAG, "type: " + type);
                Log.d(LOG_TAG, "label: " + label);
            }
        } finally {
            c.close();
        }
*/
        //id: 1325 contactMatch lookup key: 286iff364658fb338a0

        Cursor c = getContentResolver().query(Data.CONTENT_URI, null,
                Phone.NUMBER + "=?",
                new String[] {String.valueOf("002144")}, null);
        try {
            while(c.moveToNext()){
                String dataID = c.getString(c.getColumnIndexOrThrow(Data._ID));
                String name = c.getString(c.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
                String number = c.getString(c.getColumnIndexOrThrow(Phone.NUMBER));
                String type = c.getString(c.getColumnIndexOrThrow(Phone.TYPE));
                String label = c.getString(c.getColumnIndexOrThrow(Phone.LABEL));
                Log.d(LOG_TAG, "dataid: " + dataID); // id of data row
                Log.d(LOG_TAG, "phonelookup display name: " + name);
                Log.d(LOG_TAG, "number: " + number);
                Log.d(LOG_TAG, "type: " + type);
                Log.d(LOG_TAG, "label: " + label);
            }
        } finally {
            c.close();
        }

        Cursor cursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                null, ContactsContract.Data.DISPLAY_NAME + "=?",
                new String[] {String.valueOf("Saul Goodman")}, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String mime = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                if (mime.equals(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                    String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                    String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                    String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                    Log.d(LOG_TAG, "structured name given name: " + givenName);
                    Log.d(LOG_TAG, "structured name family name: " + familyName);
                    Log.d(LOG_TAG, "structured name display name: " + displayName);
                } else if (mime.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                    if (ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE == cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))) {
                        Log.d(LOG_TAG, "phone #: " + cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    }
                }
            }
            cursor.close();
        }
    }

    private void removeNumber(int contactId) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                    .withSelection(Phone.NUMBER + "=? and " + Data.MIMETYPE + "=?", new String[]{String.valueOf(contactId), Phone.CONTENT_ITEM_TYPE})
                    .build());
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (OperationApplicationException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }


}