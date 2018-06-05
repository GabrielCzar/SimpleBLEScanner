package com.example.joelwasserman.androidbletutorial;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "TIME";
    private static int QNT = 0;
    private static Long INITIAL = null;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    ListView peripheralListView;
    HashMap<String, BluetoothDevice> peripheralDevices;

    // SignalLoss = 3.06 to 4.72 dBm
    // txPower = -59 to -65 na maioria dos casos.
    // ACIMA ou ABAIXO do SMARTPHONE melhora
    // Colocando o em direção ao lado correto do BLE tambem melhora

    // Tem uma taxa inicial de erro de 30cm dentro do raio de 1m ou 50%

    // 4 metros de distancia praticamente é isso mesmo (1m loss)

    // Apartir de 2m
    // txPower -65
    // signalLoss 10 * (4.5~4.72)

    // pega a media de 10 amostras por exemplo pode ser menos ou ate mesmo 31 amostras***
    
    public double rssToDistance(int rss) {
        DecimalFormat decimalFormat = new DecimalFormat(".#");
        DecimalFormatSymbols dsf = new DecimalFormatSymbols();
        dsf.setDecimalSeparator('.');
        decimalFormat.setDecimalFormatSymbols(dsf);

        double dist;

        int rssOneMeter = -62; //value media of txPower

        double loss = ((3.06 + 4.72) / 2); // value media of signalLoss
        double signalLoss = -10 * loss;

        dist = Math.pow(10, ((rss - rssOneMeter) / signalLoss));

        return Double.parseDouble(decimalFormat.format(dist));
    }

    public double calculateDistance(int rssi) {
        int txPower = -65; //hard coded power value. Usually ranges between -59 to -65

        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        }
        else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }
    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (INITIAL == null) {
                INITIAL = System.currentTimeMillis();
            }
            long init = System.currentTimeMillis();
            Log.d(TAG, "TIMESTAMP," + init + ",SEGUNDO," + ((init-INITIAL)/1000) + ",QNT," + QNT +
                    ",MAC,"+result.getDevice().getAddress()+ ",RSSI,"+result.getRssi()
                    +",NAME,"+result.getDevice().getName()+",UUIDS,"+ Arrays.toString(result.getDevice().getUuids()) +
                    ",DIST,"+ rssToDistance(result.getRssi())
            );

            QNT ++;
            peripheralTextView.append("Device Name: " +
                    result.getDevice().getName() +
                    " RSSI: " + result.getRssi() +
                    " ADDRESS: " + result.getDevice().getAddress() +
                    " DIST " + calculateDistance(result.getRssi()) +
                    " D: " + rssToDistance(result.getRssi()) +
                    "\n");

            if(result.getDevice().getName() != null)
                peripheralDevices.put(result.getDevice().getName().trim(),result.getDevice());

            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();

            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                peripheralTextView.scrollTo(0, scrollAmount);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peripheralDevices = new HashMap<>();

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        peripheralListView = (ListView)findViewById(R.id.peripheralListView);
        peripheralListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String text = peripheralListView.getItemAtPosition(i).toString();
                connect(peripheralDevices.get(text));
            }
        });

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        if (btManager != null) {
            btAdapter = btManager.getAdapter();
        }

        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);

                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            Toast.makeText(getApplicationContext(), "DESMISS", Toast.LENGTH_SHORT).show();
                        }
                    });

                    builder.show();
                }
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        peripheralListView.setAdapter(null);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        ArrayList<String> list = new ArrayList<>(peripheralDevices.keySet());

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        peripheralListView.setAdapter(adapter);
        peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void connect(BluetoothDevice device){
        if(device == null)
            return;

        Toast.makeText(getApplicationContext(),"CONNECTED", Toast.LENGTH_LONG).show();
        device.connectGatt(getApplicationContext(),true, mGattCallback);
    }


    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

    };
}
