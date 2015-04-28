package com.monmouth.monmouthtelecom;

import org.json.JSONObject;
import com.monmouth.monmouthtelecom.MobileCarrier;
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

public class MTTMsgExecuter {

    private static final String LOG_TAG                     = "Faye";
    private static final String CONTACT_NOTE                = "Contact created by Monmouth Telecom App";

    private MobileCarrier carrier;
    private Context context;
    private ArrayList<ContentProviderOperation> ops;
    
    public MTTMsgExecuter(Context context, MobileCarrier carrier) {
        this.carrier = carrier;
        this.context = context;
        this.ops = new ArrayList<ContentProviderOperation>();
    }

    public void execute(JSONObject fayeMsg) {

        try {
            String command = fayeMsg.getString("command");
            Log.i(LOG_TAG, "fayemsg command: " + command);

            if (command.equals("AddContact")) {
                Log.i(LOG_TAG, "fayemsg contact: " + fayeMsg.getJSONObject("contact").toString());
                editContact(fayeMsg.getJSONObject("contact"));
            }
        } catch (JSONException ex) {
            Log.e(LOG_TAG, "invalid json");
            Log.e(LOG_TAG, ex.getMessage());
        }


    }

    private void editContact(JSONObject contact) throws JSONException {
        boolean contactFound = false;
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
        // if names differ, remove data row
        addRowsToDelete(data);

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
