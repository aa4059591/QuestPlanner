package org.example.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SocialServer {
    private static final int PORT = 9999;
    public static final Map<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();
    public static final Map<String, Map<String, String>> roomRosters = new ConcurrentHashMap<>();
    public static final Set<String> disbandedRooms = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("QuestPlanner 길드 서버가 시작되었습니다. 포트: " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("서버 실행 오류: " + e.getMessage());
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String roomCode;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String handshake = in.readLine();
            if (handshake == null) return;

            if (handshake.startsWith("CREATE:")) {
                roomCode = handshake.substring(7).trim();

                String newBaseName = roomCode;
                if (roomCode.contains("_[PW:")) {
                    newBaseName = roomCode.substring(0, roomCode.indexOf("_[PW:"));
                }

                final String finalBaseName = newBaseName;

                boolean baseNameExists = false;
                for (String existingCode : SocialServer.rooms.keySet()) {
                    String existingBaseName = existingCode;
                    if (existingCode.contains("_[PW:")) {
                        existingBaseName = existingCode.substring(0, existingCode.indexOf("_[PW:"));
                    }
                    if (existingBaseName.equals(finalBaseName)) {
                        baseNameExists = true;
                        break;
                    }
                }

                if (baseNameExists) {
                    out.println("FAIL");
                    return;
                } else {
                    SocialServer.disbandedRooms.removeIf(disbandedCode -> {
                        String disbandedBaseName = disbandedCode;
                        if (disbandedCode.contains("_[PW:")) {
                            disbandedBaseName = disbandedCode.substring(0, disbandedCode.indexOf("_[PW:"));
                        }
                        return disbandedBaseName.equals(finalBaseName);
                    });

                    SocialServer.rooms.put(roomCode, new CopyOnWriteArrayList<>());
                    SocialServer.roomRosters.put(roomCode, new ConcurrentHashMap<>());
                    SocialServer.rooms.get(roomCode).add(this);
                    out.println("SUCCESS");
                }
            }
            else if (handshake.startsWith("JOIN:")) {
                roomCode = handshake.substring(5).trim();
                if (!SocialServer.rooms.containsKey(roomCode)) {
                    out.println("FAIL");
                    return;
                } else {
                    SocialServer.rooms.get(roomCode).add(this);
                    out.println("SUCCESS");

                    Map<String, String> roster = SocialServer.roomRosters.get(roomCode);
                    if (roster != null) {
                        for (String userData : roster.values()) out.println(userData);
                    }
                }
            }
            else if (handshake.startsWith("RECONNECT:")) {
                roomCode = handshake.substring(10).trim();
                if (SocialServer.disbandedRooms.contains(roomCode)) {
                    out.println("DISBANDED");
                    return;
                }
                SocialServer.rooms.putIfAbsent(roomCode, new CopyOnWriteArrayList<>());
                SocialServer.roomRosters.putIfAbsent(roomCode, new ConcurrentHashMap<>());
                SocialServer.rooms.get(roomCode).add(this);
                out.println("SUCCESS");

                Map<String, String> roster = SocialServer.roomRosters.get(roomCode);
                if (roster != null) {
                    for (String userData : roster.values()) out.println(userData);
                }
            }
            else {
                out.println("FAIL");
                return;
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.trim().isEmpty()) continue;
                String sender = "";
                try { sender = message.split("\"senderNickname\":\"")[1].split("\"")[0]; } catch (Exception e) {}

                if (message.contains("\"command\":\"DISBAND\"")) {
                    SocialServer.rooms.remove(roomCode);
                    SocialServer.roomRosters.remove(roomCode);
                    SocialServer.disbandedRooms.add(roomCode);
                } else if (message.contains("\"command\":\"LEAVE\"")) {
                    if (!sender.isEmpty() && SocialServer.roomRosters.containsKey(roomCode)) {
                        SocialServer.roomRosters.get(roomCode).remove(sender);
                    }
                } else if (message.contains("\"command\":\"TRANSFER:")) {
                    String newHost = message.split("TRANSFER:")[1].split("\"")[0];
                    if (SocialServer.roomRosters.containsKey(roomCode)) {
                        Map<String, String> roster = SocialServer.roomRosters.get(roomCode);
                        if (roster.containsKey(newHost)) {
                            String oldJson = roster.get(newHost);
                            String newJson = oldJson.replace("\"host\":false", "\"host\":true").replace("\"isHost\":false", "\"isHost\":true");
                            roster.put(newHost, newJson);
                        } else {
                            String dummyJson = "{\"command\":\"UPDATE\",\"roomCode\":\"" + roomCode + "\",\"senderNickname\":\"" + newHost + "\",\"level\":1,\"expPercentage\":0.0,\"host\":true,\"isHost\":true,\"rebirthCount\":0}";
                            roster.put(newHost, dummyJson);
                        }
                    }
                } else {
                    if (!sender.isEmpty() && SocialServer.roomRosters.containsKey(roomCode)) {
                        SocialServer.roomRosters.get(roomCode).put(sender, message);
                    }
                }
                broadcast(message);
            }
        } catch (IOException e) {
        } finally {
            if (roomCode != null && SocialServer.rooms.containsKey(roomCode)) {
                SocialServer.rooms.get(roomCode).remove(this);
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }

    private void broadcast(String message) {
        List<ClientHandler> clients = SocialServer.rooms.get(roomCode);
        if (clients != null) {
            for (ClientHandler client : clients) {
                if (client != this) {
                    client.out.println(message);
                }
            }
        }
    }
}