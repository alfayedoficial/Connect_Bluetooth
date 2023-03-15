package com.example.connectbluetooth.sample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.connectbluetooth.R;

public class MsrFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private Button buttonGetTrack;
    private CheckBox checkDataEvent = null;

    private TextView deviceMessagesTextView;

    public static MsrFragment newInstance() {
        MsrFragment fragment = new MsrFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_msr, container, false);

        view.findViewById(R.id.buttonMsrOpen).setOnClickListener(this);
        view.findViewById(R.id.buttonMsrClose).setOnClickListener(this);
        view.findViewById(R.id.buttonCheckHealth).setOnClickListener(this);
        view.findViewById(R.id.buttonInfo).setOnClickListener(this);

        buttonGetTrack = view.findViewById(R.id.buttonGetTrack);
        buttonGetTrack.setOnClickListener(this);
        buttonGetTrack.setEnabled(false);

        checkDataEvent = view.findViewById(R.id.checkBoxEvent);
        checkDataEvent.setOnCheckedChangeListener(this);

        deviceMessagesTextView = view.findViewById(R.id.textViewDeviceMessages);
        deviceMessagesTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceMessagesTextView.setVerticalScrollBarEnabled(true);

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()) {
            case R.id.checkBoxEvent:
                MainActivityJava.getPrinterInstance().setDataEventEnabled(b);

                if (b) {
                    buttonGetTrack.setEnabled(false);
                } else {
                    buttonGetTrack.setEnabled(true);
                }

                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonMsrOpen:
                if (MainActivityJava.getPrinterInstance().msrOpen()) {
                    MainActivityJava.getPrinterInstance().setDataEventEnabled(checkDataEvent.isChecked());
                }
                break;

            case R.id.buttonGetTrack:
                String track1 = "Track1 : " + MainActivityJava.getPrinterInstance().getTrackData(1) + "\n";
                String track2 = "Track2 : " + MainActivityJava.getPrinterInstance().getTrackData(2) + "\n";
                String track3 = "Track3 : " + MainActivityJava.getPrinterInstance().getTrackData(3) + "\n";

                setDeviceLog(track1 + track2 + track3);
                break;

            case R.id.buttonCheckHealth:
                String checkHealth = MainActivityJava.getPrinterInstance().msrCheckHealth();
                if (checkHealth != null) {
                    Toast.makeText(getContext(), checkHealth, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.buttonInfo:
                String info = MainActivityJava.getPrinterInstance().getMsrInfo();
                if (info != null) {
                    Toast.makeText(getContext(), info, Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.buttonMsrClose:
                MainActivityJava.getPrinterInstance().msrClose();
                break;
        }
    }

    public void setDeviceLog(String data) {
        mHandler.obtainMessage(0, 0, 0, data).sendToTarget();
    }

    public final Handler mHandler = new Handler(new Handler.Callback() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    deviceMessagesTextView.append((String) msg.obj + "\n");

                    Layout layout = deviceMessagesTextView.getLayout();
                    if (layout != null) {
                        int y = layout.getLineTop(
                                deviceMessagesTextView.getLineCount()) - deviceMessagesTextView.getHeight();
                        if (y > 0) {
                            deviceMessagesTextView.scrollTo(0, y);
                            deviceMessagesTextView.invalidate();
                        }
                    }
                    break;
            }
            return false;
        }
    });
}
