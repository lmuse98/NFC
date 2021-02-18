package com.example.nfc;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nfc.R;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected = "Nije otkrivena NFC oznaka";
    public static final String Write_Success = "Text uspjesno napisan!";
    public static final String Write_Error = "Pogreška, pokušajte ponovo !";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    boolean writeMode;
    Tag tag;
    Context context;
    TextView edit_message;
    TextView nfc_contents;
    Button ActivateButton;
    private Button button;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit_message = (TextView) findViewById(R.id.edit_message);
        nfc_contents = (TextView) findViewById(R.id.nfc_contents);
        ActivateButton =  findViewById(R.id.ActivateButton);
        context = this;
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryActivity();
            }
        });


        ActivateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(tag ==null) {
                        Toast.makeText(context, Error_Detected, Toast.LENGTH_LONG).show();
                    } else {
                        write("PlainText|"+edit_message.getText().toString(), tag);
                        Toast.makeText(context, Write_Success, Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }

        });
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null){
            Toast.makeText(this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }
        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] { tagDetected };
    }

    private void openHistoryActivity() {
        Intent intent = new Intent(this,HistoryActivity.class);
        startActivity(intent);
    }

    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        nfc_contents.setText("NFC Content: " + text);
    }
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.

        if(tag== null){
            Toast.makeText(this, "Tag is not a null!",Toast.LENGTH_SHORT).show();
            return;
        }

        Ndef ndef = Ndef.get(tag);
        if(ndef == null){
            formatTag(tag, message);
        }

        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
        Toast.makeText(this, "Tag is written!",Toast.LENGTH_SHORT).show();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(nfcAdapter != null){
            WriteModeOff();
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        if(nfcAdapter != null){
            WriteModeOn();
        }
    }


    private void WriteModeOn(){
        writeMode = true;
        if(nfcAdapter==null)
        {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writingTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        if(nfcAdapter==null)
        {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage){
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if(ndefFormatable == null){
                Toast.makeText(this, "Tag is not formatable!",Toast.LENGTH_SHORT).show();
            }
            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();
            Toast.makeText(this, "Tag is written!",Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            Log.e("tag", e.getMessage());
        }
    }

}
