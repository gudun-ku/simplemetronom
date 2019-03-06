package com.beloushkin.android.learn.simplemetronom;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimpleMetronomService extends Service {
    final String TAG = SimpleMetronomService.class.getSimpleName();

    public static final int MSG_NEW_TIME = 123212;
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;

    private Messenger mClient;
    private boolean mIsRunning;
    private int mInterval = 1000; // 60 rounds per second


    // Messenger object
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ScheduledExecutorService mScheduledExecutorService;
    LocalBinder mBinder = new LocalBinder();

    public void ringtone(){
        new Beeper().beep(50);
    }


    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClient = null;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToUI(Long valueToSend) {
        if (mClient != null) {
            try {
                // Send data as an Integer
                mClient.send(Message.obtain(null , MSG_NEW_TIME,0,0, valueToSend));
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClient = null;
            }
        }
    }

    public class LocalBinder extends Binder {
        SimpleMetronomService getService() {
            return SimpleMetronomService.this;
        }

        IBinder getMessenger() {
            return mMessenger.getBinder();
        }
    }

    public SimpleMetronomService() {

    }

    public boolean isRunning() {
        return  mIsRunning;
    }

    public void setInterval(int interval) {
        mInterval = interval;
    }

    public int getInterval(){
        return mInterval;
    }

    public void startTicking() {
        mIsRunning = true;
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long currTime = System.currentTimeMillis();
                Log.d(TAG, "run: " + currTime);
                ringtone();
                sendMessageToUI(currTime);

            }
        }, mInterval, mInterval, TimeUnit.MILLISECONDS);
    }

    public void stopTicking() {
        if(!mScheduledExecutorService.isShutdown())
            mScheduledExecutorService.shutdownNow();
        mIsRunning = false;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        stopTicking();
        Log.d(TAG, "onDestroy: ");
    }

}
