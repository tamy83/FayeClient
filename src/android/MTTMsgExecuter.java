package com.monmouth.monmouthtelecom;

import org.json.JSONObject;
import com.monmouth.monmouthtelecom.MobileCarrier;

import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.net.Uri;
import java.util.ArrayList;
import android.content.ContentProviderOperation;

import java.util.HashMap;
import java.util.List;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.os.RemoteException;
import android.content.OperationApplicationException;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.content.Context;
import org.json.JSONException;
import com.monmouth.fayePG.FayePG;
import android.app.Activity;



public class MTTMsgExecuter {

    private static final String LOG_TAG                     = "Faye";
    private static final String CONTACT_NOTE                = "Contact created by Monmouth Telecom App";

    private MobileCarrier carrier;
    private Context context;
    private Class<?> className;
    private Handler mHandler;
    private ArrayList<ContentProviderOperation> ops;
    private FayePG fayePG;

    // TODO remove context & class (get from fayePG), remove handler?
    public MTTMsgExecuter(Context context, Handler mHandler, FayePG fayePG, MobileCarrier carrier, Class<?> activityClass) {
        this.carrier = carrier;
        this.context = context;
        this.className = activityClass;
        this.mHandler = mHandler;
        this.fayePG = fayePG;
    }

    public void execute(JSONObject fayeMsg) {
        try {
            String command = fayeMsg.getString("command");
            Log.i(LOG_TAG, "fayemsg command: " + command);
            if (command.equals("AddContact")) {
              JSONObject contact = fayeMsg.getJSONObject("contact");
              editContact(contact);
              Log.i(LOG_TAG, "carrier info: mcc:" + carrier.getMcc() + " mnc: " + carrier.getMnc());
              if (carrier.getMcc() == 310) {
                // t-mobile
                if (carrier.getMnc() == 26 || carrier.getMnc() == 60 || carrier.getMnc() == 160 || carrier.getMnc() == 260 || carrier.getMnc() == 490) {
                  contact.put("phoneNumber", carrier.convertPhoneNumber(contact.getString("phoneNumber")));
                  Log.i(LOG_TAG, "fayemsg contact: " + contact.toString());
                  editContact(contact);
                }
              }
            } else if (command.equals("OpenApp")) {
                Activity activity = fayePG.cordova.getActivity();
                if (activity instanceof MonmouthTelecom) {
                    if (((MonmouthTelecom) activity).isActivityPaused()) {
                        Intent notifIntent = new Intent(context, className);
                        notifIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Log.i(LOG_TAG, "opening app...");
                        // store incoming call info into app prefs so js can access
                        // js checks inc call info to know how to load
                        context.startActivity(notifIntent);
                    }
                }
                // vvv works!!!
                //fayePG.webView.sendJavascript("connectedUser = '" + fayeMsg.getString("incomingCall") + "';");
                //fayePG.webView.sendJavascript(jsCommand +"("+fayeMsg.toString()+");");
                if (fayePG.getCommand() != null)
                    fayePG.webView.sendJavascript(fayePG.getCommand() +"("+fayeMsg.toString()+");");
            } else if (command.equals("Response")) {
                Log.i(LOG_TAG, "execute response... ");
                if (fayePG.isActivityAlive()) {
                    // can use js here
                    if (fayePG.getCommand() != null)
                        fayePG.webView.sendJavascript(fayePG.getCommand() +"("+fayeMsg.toString()+");");
                } else {
                    Log.d(LOG_TAG, "can't run javascript...");
                }
            } else if (command.equals("UserHangup")) {
                if (fayePG.isActivityAlive()) {
                    // can use js here
                    if (fayePG.getCommand() != null) {
                        Log.d(LOG_TAG, "sending UserHangup cmd to js...");
                        fayePG.webView.sendJavascript(fayePG.getCommand() + "(" + fayeMsg.toString() + ");");
                    }
                } else {
                    Log.d(LOG_TAG, "can't run javascript...");
                }
            } else if (command.equals("CallStatus")) {
              if (fayePG.isActivityAlive()) {
                // can use js here
                if (fayePG.getCommand() != null) {
                  Log.d(LOG_TAG, "sending CallStatus cmd to js...");
                  fayePG.webView.sendJavascript(fayePG.getCommand() + "(" + fayeMsg.toString() + ");");
                }
              } else {
                Log.d(LOG_TAG, "can't run javascript...");
              }
            }
        } catch (JSONException ex) {
            Log.e(LOG_TAG, "invalid json");
            Log.e(LOG_TAG, ex.getMessage());
        }
    }


