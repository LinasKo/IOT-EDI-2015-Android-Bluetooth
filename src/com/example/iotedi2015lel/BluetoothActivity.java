package com.example.iotedi2015lel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;

public class BluetoothActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;
	private Button onBtn;
	private Button offBtn;
	private Button listBtn;
	private Button findBtn;
	private TextView text;
	private BluetoothAdapter myBluetoothAdapter;
	private Set<BluetoothDevice> pairedDevices;
	private BluetoothDevice[] foundDevices;
	private int found_index;
	private ListView myListView;
	private ArrayAdapter<String> BTArrayAdapter;
	private UUID MY_UUID; // TODO - initialize

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth);

		// take an instance of BluetoothAdapter - Bluetooth radio
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (myBluetoothAdapter == null) {
			onBtn.setEnabled(false);
			offBtn.setEnabled(false);
			listBtn.setEnabled(false);
			findBtn.setEnabled(false);
			text.setText("Status: not supported");

			Toast.makeText(getApplicationContext(),
					"Your device does not support Bluetooth", Toast.LENGTH_LONG)
					.show();
		} else {
			text = (TextView) findViewById(R.id.text);
			onBtn = (Button) findViewById(R.id.turnOn);
			onBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					on(v);
				}
			});

			offBtn = (Button) findViewById(R.id.turnOff);
			offBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					off(v);
				}
			});

			listBtn = (Button) findViewById(R.id.paired);
			listBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
					
					// TODO #1 debugging to show uuid format
					BluetoothDevice d0 = myBluetoothAdapter.getBondedDevices().iterator().next();
					String msg = d0.getName();
					for (ParcelUuid uuid : d0.getUuids()){
						msg += "\n" + uuid.getUuid().toString();
					}
					Log.d("UUIDS List", msg);
					
					
					list(v);
				}
			});

			findBtn = (Button) findViewById(R.id.search);
			findBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					find(v);
				}
			});

			myListView = (ListView) findViewById(R.id.listView1);
			foundDevices = new BluetoothDevice[20];
			found_index = 0;

			// create the arrayAdapter that contains the BTDevices, and set it
			// to the ListView
			BTArrayAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1);
			myListView.setAdapter(BTArrayAdapter);

			myListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					BluetoothDevice device = foundDevices[arg2];
					Toast.makeText(getApplicationContext(), device.getName(),
							Toast.LENGTH_SHORT).show();
					pairDevice(device);
					ConnectThread connectionThread = new ConnectThread(device);
					connectionThread.start();
				}
			});
		}
	}

	public void on(View view) {
		if (!myBluetoothAdapter.isEnabled()) {
			Intent turnOnIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

			Toast.makeText(getApplicationContext(), "Bluetooth turned on",
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth is already on",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (myBluetoothAdapter.isEnabled()) {
				text.setText("Status: Enabled");
			} else {
				text.setText("Status: Disabled");
			}
		}
	}

	public void list(View view) {
		// get paired devices
		pairedDevices = myBluetoothAdapter.getBondedDevices();

		// put it's one to the adapter
		for (BluetoothDevice device : pairedDevices)
			BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

		Toast.makeText(getApplicationContext(), "Show Paired Devices",
				Toast.LENGTH_SHORT).show();

	}

	final BroadcastReceiver bReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// add the name and the MAC address of the object to the
				foundDevices[found_index++] = device;
				// arrayAdapter
				BTArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
				BTArrayAdapter.notifyDataSetChanged();
			}
		}
	};

	public void find(View view) {
		if (myBluetoothAdapter.isDiscovering()) {
			// the button is pressed when it discovers, so cancel the discovery
			myBluetoothAdapter.cancelDiscovery();
		} else {
			BTArrayAdapter.clear();
			myBluetoothAdapter.startDiscovery();

			registerReceiver(bReceiver, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));
		}
	}

	public void off(View view) {
		myBluetoothAdapter.disable();
		text.setText("Status: Disconnected");

		Toast.makeText(getApplicationContext(), "Bluetooth turned off",
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(bReceiver);
	}

	public void getData(BluetoothDevice device) {
		//
	}

	private void pairDevice(BluetoothDevice device) {
		try {

			Method m = device.getClass()
					.getMethod("createBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			Log.e("result", e.getMessage());
		}
	}

	private void unpairDevice(BluetoothDevice device) {
		try {
			Method m = device.getClass()
					.getMethod("removeBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			Log.e("result", e.getMessage());
		}
	}

	
	// #1 Added Thread to connect to another device
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			myBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
			Toast.makeText(getApplicationContext(),
					"I should now be connected", Toast.LENGTH_LONG).show();
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}
}