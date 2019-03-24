package abdullah.mansour.atmel;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import abdullah.mansour.atmel.Model.DeviceModel;
import abdullah.mansour.atmel.Model.MessageModel;

@SuppressWarnings("ALL")
public class DeviceActivity extends AppCompatActivity
{
    BluetoothAdapter mBluetooth;
    BluetoothSocket mBtSocket;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    boolean isBtConnected = false;

    String name,mac;

    TextView device_name,device_mac,status;
    RecyclerView recyclerView;
    EditText msg_field;
    ImageButton clear_btn;
    Button send_btn;

    public static MessagesListAdapter messagesListAdapter;
    public static List<MessageModel> messageModels;

    ProgressDialog progressDialog;

    String read_message;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        name = getIntent().getStringExtra(MainActivity.EXTRA_NAME);
        mac = getIntent().getStringExtra(MainActivity.EXTRA_MAC);

        device_name = findViewById(R.id.device_name);
        device_mac = findViewById(R.id.mac_txt);
        status = findViewById(R.id.status_txt);
        recyclerView = findViewById(R.id.msgs_recyclerview);
        msg_field = findViewById(R.id.message_field);
        clear_btn = findViewById(R.id.clear_btn);
        send_btn = findViewById(R.id.send_btn);

        device_name.setText(name);
        device_mac.setText(mac);
        status.setText("Connecting");

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(),DividerItemDecoration.VERTICAL));
        recyclerView.setHasFixedSize(true);

        messageModels = new ArrayList<>();
        messagesListAdapter = new MessagesListAdapter(messageModels);
        recyclerView.setAdapter(messagesListAdapter);

        clear_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                msg_field.setText("");
            }
        });

        send_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String msg = msg_field.getText().toString();
                byte[] d = hexStringToByteArray(msg);
                writeBluetooth(d);

                MessageModel messageModel = new MessageModel(msg,1);
                messagesListAdapter.notifyDataSetChanged();
            }
        });

        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (mBluetooth == null)
        {
            Toast.makeText(this, "Your Device has NO Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!mBluetooth.isEnabled())
        {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBT);
        }

        IntentFilter filterBT = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filterBT);


        new ConnectBT().execute();
        new ReadBT().execute();
    }

    private void Disconnect()
    {
        if (mBtSocket != null) //If the btSocket is busy
        {
            try
            {
                mBtSocket.close(); //close connection
                //Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            } catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        finish(); //return to the first layout
    }

    void writeBluetooth(byte[] data)
    {
        if (mBtSocket!=null)
        {
            try
            {
                mBtSocket.getOutputStream().write(data);
            }
            catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(), "You can not turn OFF bluetooth while Search is running", Toast.LENGTH_LONG).show();
                        finish();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }
        }
    };

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progressDialog = ProgressDialog.show(DeviceActivity.this, "Connecting ...", "Please wait !!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (mBtSocket == null || !isBtConnected)
                {
                    BluetoothDevice dispositivo = mBluetooth.getRemoteDevice(mac);//connects to the device's address and checks if it's available
                    mBtSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBtSocket.connect();//start connection
                }
            } catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
                //Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                Toast.makeText(getApplicationContext(), "Connection Failed .. \n Is it a SPP Bluetooth ? Try again ..", Toast.LENGTH_SHORT).show();
                finish();
            } else
                {
                    status.setText("Connected");
                    Toast.makeText(getApplicationContext(), "Connected ..", Toast.LENGTH_SHORT).show();
                    isBtConnected = true;
                }
            progressDialog.dismiss();
        }
    }

    private class ReadBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        byte[] read;
        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {

            while (mBtSocket.isConnected())
            {
                try
                {
                    int av = mBtSocket.getInputStream().available();
                    if (av > 0)
                    {
                        read = new byte[av];
                        mBtSocket.getInputStream().read(read);

                        //readMsg = "";
                        for (int i=0; i<av; i++)
                        {
                            read_message += String.format("%X",read[i])+ " ";
                        }
                        //msg(readMsg);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                {
                                    //lblFileContents.setText(readMsg);
                                    MessageModel messageModel = new MessageModel(read_message,2);
                                    messagesListAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }
    }

    public byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public class MessagesListAdapter extends RecyclerView.Adapter<MessagesListAdapter.Viewholder>
    {
        List<MessageModel> messageModels;

        public MessagesListAdapter(List<MessageModel> messageModels)
        {
            this.messageModels = messageModels;
        }

        @NonNull
        @Override
        public Viewholder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
        {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            return new Viewholder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final Viewholder viewholder, final int i)
        {
            String message = messageModels.get(i).getMessage();
            int id = messageModels.get(i).getId();

            if (id == 1)
            {
                viewholder.message.setText(message);
                viewholder.message.setTextColor(getResources().getColor(R.color.to));

                viewholder.msg_image.setImageResource(R.drawable.ic_arrow_forward_black_24dp);
            } else if (id == 2)
            {
                viewholder.message.setText(message);
                viewholder.message.setTextColor(getResources().getColor(R.color.from));

                viewholder.msg_image.setImageResource(R.drawable.ic_arrow_back_black_24dp);
            }
        }

        @Override
        public int getItemCount()
        {
            return messageModels.size();
        }

        class Viewholder extends RecyclerView.ViewHolder
        {
            View view;

            TextView message;
            ImageView msg_image;

            Viewholder(@NonNull View itemView)
            {
                super(itemView);

                view = itemView;

                message = view.findViewById(R.id.msg);
                msg_image = view.findViewById(R.id.msg_image);
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Disconnect();
    }
}