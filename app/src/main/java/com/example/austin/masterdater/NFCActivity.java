package com.example.austin.masterdater;

import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import com.example.austin.masterdater.R;
import java.lang.Override;
import java.lang.String;
import java.lang.System;
import java.util.Locale;


public class NFCActivity extends AppCompatActivity {

    private TextView mInstr_TV;
    private TextView mFeedback_TV;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mNfcPendingIntent;
    private IntentFilter[] mNdefExchangeFilters;
    private NdefMessage mNdefMessage;
    private String mUserPhoneNumber;

    public NFCActivity() {
        // Required empty public constructor
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        mInstr_TV = (TextView) findViewById(R.id.Instruction_textView);
        mFeedback_TV = (TextView) findViewById(R.id.Feedback_textView);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            mFeedback_TV.setText("NFC is ready");
        } else {
            mFeedback_TV.setText("This phone is not NFC enabled.");
        }

        mNfcPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // Intent filters for exchanging over p2p.
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefDetected.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
        }
        mNdefExchangeFilters = new IntentFilter[] { ndefDetected };
    }

    @Override
    protected void onResume() {
        super.onResume();

        // get the user's phone number
        TelephonyManager tMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        mUserPhoneNumber = CalendarActivity.getMyNumber();
        if (mUserPhoneNumber == null) {
            mUserPhoneNumber = "1234567890";
        }

        if(mUserPhoneNumber != null){
            if(mUserPhoneNumber != null){
                // create an NDEF message with record of user's phone number of plain text type
                mNdefMessage = new NdefMessage(
                        new NdefRecord[] {
                                createNewTextRecord(mUserPhoneNumber, Locale.ENGLISH, true) });

                enableNdefExchangeMode();
            }
        }else{
            mFeedback_TV.setText("number is null");
            mNdefMessage = new NdefMessage(
                    new NdefRecord[] {
                            createNewTextRecord("1234567890", Locale.ENGLISH, true) });

            enableNdefExchangeMode();
        }}

    private void enableNdefExchangeMode() {

        mNfcAdapter.enableForegroundNdefPush(NFCActivity.this,
                mNdefMessage);
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
                mNdefExchangeFilters, null);
    }

    public static NdefRecord createNewTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char)(utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte)status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // NDEF exchange mode
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);


            // fireFriendRequest(msgs[0]); //TODO
            // this is where we send the message to the backend

            NdefRecord[] recs = msgs[0].getRecords();
            byte[] number = recs[0].getPayload();
            String numberEncoding;
            if ((number[0] & 128) != 0) numberEncoding = "UTF-16";
            else numberEncoding = "UTF-8";
            int languageCodeLength = number[0] & 0063;
            String friendNumber = "";
            try {
                friendNumber = new String(number, languageCodeLength + 1, number.length - languageCodeLength - 1, numberEncoding);
            }catch(Exception e){
                friendNumber = "Error";
            }
            //mFeedback_TV.setText("NFC connection successful with " + friendNumber); //TODO
            CalendarActivity.setFriendNumber(friendNumber);
            CalendarActivity.clearFriendList();
            CalendarActivity.getFriendEvents();
            Toast.makeText(this, "Added " + friendNumber + " via nfc!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs =
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record =
                        new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                        record
                });
                msgs = new NdefMessage[] {
                        msg
                };
            }
        } else {
            Log.d("a", "Unknown intent.");
            finish();
        }
        return msgs;
    }

}
