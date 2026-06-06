package org.example.model;


public class GameData {
    private String command;
    private String roomCode;
    private String senderNickname;
    private int level;
    private double expPercentage;
    private boolean isHost;
    private int rebirthCount;

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getSenderNickname() { return senderNickname; }
    public void setSenderNickname(String senderNickname) { this.senderNickname = senderNickname; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public double getExpPercentage() { return expPercentage; }
    public void setExpPercentage(double expPercentage) { this.expPercentage = expPercentage; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { this.isHost = host; }

    public int getRebirthCount() { return rebirthCount; }
    public void setRebirthCount(int rebirthCount) { this.rebirthCount = rebirthCount; }
}