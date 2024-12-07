package net.psimarron.tcpthree;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.WriterException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String IPADDRESS = "IPAddress";

    private InetAddress IPAddress;

    private final String IPPORT = "IPPort";

    private int IPPort;

    private int ServerPort = 30401;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;
        });

        String endpoint = String.format("%s:%d", getIPAddress(), ServerPort);

        TextView serverIp = findViewById(R.id.server_ip);

        serverIp.setText(endpoint);

        ImageView qrCode = findViewById(R.id.qr_code);

        qrCode.setImageBitmap(createQRCode(endpoint));

        Thread serverThread = new RunServerInThread(findViewById(R.id.last_message));

        serverThread.start();

        Button send = findViewById(R.id.send_button);

        send.setOnClickListener(v -> {
            Thread thread = new Thread(() -> {
                try {
                    String content = new Date().toString();

                    DatagramPacket packet = new DatagramPacket(
                            content.getBytes(),
                            content.length(),
                            IPAddress,
                            IPPort
                    );

                    DatagramSocket socket = new DatagramSocket();

                    socket.send(packet);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        });

        Button scan = findViewById(R.id.qr_scan);

        scan.setOnClickListener(v -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(this);

            intentIntegrator.setPrompt("Scan Server Endpoint from QR Code");
            intentIntegrator.setBeepEnabled(false);
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.initiateScan();
        });

        if (savedInstanceState != null) {
            String ip = savedInstanceState.getString(IPADDRESS);
            int port = savedInstanceState.getInt(IPPORT);

            SetEndpoint(ip + ":" + port);
        }

        LoadSettings();
    }

    private class RunServerInThread extends Thread {
        private final TextView Report;

        public RunServerInThread(TextView report) {
            Report = report;
        }

        @Override
        public void run() {
            String message;
            byte[] lmessage = new byte[1000];
            DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);

            DatagramSocket socket = null;

            for (; ; ) {
                try {
                    socket = new DatagramSocket(ServerPort);

                    socket.receive(packet);

                    message = new String(lmessage, 0, packet.getLength());

                    Report.setText(message);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        }
    }

    private static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();

                        if (sAddr.indexOf(':') < 0) return sAddr;
                    }
                }
            }
        } catch (Exception e) {
        }

        return "";
    }

    private static Bitmap createQRCode(String str) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        try {
            return barcodeEncoder.encodeBitmap(str, BarcodeFormat.QR_CODE, 400, 400);
        } catch (WriterException e) {
            e.printStackTrace();

            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (intentResult == null)
            super.onActivityResult(requestCode, resultCode, data);
        else
            SetEndpoint(intentResult.getContents());
    }

    private boolean SetEndpoint(String endpoint) {
        if (endpoint != null)
            try {
                String[] parts = endpoint.split(":");

                if (parts.length == 2) {
                    InetAddress ip = InetAddress.getByName(parts[0]);
                    int port = Integer.parseUnsignedInt(parts[1]);

                    if (ip == null && port < 1 && port > 0xffff) return false;

                    IPAddress = ip;
                    IPPort = port;

                    findViewById(R.id.send_button).setEnabled(true);

                    TextView lastMessage = findViewById(R.id.last_message);

                    lastMessage.setText(ip.getHostAddress() + ":" + port);

                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        return false;
    }

    private void SaveSettings() {
        if (IPAddress == null) return;

        SharedPreferences prefs = getSharedPreferences("TcpThree", MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(IPADDRESS, IPAddress.getHostAddress());
        editor.putInt(IPPORT, IPPort);

        editor.commit();
    }

    private void LoadSettings() {
        SharedPreferences prefs = getSharedPreferences("TcpThree", MODE_PRIVATE);

        String ip = prefs.getString(IPADDRESS, null);
        int port = prefs.getInt(IPPORT, -1);

        if (ip != null && port != -1) SetEndpoint(ip + ":" + port);
    }

    @Override
    protected void onStop() {
        SaveSettings();

        super.onStop();
    }

    @Override
    protected void onPause() {
        SaveSettings();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LoadSettings();
    }
}