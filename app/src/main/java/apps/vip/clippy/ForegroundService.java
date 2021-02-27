package apps.vip.clippy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ForegroundService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static Connection main = null;
    public static String url = "192.168.0.40";
    public static String port = "8765";
    public static boolean connected = false;
    public static boolean started = false;
    public static Context context=null;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        started = true;
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        ClipboardManager clipboardManager=(ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        context=this;
        String path = "get";
        main = new Connection(clipboardManager, getURI(url, port, path));
        main.setAsMain();
        clipboardManager.addPrimaryClipChangedListener(new ClipboardListener());
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        main.close();
        started = false;

    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private static URI getURI(String url, String port, String path) {
        URI uri = null;
        try {
            uri = new URI("ws://" + url + ":" + port + "/" + path + "");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return uri;
    }

    public static void sendCommand(ClipboardManager clipBoard, String command) {
        new Connection(clipBoard, getURI(url, port, "send"), "command", command);
    }

    public static void getInfo(ClipboardManager clipBoard, String command) {
        new Connection(clipBoard, getURI(url, port, "send"), "info", command);
    }
    public static void createLinksNotification(ArrayList<String> links) {
        if(links.size()==0){
            return;
        }

        String CHANNEL_ID = "Link";
        CharSequence name = "link";
        String Description = "for links from PC";
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        //create the channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setDescription(Description);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        if (links.size()==1) {
            for (int i = 0; i < links.size(); i++) {
                String link = links.get(i);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, browserIntent, 0);
                Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle("Link Received")
                        .setContentText(link)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
                mNotificationManager.notify(i + 2, notification);
            }
        }else{
            Intent notificationIntent = new Intent(context, LinksPage.class);


            LinksPage.links=links;
            PendingIntent pendingIntent = PendingIntent.getActivity(context,0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Links Received")
                    .setContentText("open to view links")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            mNotificationManager.notify(99, notification);
        }

    }

    private class ClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener {

        @Override
        public void onPrimaryClipChanged() {
            ClipboardManager clipBoard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            CharSequence pasteData = "";
            if (clipBoard.getPrimaryClip() != null) {
                ClipData.Item item = clipBoard.getPrimaryClip().getItemAt(0);
                pasteData = item.getText();
                if (Connection.lastRecieved.compareTo(String.valueOf(pasteData)) != 0) {
                    new Connection(clipBoard, getURI(url, port, "send"), "clipboard", String.valueOf(pasteData));
                }
            }
        }
    }
}
