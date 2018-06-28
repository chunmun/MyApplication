package com.example.android.myapplication;

import android.Manifest;
import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

import static android.support.v4.app.ActivityCompat.requestPermissions;
import static com.example.android.myapplication.Constants.ACCOUNT_NAME;
import static com.example.android.myapplication.Constants.ACCOUNT_TYPE;
import static com.example.android.myapplication.MainActivity.PERMISSIONS_REQUEST_READ_CONTACTS;

public class SyncService extends Service {

    private static SyncAdapter sAdapter;
    private final static Object lock = new Object();

    @Override
    public void onCreate() {
        android.os.Debug.waitForDebugger();
        super.onCreate();
        synchronized (lock) {
            if (sAdapter == null) {
                sAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sAdapter.getSyncAdapterBinder();
    }

    public class SyncAdapter extends AbstractThreadedSyncAdapter {

        private ContentResolver resolver;

        public SyncAdapter(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
            resolver = getContentResolver();
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this.getContext(), MainActivity.class);
                    intent.putExtra(MainActivity.ASK_FOR_PERMISSION_KEY, "123");
                    startActivity(intent);
                }
            }

            Log.d("[test]", "performing sync");
            String[] projection =
                    new String[]{
                            ContactsContract.Contacts._ID,
                    };

            Cursor cursor =
                    resolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            projection,
                            null,
                            null,
                            ContactsContract.Contacts.SORT_KEY_PRIMARY
                    );

            cursor.close();

            addContact("52930841678", "12348901928");
            /*

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long accountId = cursor.getLong(
                            cursor.getColumnIndex(ContactsContract.Contacts._ID));

                    Log.d("[test]", String.format("found contact %d", accountId));
                    if (!isInitialized(accountId)) {
                        String phoneNumber =
                    }
                }
                cursor.close();
            }
            */
        }

        private void addContact(String name, String phoneNumber) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            // insert account name and account type
            ops.add(ContentProviderOperation
                    .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true))
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                    .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                            ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                    .build());

            // insert structured name
            /*
            ops.add(ContentProviderOperation
                    .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());
            */

            // insert contact number
            ops.add(ContentProviderOperation
                    .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber)
                    .build());

            // insert mime-type data
            ops.add(ContentProviderOperation
                    .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, Constants.MIME_TYPE)
                    .withValue(ContactsContract.Data.DATA1, 12345)
                    .withValue(ContactsContract.Data.DATA2, "name")
                    .withValue(ContactsContract.Data.DATA3, "another")
                    .build());

            try {
                ContentProviderResult[] result = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                Log.d("[test]", result.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (OperationApplicationException e) {
                e.printStackTrace();
            }
        }

        private boolean isInitialized(long contactId) {
            Uri uri = ContactsContract.RawContacts.CONTENT_URI;
            Cursor entityCursor =
                    resolver.query(
                            uri,
                            new String[]{
                                    ContactsContract.RawContacts.ACCOUNT_TYPE},
                            ContactsContract.RawContacts._ID + "=?",
                            new String[]{String.valueOf(contactId)},
                            null);

            while (entityCursor.moveToNext()) {
                String type =
                        entityCursor.getString(entityCursor.getColumnIndexOrThrow(
                                ContactsContract.RawContacts.ACCOUNT_TYPE
                        ));
                if (type == ACCOUNT_TYPE) {
                    entityCursor.close();
                    return true;
                }

            }
            entityCursor.close();
            return false;
        }

        public Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isCallerSyncAdapter) {
            if (isCallerSyncAdapter) {
                uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
            }
            return uri;
        }

    }
}

