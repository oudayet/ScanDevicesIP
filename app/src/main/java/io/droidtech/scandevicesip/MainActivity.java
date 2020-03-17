package io.droidtech.scandevicesip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    TextView tvWifiState;
    TextView tvScanning, tvResult;

    ArrayList<InetAddress> inetAddresses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWifiState = (TextView) findViewById(R.id.WifiState);
        tvScanning = (TextView) findViewById(R.id.Scanning);
        tvResult = (TextView) findViewById(R.id.Result);

        //To prevent memory leaks on devices prior to Android N,
        //retrieve WifiManager with
        //getApplicationContext().getSystemService(Context.WIFI_SERVICE),
        //instead of getSystemService(Context.WIFI_SERVICE)
        WifiManager wifiManager =
                (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        tvWifiState.setText(readtvWifiState(wifiManager));

        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ScanTask(tvScanning, tvResult).execute();
            }
        });


    }

    // "android.permission.ACCESS_WIFI_STATE" is needed
    private String readtvWifiState(WifiManager wm) {
        String result = "";
        switch (wm.getWifiState()) {
            case WifiManager.WIFI_STATE_DISABLED:
                result = "WIFI_STATE_DISABLED";
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                result = "WIFI_STATE_DISABLING";
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                result = "WIFI_STATE_ENABLED";
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                result = "WIFI_STATE_ENABLING";
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                result = "WIFI_STATE_UNKNOWN";
                break;
            default:
        }
        return result;
    }

    private class ScanTask extends AsyncTask<Void, String, Void> {

        TextView tvCurrentScanning, tvScanResullt;
        ArrayList<String> canonicalHostNames;

        public ScanTask(TextView tvCurrentScanning, TextView tvScanResullt) {
            this.tvCurrentScanning = tvCurrentScanning;
            this.tvScanResullt = tvScanResullt;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tvCurrentScanning.setText("Finished.");
            tvScanResullt.setText("");
            for (int i = 0; i < inetAddresses.size(); i++) {
                tvScanResullt.append(canonicalHostNames.get(i) + "\n");
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            scanInetAddresses();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            tvCurrentScanning.setText(values[0]);
        }


        private void scanInetAddresses() {
            //May be you have to adjust the timeout
            final int timeout = 1500;

            if (inetAddresses == null) {
                inetAddresses = new ArrayList<>();
            }
            inetAddresses.clear();

            if (canonicalHostNames == null) {
                canonicalHostNames = new ArrayList<>();
            }
            canonicalHostNames.clear();

            //For demonstration, scan 192.168.1.xxx only
            byte[] ip = {(byte) 192, (byte) 168, (byte) 1, 0};
            for (int j = 1; j < 11; j++) {
                ip[3] = (byte) j;
                try {
                    InetAddress checkAddress = InetAddress.getByAddress(ip);
                    publishProgress(checkAddress.getCanonicalHostName());
                    if (!runCommande("ping -c 1 " + checkAddress.getHostName()).contains("Unreachable")) {
                        Log.d("SCAN", "Reachable: " + checkAddress.getHostName());
                        inetAddresses.add(checkAddress);
                        canonicalHostNames.add(checkAddress.getCanonicalHostName());
                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    publishProgress(e.getMessage());
                }
            }
        }
    }

    String runCommande(String cmd) {
        StringBuffer cmdOut = new StringBuffer();

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStreamReader r = new InputStreamReader(process.getInputStream());
            char[] buf = new char[4096];
            int mRead = 0;
            BufferedReader bufferedReader = new BufferedReader(r);
            while ((mRead = bufferedReader.read(buf)) > 0) {
                cmdOut.append(buf, 0, mRead);
            }

            bufferedReader.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


        return cmdOut.toString();
    }

}
