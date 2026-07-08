package com.example.iknos;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket mSocket;

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    // Fungsi untuk menyambungkan Android ke Server Backend
    public void connectSocket(String jwtToken) {
        if (mSocket != null && mSocket.connected()) {
            return; // Jika sudah tersambung, tidak perlu buat koneksi baru
        }

        try {
            // Sesuai README BE: Autentikasi dikirim lewat handshake auth
            IO.Options opts = new IO.Options();
            Map<String, String> authData = new HashMap<>();
            authData.put("token", jwtToken);
            opts.auth = authData;

            // Sesuaikan IP jika menggunakan emulator (10.0.2.2 adalah localhost laptop)
            mSocket = IO.socket("http://10.0.2.2:3000", opts);

            // Pasang log sistem untuk memantau status koneksi di Logcat
            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "WebSocket Connected!!!"));
            mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "WebSocket Disconnected"));
            mSocket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "Connect Error: " + args[0]));

            mSocket.connect();

        } catch (URISyntaxException e) {
            Log.e(TAG, "Koneksi Gagal: " + e.getMessage());
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    // Fungsi EMIT 1: Mengirim perintah join ke kamar tertentu
    public void joinRoom(String roomId) {
        if (mSocket != null && mSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("roomId", roomId);
                mSocket.emit("join_room", data);
                Log.d(TAG, "Emitted join_room untuk Room: " + roomId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Fungsi EMIT 2: Mengirim koordinat GPS kita ke server (di-throttle 10 detik oleh BE)
    public void pushLocation(String roomId, double lat, double lng) {
        if (mSocket != null && mSocket.connected()) {
            try {
                JSONObject data = new JSONObject();
                data.put("roomId", roomId);
                data.put("lat", lat);
                data.put("lng", lng);
                mSocket.emit("location_update", data);
                Log.d(TAG, "Push lokasi ke BE: Lat=" + lat + ", Lng=" + lng);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}