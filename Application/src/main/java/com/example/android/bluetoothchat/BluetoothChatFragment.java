/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.logger.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

//This fragment controls Bluetooth to communicate with other devices.

public class BluetoothChatFragment extends Fragment {
    private static final String TAG = "BluetoothChatFragment";
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int PIC_PHOTO=4;
    String address;
    ImageView Image_view;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    Button button_img;
    Uri uri;
    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    //Member object for the chat services

    private BluetoothChatService mChatService = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        View view=getView();
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
        button_img=(Button)view.findViewById(R.id.button_img);
        Image_view=(ImageView)view.findViewById(R.id.imageView);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                mChatService.start();
            }
        }
    }



    private void setupChat() {
        Log.d(TAG, "setupChat()");
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    try {
                        sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mChatService = new BluetoothChatService(getActivity(), mHandler);
        mOutStringBuffer = new StringBuffer("");
        button_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PIC_PHOTO);
            }
        });
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(String message) throws IOException {
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send_msg = message.getBytes();
            generate(Constants.TEXT,send_msg);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    public void generate(int msg_type,byte[] send_msg){
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        int len;
        bs.write(msg_type);
        len = send_msg.length;
        do {
            bs.write(len % 10);
            len = len / 10;
        } while (len > 0);
        try {
            bs.write(Constants.END_2);
            bs.write(send_msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mChatService.write(bs.toByteArray());
    }

    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                try {
                    sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    };

    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.WRITE_MSG:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    Log.d("Writing msg","Writing msg");
                    break;
                case Constants.READ_MSG:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    String readMessage = new String(readBuf);
                    Log.d("Reading MSg","Reading MSg");
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.WRITE_IMG:
                    Toast.makeText(getActivity(),"Image being sent",Toast.LENGTH_LONG).show();
                    break;
                case Constants.READ_IMG:
                    byte[] bs_img= (byte[]) msg.obj;
                    int len=bs_img.length;
                    Bitmap bm;
                    bm= BitmapFactory.decodeByteArray(bs_img,0,len);
                    Image_view.setImageBitmap(bm);
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            case PIC_PHOTO:

                if(resultCode==Activity.RESULT_OK){
                    uri=data.getData();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            Bitmap bitmap = null;
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                                if (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024) {
                                    float scalingFactor;

                                    if (bitmap.getWidth() >= bitmap.getHeight()) {
                                        scalingFactor = 1024f / bitmap.getWidth();
                                    } else {
                                        Matrix fixRotation = new Matrix();
                                        fixRotation.postRotate(90);
                                        scalingFactor = 1024f / bitmap.getHeight();
                                    }

                                    bitmap = Bitmap.createScaledBitmap(
                                            bitmap,
                                            (int) (bitmap.getWidth() * scalingFactor),
                                            (int) (bitmap.getHeight() * scalingFactor),
                                            false
                                    );
                                }

                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                byte[] byteArray = stream.toByteArray();
                                generate(Constants.IMAGE,byteArray);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                }
        }
    }


    private void connectDevice(Intent data, boolean secure) {
        address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }
    /*
private class getcode extends AsyncTask<Uri,String,String> {

    @Override
    protected String doInBackground(Uri... params) {
        String Img_to_be_sent="";
        try {
            Bitmap bitmap= MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),params[0]);
            ByteArrayOutputStream stream=new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
            byte[] byteArray=stream.toByteArray();
            Img_to_be_sent= Arrays.toString(byteArray);
            sendMessage(Img_to_be_sent);
            Log.d("Sending Image","On Activity Result"+byteArray);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                    if (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024) {
                        float scalingFactor;

                        if (bitmap.getWidth() >= bitmap.getHeight()) {
                            scalingFactor = 1024f / bitmap.getWidth();
                        } else {
                            Matrix fixRotation = new Matrix();
                            fixRotation.postRotate(90);
                            scalingFactor = 1024f / bitmap.getHeight();
                        }

                        bitmap = Bitmap.createScaledBitmap(
                                bitmap,
                                (int) (bitmap.getWidth() * scalingFactor),
                                (int) (bitmap.getHeight() * scalingFactor),
                                false
                        );
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    Img_to_be_sent = Arrays.toString(byteArray);
                    sendMessage(Img_to_be_sent);
                    Log.d("Sending Image", "On Activity Result" + byteArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return null;
    }
    }*/
}

