// Version 1.6.0
package com.example.iotedi2015lel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
	private boolean discovered = false;
	private boolean pair_list_displayed_now = true;
	private int found_index;
	private ListView myListView;
	private ArrayAdapter<String> BTArrayAdapter;

	private String LOG_TAG_UUID = "UUID List";
	private String LOG_TAG_PROGRESS = "Progress";
	private final int MAX_NUMBER_OF_DEVICES = 20;

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

			Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth", Toast.LENGTH_LONG).show();
		} else {

			foundDevices = new BluetoothDevice[MAX_NUMBER_OF_DEVICES];
			found_index = 0;

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
					if (discovered) {
						pair_list_displayed_now = true;
						logToast("Clicked on 'Paired List' button", LOG_TAG_PROGRESS);
						list(v);
					} else {
						logToast("Rejected as not discovered. Please search for nearby devices first", LOG_TAG_PROGRESS);
					}
				}
			});

			findBtn = (Button) findViewById(R.id.search);
			findBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					logToast("Searching", LOG_TAG_PROGRESS);
					discovered = true;
					find(v);
				}
			});

			myListView = (ListView) findViewById(R.id.listView1);

			// create the arrayAdapter that contains the BTDevices, and set it
			// to the ListView

			Log.d(LOG_TAG_PROGRESS, "New array adapter is initialized??? for some reason");
			BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
			myListView.setAdapter(BTArrayAdapter);

			Log.d(LOG_TAG_PROGRESS, "OnClickListener is set onto the list to establish connections");
			myListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					BluetoothDevice device;
					device = pair_list_displayed_now ? device = getNthSetItem(pairedDevices, arg2) : foundDevices[arg2];
					// BluetoothDevice device = foundDevices[arg2];
					// Log.d(LOG_TAG_PROGRESS, "" +
					// Arrays.asList(foundDevices).contains(device)); // TODO
					if (!Arrays.asList(foundDevices).contains(device)) {
						logToast("Device is not in reach. Aborting.", LOG_TAG_PROGRESS);
					} else {
						logToast("trying to pair and (or) connect to: " + device.getName(), LOG_TAG_PROGRESS);
						if (!myBluetoothAdapter.getBondedDevices().contains(device)) {
							Log.d(LOG_TAG_PROGRESS, "Device not paired with. Pairing");
							pairDevice(device);
						}
						if (myBluetoothAdapter.getBondedDevices().contains(device)) {
							Log.d(LOG_TAG_PROGRESS, "Devices are paired. Attempting to establish connection.");
							ConnectThread connectionThread = new ConnectThread(device);
							connectionThread.start();
						} else {
							Log.d(LOG_TAG_PROGRESS, "Devices are not paired. Aborting.");
							Toast.makeText(getApplicationContext(), "Devices are not paired. Aborting.",
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			});
		}
	}

	public void on(View view) {
		if (!myBluetoothAdapter.isEnabled()) {
			Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

			Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_LONG).show();
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
		// the button is pressed when it discovers, so cancel the discovery
		myBluetoothAdapter.cancelDiscovery();

		// get paired devices
		pairedDevices = myBluetoothAdapter.getBondedDevices();

		// put it's one to the adapter
		for (BluetoothDevice device : pairedDevices)
			BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

		Log.d(LOG_TAG_PROGRESS, "Filled in the list of paired devices");

	}

	final BroadcastReceiver bReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(LOG_TAG_PROGRESS, "Device + " + device.getName()
						+ " is found. BTArrayAdapter is updated. Observers notified.");
				// add the name and the MAC address of the object to the
				foundDevices[found_index++] = device;
				// arrayAdapter
				BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				BTArrayAdapter.notifyDataSetChanged();
				pair_list_displayed_now = false;
			}
		}
	};

	public void find(View view) {

		if (myBluetoothAdapter.isDiscovering()) {
			Log.d(LOG_TAG_PROGRESS, "Search stopped");
			// the button is pressed when it discovers, so cancel the discovery
			myBluetoothAdapter.cancelDiscovery();
			Toast.makeText(getApplicationContext(), "Search Stopped", Toast.LENGTH_SHORT).show();
		} else {
			Log.d(LOG_TAG_PROGRESS,
					"Search commenced. Arrays cleared for discoveries. Discovery started. Broadcasts sent if something is found.");
			BTArrayAdapter.clear();
			foundDevices = new BluetoothDevice[MAX_NUMBER_OF_DEVICES];
			found_index = 0;
			myBluetoothAdapter.startDiscovery();

			registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		}
	}

	public void off(View view) {
		myBluetoothAdapter.disable();
		text.setText("Status: Disconnected");

		Toast.makeText(getApplicationContext(), "Bluetooth turned off", Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(bReceiver);
	}

	private void pairDevice(BluetoothDevice device) {
		try {
			Method m = device.getClass().getMethod("createBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
			Log.d(LOG_TAG_PROGRESS, "Pairing invoked");
		} catch (Exception e) {
			Log.d(LOG_TAG_PROGRESS, "Unable to pair.\n" + e.getMessage());
		}
	}

	private void unpairDevice(BluetoothDevice device) {
		try {
			Method m = device.getClass().getMethod("removeBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			Log.e("result", e.getMessage());
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private BluetoothSocket fallbackSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			Log.d(LOG_TAG_PROGRESS, "ConnectionThread initialization started.");
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			String msg = device.getName();
			Log.d(LOG_TAG_UUID, "just before breaking " + msg);
			for (ParcelUuid uuid : device.getUuids()) {
				msg += "\n" + uuid.getUuid().toString();
			}

			UUID deviceUUID = device.getUuids()[0].getUuid();
			logToast("UUID from the server is found.", LOG_TAG_PROGRESS);

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
			} catch (IOException e) {
			}
			mmSocket = tmp;
			logToast("ConnectionThread initialization completed. Socket acquired.", LOG_TAG_PROGRESS);
		}

		public void run() {
			Log.d(LOG_TAG_PROGRESS, "Connection thread started.");

			// Cancel discovery because it will slow down the connection
			myBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
				Log.d(LOG_TAG_PROGRESS, "Socket connected.");
			} catch (IOException connectException) {
				Log.d(LOG_TAG_PROGRESS, "Closing first socket. Trying fallback socket.");
				try {
					mmSocket.close();
					fallbackSocket = (BluetoothSocket) mmDevice.getClass()
							.getMethod("createRfcommSocket", new Class[] { int.class }).invoke(mmDevice, 1);
					fallbackSocket.connect();
					Log.d(LOG_TAG_PROGRESS, "Connection successful.");
				} catch (Exception e) {
					Log.d(LOG_TAG_PROGRESS, "Closing socket. failed because of " + e.getMessage());
					try {
						fallbackSocket.close();
						Log.d(LOG_TAG_PROGRESS, "Socket closed");
					} catch (IOException closeException) {
						Log.d(LOG_TAG_PROGRESS, "failed. Socket NOT closed because of: " + closeException.getMessage());
					}
					return;
				}
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
			Toast.makeText(getApplicationContext(), "I should now be connected", Toast.LENGTH_SHORT).show();
			Log.d(LOG_TAG_PROGRESS, "Connection established");
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			Log.d(LOG_TAG_PROGRESS, "Attempting to close socket.");
			try {
				mmSocket.close();
				Log.d(LOG_TAG_PROGRESS, "Socket closed.");

			} catch (IOException e) {
				Log.d(LOG_TAG_PROGRESS, "failed. Socket not closed because of: " + e.getMessage());
			}
		}
	}

	private void logToast(String msg, String tag) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		Log.d(tag, msg);
	}

	private BluetoothDevice getNthSetItem(Set<BluetoothDevice> set, int n) {
		BluetoothDevice head;
		do {
			head = set.iterator().next();
			if (n-- == 0)
				return head;
		} while (head != null);
		return head;
	}
}