    private void editContact(JSONObject contact) throws JSONException {
      Log.i(LOG_TAG, "in editContact() w/contact: " + contact.toString());
        boolean contactFound = false;
        ops = new ArrayList<ContentProviderOperation>();
        // get list of all data rows and display names containing phone #
        String phoneNum = contact.getString("phoneNumber");
        HashMap<Integer, String> existingContacts = getDataRowIds(phoneNum);
        String fullName = contact.getString("firstName") + " " + contact.getString("lastName");
        Log.i(LOG_TAG, "contact name: " + fullName);

        // check if displaynames == caller id
        List<Integer> data = new ArrayList<Integer>();
        for (HashMap.Entry<Integer, String> entry : existingContacts.entrySet()) {
            Log.i(LOG_TAG, "Key = " + entry.getKey() + ", Value = " + entry.getValue());
            int id = entry.getKey();
            String name = entry.getValue();
            if (!name.equals(fullName)) {
                Log.i(LOG_TAG, "row id to delete: " + id + " name: " + name);
                data.add(id);
            } else
                contactFound = true;
        }

        if (!contactFound) {
            // search for contact name
            int contactID = getContactId(fullName);
            Log.i(LOG_TAG, "contact id: " + contactID);
            if (contactID != 0) {
                int rawContactID = getRawContactId(contactID);
                // add to existing contact
                addInsertPhoneNumberOp(rawContactID, phoneNum, Phone.TYPE_WORK);
            } else {
                // doesn't exist, insert new contact
                addInsertContactOp(fullName, phoneNum, Phone.TYPE_WORK);
            }
        }

        // if names differ, remove data rows, do deletes after inserts
        addRowsToDelete(data);
        executeOps();

    }

    private HashMap<Integer, String> getDataRowIds(String phoneNumber) {
        HashMap<Integer, String> result = new HashMap<Integer, String>();
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, null,
                Phone.NUMBER + "=?",
                new String[] {phoneNumber}, null);
        try {
            while(cursor.moveToNext()){
                int dataID = cursor.getInt(cursor.getColumnIndexOrThrow(Data._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(PhoneLookup.DISPLAY_NAME));
                Log.i(LOG_TAG, "dataid: " + dataID); // id of data row
                Log.i(LOG_TAG, "display name: " + name);
                result.put(dataID,name);
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private void addRowsToDelete(List<Integer> rows) {
        if (rows == null)
            return;
        for (Integer rowID : rows) {
            ops.add(ContentProviderOperation.newDelete(Data.CONTENT_URI)
                    .withSelection(Data._ID + "=? and " + Data.MIMETYPE + "=?", new String[]{String.valueOf(rowID), Phone.CONTENT_ITEM_TYPE})
                    .build());
        }
    }

    private void addInsertPhoneNumberOp(int rawContactId, String phoneNumber, int phoneNumberType) {
        if (rawContactId == 0) {
            // new contact, add id with back reference
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phoneNumber)
                    .withValue(Phone.TYPE, phoneNumberType)
                    .build());
        } else {
            // adding to existing contact
            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    .withValue(Phone.NUMBER, phoneNumber)
                    .withValue(Phone.TYPE, phoneNumberType)
                    .build());
        }
    }

    private void addInsertAccountInfoOp(String accountType, String accountName) {
        ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(RawContacts.ACCOUNT_NAME, accountName)
                .build());
    }

    private void addInsertRawContactOp(String displayName) {
        ops.add(ContentProviderOperation.newInsert(
                ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, displayName)
                .build());
    }

    private void addInsertNoteOp(int rawContactId, String note) {
        if (rawContactId == 0) {
            // new contact, add id with back reference
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, note)
                    .build());
        } else {
            // adding to existing contact
            ops.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                    .withValue(Note.NOTE, note)
                    .build());
        }
    }

    private void addInsertContactOp(String displayName, String phoneNumber, int phoneNumberType) {
        // insert caller id info
        addInsertAccountInfoOp(null, null);
        addInsertRawContactOp(displayName);
        addInsertPhoneNumberOp(0, phoneNumber, phoneNumberType);
        addInsertNoteOp(0, CONTACT_NOTE);
    }

    private void executeOps() {
        try {
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (OperationApplicationException ex) {
            Log.e(LOG_TAG, ex.getMessage());
        }
    }

    private int getContactId(String displayName) {
        int id = 0;
        Cursor contactLookupCursor = context.getContentResolver().query(Contacts.CONTENT_URI,
                new String[] {Contacts._ID}, Data.DISPLAY_NAME + "=?",
                new String[] {displayName}, null);

        try {
            while (contactLookupCursor.moveToNext()) {
                id = Integer.parseInt(contactLookupCursor.getString(contactLookupCursor.getColumnIndexOrThrow(Contacts._ID)));
            }
        } finally {
            contactLookupCursor.close();
        }

        return id;
    }

    private int getRawContactId(int contactId) {
        int rawContactId = 0;
        Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI,
                new String[]{RawContacts._ID},
                RawContacts.CONTACT_ID+"=?",
                new String[]{String.valueOf(contactId)}, null);
        try {
            if (c.moveToFirst()) {
                rawContactId = c.getInt(c.getColumnIndex(RawContacts._ID));
            }
        } finally {
            c.close();
        }
        Log.i(LOG_TAG,"Contact Id: " + contactId + " Raw Contact Id: " + rawContactId);
        return rawContactId;
    }

}
