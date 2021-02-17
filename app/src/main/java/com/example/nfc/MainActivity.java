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
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.nio.file.Files.write;

public class MainActivity extends AppCompatActivity {
    public static final String Error_Detected = "Nije detektiran NFC tag";
    public static final String Write_Error = "Pogreška prilikom pisanja,pokušajte ponovo";
    public static final String Write_Success ="Tekst uspješno napisan";
    NfcAdapter nfcAdapter;
    IntentFilter writingTagFilters[];
    PendingIntent pendingIntent;
    boolean writeMode;
    Context context;
    Tag tag;
    TextView poruka;
    TextView nfc_sadrzaj;
    Button AktivirajBotun;

    private Button button;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        poruka = (TextView) findViewById((R.id.poruka));
        nfc_sadrzaj = (TextView) findViewById((R.id.nfc_sadrzaj));
        AktivirajBotun = (Button) findViewById((R.id.AktivirajBotun));
        button = (Button) findViewById(R.id.PovijestTag);
        context = this;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryActivity();
            }
        });


        AktivirajBotun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (tag == null) {
                        Toast.makeText(context, Error_Detected, Toast.LENGTH_LONG).show();
                    } else {
                        write("Text" + poruka.getText().toString(), tag);
                        Toast.makeText(context, Write_Success, Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(context, Write_Error, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

            }


        });


        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter == null) {
            Toast.makeText(this ,"Ovaj uredaj ne podrzava NFC",Toast.LENGTH_SHORT).show();
            finish();
        }
        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] { tagDetected };
    }


    public void openHistoryActivity() {
        Intent intent = new Intent(this,HistoryActivity.class);
        startActivity(intent);
    }


    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || nfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if(rawMsgs != null) {
                msgs = new NdefMessage[ rawMsgs.length];
                for(int i = 0; i < rawMsgs.length; i++) {
                    msgs [i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage [] msgs) {
        if(msgs == null || msgs.length == 0) return;

        String text = "";

        byte [] payload = msgs [0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1 , payload.length - languageCodeLength - 1, textEncoding);

        }catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding" , e.toString());
        }
        nfc_sadrzaj.setText("NFC sadrzaj: " + text);
    }

    private void write(String text , Tag tag) throws IOException,FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0,payload,1,langLength);
        System.arraycopy(textBytes,0,payload,1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0] , payload);

        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }
    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,writingTagFilters,null);
    }

    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }




}