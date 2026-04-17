package com.xerolith.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class XeroBridgeService extends AccessibilityService {
    private static final String TAG = "XeroBridge";
    private static final int PORT = 9999;
    private Thread serverThread;

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "XeroBridge Accessibility Service Connected.");
        startLocalServer();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Passive listening for UI changes can be implemented here
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Service Interrupted");
    }

    private void startLocalServer() {
        serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Log.i(TAG, "Server listening on port " + PORT);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket client = serverSocket.accept();
                    handleClientRequest(client);
                }
            } catch (Exception e) {
                Log.e(TAG, "Server Error", e);
            }
        });
        serverThread.start();
    }

    private void handleClientRequest(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream out = client.getOutputStream();
            
            String line = in.readLine();
            if (line != null && line.startsWith("POST")) {
                // Simplified parser for demonstration.
                // In production, parse JSON payload for {"x": 500, "y": 500}
                int contentLength = 0;
                while (!(line = in.readLine()).isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(" ")[1].trim());
                    }
                }
                
                char[] body = new char[contentLength];
                in.read(body, 0, contentLength);
                String payload = new String(body);
                
                // Example payload: x=500,y=500
                if(payload.contains("x=") && payload.contains("y=")) {
                    String[] parts = payload.split(",");
                    int x = Integer.parseInt(parts[0].split("=")[1]);
                    int y = Integer.parseInt(parts[1].split("=")[1]);
                    
                    performTap(x, y);
                    
                    String response = "HTTP/1.1 200 OK\r\n\r\nSuccess";
                    out.write(response.getBytes());
                }
            }
            client.close();
        } catch (Exception e) {
            Log.e(TAG, "Client Handling Error", e);
        }
    }

    private void performTap(int x, int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.StrokeDescription clickStroke = 
            new GestureDescription.StrokeDescription(clickPath, 0, 50);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        dispatchGesture(clickBuilder.build(), null, null);
        Log.i(TAG, "Tapped at " + x + ", " + y);
    }
}
