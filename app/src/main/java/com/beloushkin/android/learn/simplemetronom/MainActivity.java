package com.beloushkin.android.learn.simplemetronom;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;

public class MainActivity extends AppCompatActivity
    implements View.OnClickListener{

    public static final String TAG = AppCompatActivity.class.getSimpleName();

    private ScrollableNumberPicker snpB;
    private ProgressBar pb;
    private Toast mToast;

    private SimpleMetronomService mService;
    private Messenger mServiceMessenger;
    private boolean mIsBound = false;
    final Messenger mMessenger = new Messenger(new IncomingEventHandler());

    private void showToast(String msg) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        mToast.show();
    }

    class IncomingEventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SimpleMetronomService.MSG_NEW_TIME:

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((SimpleMetronomService.LocalBinder) service).getService();
            mServiceMessenger = new Messenger(((SimpleMetronomService.LocalBinder) service).getMessenger());

            mIsBound = true;
            Log.d(TAG, "onServiceConnected: ");
            try {
                Message msg = Message.obtain(null, SimpleMetronomService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mServiceMessenger.send(msg);

            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
            refreshUi();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mServiceMessenger = null;
            mService = null;
            mIsBound = false;
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };

    @Override
    public void onClick(View v) {

        if (mMessenger != null && mService != null) {
            int rate = Integer.valueOf(snpB.getValue());
            if (rate <= 0) {
                return;
            }
            mService.setInterval(Math.round(1000 * 60 / rate));
            if (mService.isRunning()) {
                mService.stopTicking();
            } else {

                mService.startTicking();
            }
            refreshUi();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pb = findViewById(R.id.circularProgressbar);
        snpB = findViewById(R.id.snp_vertical);
        snpB.setOnClickListener(this);
        doBindService();
    }

    private void refreshUi() {
        snpB.setValue((int) Math.round(1.0/ (mService.getInterval() / 1000.0) * 60));
        if (mService.isRunning()) {
            pb.setProgress(100);
            snpB.setStepSize(0);
        } else {
            snpB.setStepSize(5);
            pb.setProgress(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
    }

    void doBindService() {
        bindService(new Intent(this, SimpleMetronomService.class), mConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "doBindService: ");

    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, SimpleMetronomService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "doUnbindService: ");
        }
    }
}
