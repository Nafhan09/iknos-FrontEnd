package com.example.iknos.socket;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import io.socket.client.Ack;

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
    // Fungsi meliputi validasi token, validasi koneksi dan pencatatan log
    public void connectSocket(String jwtToken) {
        if (mSocket != null && mSocket.connected()) {
            // Jika sudah tersambung, tidak perlu buat koneksi baru
            return;
        }

        try {
            // Autentikasi dikirim lewat handshake auth, Perlu tervalidasi terlebih dahulu sebelum memulai koneksi dengan websocket
            IO.Options opts = new IO.Options();
            Map<String, String> authData = new HashMap<>();
            authData.put("token", jwtToken);
            opts.auth = authData;

            // Koneksi pada IP Backend
            // SESUAIKAN IP TERGANTUNG RUNNING DEVICES
            mSocket = IO.socket("https://iknos-be.onrender.com", opts);

            // Log status koneksi di Logcat
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

    public interface JoinRoomCallback {
        void onSnapshot(JSONArray snapshot);
    }

    // Fungsi EMIT 1 untuk Mengirim perintah join ke Room tertentu
    public void joinRoom(String roomId, JoinRoomCallback callback) {
        if (mSocket != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("roomId", roomId);
                mSocket.emit("join_room", data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        if (args.length > 0 && args[0] instanceof JSONObject) {
                            JSONObject response = (JSONObject) args[0];
                            try {
                                if (response.getBoolean("success") && response.has("snapshot")) {
                                    JSONArray snapshot = response.getJSONArray("snapshot");
                                    if (callback != null) {
                                        callback.onSnapshot(snapshot);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                Log.d(TAG, "Emitted join_room untuk Room: " + roomId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Fungsi EMIT 2 untuk Mengirim koordinat GPS ke server
    public void pushLocation(String roomId, double lat, double lng) {
        if (mSocket != null) {
            try {
                JSONObject data = new JSONObject();
                data.put("roomId", roomId);
                data.put("lat", lat);
                data.put("lng", lng);
                mSocket.emit("location_update", data);
                Log.d(TAG, "Push koordinat ke Server: Lat=" + lat + ", Lng=" + lng);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}