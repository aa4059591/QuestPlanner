package org.example.model;

import java.util.List;

public class SaveData {
    private Player player;
    private List<Quest> quests;

    public SaveData() {}

    public SaveData(Player player, List<Quest> quests) {
        this.player = player;
        this.quests = quests;
    }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public List<Quest> getQuests() { return quests; }
    public void setQuests(List<Quest> quests) { this.quests = quests; }
}