package com.example.android.myapplication;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import static com.example.android.myapplication.Constants.ACCOUNT_NAME;
import static com.example.android.myapplication.Constants.ACCOUNT_TYPE;
import static com.example.android.myapplication.LoginActivity.CONTENT_AUTHORITY;

public class MainActivity extends AppCompatActivity {

    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    public static final String ASK_FOR_PERMISSION_KEY = "123";
    private static final int REQUEST_CODE_FOR_ACCOUNT = 1;
    private static final String PASSWORD = "12345";

    private boolean askForPermissionsOnly = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Intent intent = new Intent(this, LoginActivity.class);
        // startActivityForResult(intent, REQUEST_CODE_FOR_ACCOUNT);


        Account account = new Account("123@123", ACCOUNT_TYPE);

        AccountManager mAccountManager = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        if (mAccountManager.addAccountExplicitly(account, PASSWORD, null)) {
            mAccountManager.setAuthToken(account, Constants.SUPER_AUTH_TOKEN_TYPE, "");
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
            new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
            PERMISSIONS_REQUEST_READ_CONTACTS);
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, CONTENT_AUTHORITY, bundle);

        if (getIntent().hasExtra(ASK_FOR_PERMISSION_KEY)) {
            askForPermissionsOnly = true;
        }

    }

    public Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isCallerSyncAdapter) {
            if (isCallerSyncAdapter) {
                uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true");
            }
            return uri;
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("[test]", "yay");
            } else {
                Log.d("[test]", "boo");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //addContact();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FOR_ACCOUNT) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addContact() {

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // insert account name and account type
        ops.add(ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.RawContacts.CONTENT_URI, true))
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                        ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                .build());

        // insert contact number
        ops.add(ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI, true))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, 0123)
                .build());

        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }
}
