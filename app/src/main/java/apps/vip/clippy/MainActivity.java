package apps.vip.clippy;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    String ip="192.168.0.40";
    String port = "8765";
    boolean scan = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scan = false;
        EditText ip = findViewById(R.id.ip);
        EditText port = findViewById(R.id.port);
        ip.setText(this.ip);
        port.setText(this.port);
        Button save = findViewById(R.id.saveIP);
        Button stop = findViewById(R.id.stop);
        Context context = this;
        save.setOnClickListener(v -> openMainPage(context));
        stop.setOnClickListener(v -> new serviceControl().killService(context));
        Button scan = findViewById(R.id.scan);
        scan.setOnClickListener(v -> openScanner());
        if (ForegroundService.started) {
            if (ForegroundService.main == null || !ForegroundService.main.isConnected()) {
                new serviceControl().killService(context);
            } else {
                Toast.makeText(getApplicationContext(), "Service is running", Toast.LENGTH_SHORT).show();
                Intent activity = new Intent(context, main_page.class);
                startActivity(activity);
            }
        }
    }

    private void openMainPage(Context context) {
        EditText ip = findViewById(R.id.ip);
        EditText port = findViewById(R.id.port);
        ForegroundService.url = ip.getText().toString();
        ForegroundService.port = port.getText().toString();
        new serviceControl().startService(context);
        Intent activity = new Intent(context, main_page.class);
        startActivity(activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EditText ip = findViewById(R.id.ip);
        EditText port = findViewById(R.id.port);
        ip.setText(this.ip);
        port.setText(this.port);
        if (scan) {
            openMainPage(this);
        }
    }

    private void openScanner() {
        try {

            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

            startActivityForResult(intent, 0);

        } catch (Exception e) {

            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {

                String contents = data.getStringExtra("SCAN_RESULT");
                System.out.println("qr "+contents);
                try {
                    JSONObject jsonData = new JSONObject(contents);
                    System.out.println("qr "+jsonData.get("ip"));
                    System.out.println("qr "+jsonData.get("port"));
                    ip= String.valueOf(jsonData.get("ip"));
                    port = String.valueOf(jsonData.get("port"));
                    scan = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
                System.out.println("qr cancelled");

            }
        }
    }


}