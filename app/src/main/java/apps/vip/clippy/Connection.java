package apps.vip.clippy;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import androidx.core.app.NotificationCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.json.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class Connection {
    private final WebSocketClient mWs;
    public static String lastRecieved = "";
    boolean mainConnection = false;
    Connection(ClipboardManager clipboardManager, URI uri) {


        mWs = new WebSocketClient(uri) {
            @Override
            public void onMessage(String message) {
                System.out.println(message);
                Log.d("testing connection", "onMessage: " + message);
                JSONObject obj = null;
                try {
                    obj = new JSONObject(message);
                    String type = obj.getString("type");
                    String data = obj.getString("data");
                    if (type.compareTo("clipboard") == 0) {
                        lastRecieved = data;
                        ClipData clipData = ClipData.newPlainText("text", data);
                        clipboardManager.setPrimaryClip(clipData);
                        String[] arr=data.trim().split("\\s+");
                        ArrayList<String> links=new ArrayList<>();
                        for(String s:arr){
                            if ( Patterns.WEB_URL.matcher(s).matches()){
                                links.add(s);
                            }
                        }
                        System.out.println("Links in connection "+Arrays.toString(links.toArray()));
                        ForegroundService.createLinksNotification(links);
                    } else if (type.compareTo("info") == 0) {
                        JSONObject jsonData = new JSONObject(data);
                        if (jsonData.has("type")) {
                            type = jsonData.getString("type");
                            if (type.compareTo("media") == 0) {
                                String title = jsonData.getString("title");
                                String thumb = jsonData.getString("thumbnail");
                                if (media_control.playingTxt != null) {
                                    media_control.playingTxt.setText(title);
                                }
                                media_control.playingTXT = title;
                                System.out.println("playing title from connection " + title);
                            }
                        }

                    } else if (type.compareTo("file_path") == 0) {
                        String path = data;
                        System.out.println("filePath " + path);
                        String url = "http://" + ForegroundService.url + ":5000/get?f=" + path;
                        new downloadFile(url, MainActivity.context);


                    } else {

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onOpen( ServerHandshake handshake ) {
                System.out.println("opened connection");
                ForegroundService.connected = true;
                if (main_page.connectionStatus != null && mainConnection) {
                    main_page.connectionStatus.setText("Connected");
                }
                //this.send("test");

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onClose(int code, String reason, boolean remote ) {
                System.out.println("closed connection code:" + code + " reason:" + reason);
                ForegroundService.connected = false;
                if (main_page.connectionStatus != null && mainConnection) {
                    main_page.connectionStatus.setText("Not Connected");
                }
            }

            @Override
            public void onError(Exception ex) {
                ForegroundService.connected = false;
                ex.printStackTrace();
            }

        };
        //open websocket
        mWs.connect();
    }
    public boolean isConnected() {
        return mWs.getReadyState() == ReadyState.OPEN;
    }

    public Connection(ClipboardManager clipBoard, URI uri, String type, String data) {
        this(clipBoard, uri);
        while (!isConnected()) ;
        if (type.compareTo("clipboard") == 0) {
            sendClipboard(data);
        } else if (type.compareTo("command") == 0) {
            sendCommand(data);
        }
        if (type.compareTo("info") == 0) {
            getInfo(data);
        } else {

        }

    }

    private void getInfo(String command) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "info");
            obj.put("data", command);
            Log.d("send", "sendCommand: " + obj.toString());
            String msg = obj.toString();
            mWs.send(msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(String command) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "command");
            obj.put("data", command);
            Log.d("send", "sendCommand: " + obj.toString());
            String msg = obj.toString();
            mWs.send(msg);
            mWs.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendClipboard(String text) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "clipboard");
            obj.put("data", text);
            Log.d("send", "sendClipboard: " + obj.toString());
            String msg = obj.toString();
            mWs.send(msg);
            mWs.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mWs.close();
    }

    public void setAsMain() {
        this.mainConnection = true;
    }
}
