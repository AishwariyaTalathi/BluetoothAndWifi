package com.example.android.bluetoothchat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class Button_Fragment extends Fragment {

    //Button Bluetooth_On;
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
     View  view= inflater.inflate(R.layout.activity_button__fragment, container, false);
    Button Bluetooth_On = (Button) view.findViewById(R.id.button);
        Bluetooth_On.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //view=getView();
                FragmentTransaction fragmenttransaction=getActivity().getSupportFragmentManager().beginTransaction();
                BluetoothChatFragment fragment=new BluetoothChatFragment();
                fragmenttransaction.replace(R.id.sample_main_layout, fragment);
                fragmenttransaction.commit();
            }
        });
        return view;
    }
}
