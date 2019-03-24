package abdullah.mansour.atmel;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;
import com.victor.loading.rotate.RotateLoading;

import java.util.ArrayList;
import java.util.List;

import abdullah.mansour.atmel.Model.DeviceModel;

public class MainActivity extends AppCompatActivity
{
    RotateLoading rotateLoading;

    BluetoothAdapter mBluetooth;

    private boolean flag = true;

    Button scan;
    RecyclerView recyclerView;
    public static DevicesListAdapter devicesListAdapter;
    public static List<DeviceModel> deviceModels;

    public static String EXTRA_NAME = "name";
    public static String EXTRA_MAC = "mac";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.devices_recyclerview);
        scan = findViewById(R.id.scan);
        rotateLoading = findViewById(R.id.rotateloading);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),DividerItemDecoration.VERTICAL));
        recyclerView.setHasFixedSize(true);

        deviceModels = new ArrayList<>();
        devicesListAdapter = new DevicesListAdapter(deviceModels);
        recyclerView.setAdapter(devicesListAdapter);

        mBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (mBluetooth == null)
        {
            Toast.makeText(this,"Your Device has NO Bluetooth",Toast.LENGTH_LONG).show();
            flag = false;
            finish();
        }
        else
        {
            if (!mBluetooth.isEnabled())
            {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBT);

                IntentFilter filterBT = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(mReceiver,filterBT);
            }

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }

        scan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.BLUETOOTH,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
                mBluetooth.startDiscovery();
                deviceModels.clear();
                scan.setEnabled(false);
                scan.setText("");
                rotateLoading.start();
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                DeviceModel deviceModel = new DeviceModel(dev.getName(), dev.getAddress());

                if (deviceModel.getName() == null)
                {
                    DeviceModel deviceModel2 = new DeviceModel("New Device", dev.getAddress());

                    deviceModels.add(deviceModel2);
                    devicesListAdapter.notifyDataSetChanged();
                } else
                {
                    deviceModels.add(deviceModel);
                    devicesListAdapter.notifyDataSetChanged();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                scan.setEnabled(true);
                scan.setText("Scan");
                rotateLoading.stop();
            }
            else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(),"You can not turn OFF bluetooth while Search is running",Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
                {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0) {
                    for (int i=0; i<grantResults.length; i++)
                    {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        {
                            Toast.makeText(getApplicationContext(), "Permission denied to Access Bluetooth", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "Permission denied to Access Bluetooth", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public class DevicesListAdapter extends RecyclerView.Adapter<DevicesListAdapter.Viewholder>
    {
        List<DeviceModel> deviceModels;

        public DevicesListAdapter(List<DeviceModel> deviceModels)
        {
            this.deviceModels = deviceModels;
        }

        @NonNull
        @Override
        public Viewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            return new  Viewholder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final Viewholder viewholder, final int i)
        {
            String time = deviceModels.get(i).getName();
            viewholder.name.setText(time);

            String booked = deviceModels.get(i).getMac();
            viewholder.mac.setText(booked);

            viewholder.materialRippleLayout.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    String name = viewholder.name.getText().toString();
                    String mac = viewholder.mac.getText().toString();

                    Intent intent = new Intent(getApplicationContext(), DeviceActivity.class);
                    intent.putExtra(EXTRA_NAME, name);
                    intent.putExtra(EXTRA_MAC, mac);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount()
        {
            return deviceModels.size();
        }

        class Viewholder extends RecyclerView.ViewHolder
        {
            View view;

            TextView name,mac;
            MaterialRippleLayout materialRippleLayout;

            Viewholder(@NonNull View itemView)
            {
                super(itemView);

                view = itemView;

                name = view.findViewById(R.id.device_name);
                mac = view.findViewById(R.id.device_mac);
                materialRippleLayout = view.findViewById(R.id.device_card);
            }
        }
    }

    private long exitTime = 0;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000)
        {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finishAffinity();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBackPressed()
    {
        doExitApp();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (flag != true) {
            unregisterReceiver(mReceiver);
        }
    }
}
