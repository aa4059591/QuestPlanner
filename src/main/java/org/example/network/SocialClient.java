package org.example.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.GameData;
import org.example.model.Player;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class SocialClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper mapper = new ObjectMapper();

    public int connect(String code, Player player, Consumer<GameData> onData, String mode, boolean isHost) {
        try {
            socket = new Socket("localhost", 9999);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            out.println(mode + ":" + code);

            String response = in.readLine();

            if (response != null && response.equals("DISBANDED")) {
                disconnect();
                return -2;
            }

            if (response == null || !response.equals("SUCCESS")) {
                disconnect();
                return 0;
            }

            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        GameData data = mapper.readValue(line, GameData.class);
                        onData.accept(data);
                    }
                } catch (IOException e) {
                }
            }).start();

            try { Thread.sleep(150); } catch (InterruptedException e) {}

            boolean currentHostStatus = isHost;
            if (player.getJoinedRooms().containsKey(code)) {
                currentHostStatus = player.getJoinedRooms().get(code).endsWith(" (방장)");
            }
            sendStatus(code, player, "JOIN", currentHostStatus);

            return 1;

        } catch (IOException e) {
            return -1;
        }
    }

    public void sendStatus(String roomCode, Player player, String command, boolean isHost) {
        if (out != null) {
            try {
                GameData data = new GameData();
                data.setCommand(command);
                data.setRoomCode(roomCode);
                data.setSenderNickname(player.getNickname());
                data.setLevel(player.getLevel());
                data.setExpPercentage(player.getExpPercentage());
                data.setHost(isHost);
                data.setRebirthCount(player.getRebirthCount());

                String json = mapper.writeValueAsString(data);
                out.println(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {}
    }
}