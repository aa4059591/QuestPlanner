package org.example.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Player {
    private String nickname;
    private String password;
    private int level = 1;
    private int currentExp = 0;
    private String characterType = "EGG";
    private String lastCharacterType = "EGG";

    private int rebirthCount = 0;
    private Set<String> unlockedCollection = new HashSet<>();
    private Map<String, String> joinedRooms = new HashMap<>();

    public Player() {
        unlockedCollection.add("EGG");
    }

    public Player(String nickname, String hashedPassword) {
        this();
        this.nickname = nickname;
        this.password = hashedPassword;
    }

    public void addExp(int amount) {
        this.currentExp += amount;
        while (this.currentExp >= getRequiredExp() && this.level < 50) {
            this.currentExp -= getRequiredExp();
            this.level++;

            if (!"EGG".equals(this.characterType)) {
                int stage = getStageForLevel(this.level);
                for (int i = 1; i <= stage; i++) {
                    this.unlockedCollection.add(this.characterType + "_" + i);
                }
            }
        }
    }

    public void removeExp(int amount) {
        this.currentExp -= amount;

        while (this.currentExp < 0) {
            if (this.level > 1) {
                int oldLevel = this.level;
                this.level--;
                this.currentExp += getRequiredExp();

                // [도감 롤백] 레벨이 떨어졌으므로 도감 기록 삭제 검사
                rollbackCollection(oldLevel, this.level);

            } else {
                // 레벨 1인데 마이너스인 상황
                if (this.rebirthCount > 0) {
                    // 환생 취소 완벽 롤백
                    this.rebirthCount--;
                    this.level = 50;
                    this.currentExp += getRequiredExp();

                    if (!this.lastCharacterType.equals("EGG")) {
                        this.characterType = this.lastCharacterType;
                    }
                } else {
                    this.currentExp = 0;
                    break;
                }
            }
        }
    }

    private void rollbackCollection(int oldLevel, int newLevel) {
        if ("EGG".equals(this.characterType)) return;

        int oldStage = getStageForLevel(oldLevel);
        int newStage = getStageForLevel(newLevel);

        if (oldStage > newStage) {
            for (int i = newStage + 1; i <= oldStage; i++) {
                this.unlockedCollection.remove(this.characterType + "_" + i);
            }
        }
    }


    private int getStageForLevel(int lvl) {
        if (lvl >= 50) return 4;
        if (lvl >= 40) return 3;
        if (lvl >= 25) return 2;
        if (lvl >= 15) return 1;
        return 0;
    }

    public int getRequiredExp() {
        if (this.level >= 50) return 100;
        return level * 100;
    }

    public double getExpPercentage() {
        if (this.level >= 50) return 0.0;
        return (double) currentExp / getRequiredExp() * 100.0;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentExp() { return currentExp; }
    public void setCurrentExp(int currentExp) { this.currentExp = currentExp; }

    public String getCharacterType() { return characterType; }

    public void setCharacterType(String characterType) {
        if ("EGG".equals(characterType) && !this.characterType.equals("EGG")) {
            this.lastCharacterType = this.characterType;
        }
        this.characterType = characterType;
    }

    public int getRebirthCount() { return rebirthCount; }
    public void setRebirthCount(int rebirthCount) { this.rebirthCount = rebirthCount; }
    public Set<String> getUnlockedCollection() { return unlockedCollection; }
    public void setUnlockedCollection(Set<String> unlockedCollection) { this.unlockedCollection = unlockedCollection; }
    public Map<String, String> getJoinedRooms() { return joinedRooms; }
    public void setJoinedRooms(Map<String, String> joinedRooms) { this.joinedRooms = joinedRooms; }
}