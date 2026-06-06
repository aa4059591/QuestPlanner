package org.example.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.formdev.flatlaf.FlatDarkLaf;
import org.example.model.GameData;
import org.example.model.Player;
import org.example.model.Quest;
import org.example.model.SaveData;
import org.example.network.SocialClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainFrame extends JFrame {

    private Player player;
    private List<Quest> quests;
    private String SAVE_FILE;

    private ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private LocalDate currentDate = LocalDate.now();

    private Map<String, SocialClient> activeClients = new HashMap<>();
    private Map<String, Map<String, GameData>> multiRoomMembers = new HashMap<>();
    private List<String> roomCodesList = new ArrayList<>();
    private int currentRoomIndex = -1;

    private JPanel topSplitPanel;
    private JPanel myPanel;
    private JPanel guildContainer;

    private JLabel statusLabel;
    private JLabel totalCompletedLabel;
    private JProgressBar expBar;
    private JLabel imageLabel;
    private JPanel partyPanel;
    private JLabel guildNameLabel;
    private JLabel dateLabel;
    private JPanel todoPanel;
    private JPanel completedPanel;
    private JLabel summaryLabel;
    private JButton btnRebirth;

    public MainFrame() {
        if (!doLogin()) {
            System.exit(0);
        }
        setupUI();
        initSavedRooms();
        renderQuests();
    }

    private boolean doLogin() {
        while (true) {
            LoginDialog login = new LoginDialog();
            login.setVisible(true);

            if (login.getNickname() == null) return false;

            this.SAVE_FILE = login.getNickname() + "_data.json";

            if (login.isNewUser()) {
                this.player = new Player(login.getNickname(), login.getHashedPassword());
                this.quests = new ArrayList<>();
                saveGameData();
                return true;
            } else {
                try {
                    File file = new File(SAVE_FILE);
                    SaveData data = mapper.readValue(file, SaveData.class);

                    if (data.getPlayer().getPassword() != null &&
                            data.getPlayer().getPassword().equals(login.getHashedPassword())) {
                        this.player = data.getPlayer();
                        this.quests = data.getQuests();
                        if (player.getCharacterType() == null) player.setCharacterType("EGG");
                        return true;
                    } else {
                        JOptionPane.showMessageDialog(null, "비밀번호가 일치하지 않습니다.", "로그인 실패", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "데이터 로드 오류가 발생했습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
    }

    private void saveGameData() {
        try {
            SaveData data = new SaveData(player, quests);
            mapper.writeValue(new File(SAVE_FILE), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isHost(String code) {
        String name = player.getJoinedRooms().get(code);
        return name != null && name.endsWith(" (방장)");
    }

    private void initSavedRooms() {
        if (!player.getJoinedRooms().isEmpty()) {
            roomCodesList.addAll(player.getJoinedRooms().keySet());
            List<String> disbandedCodes = new ArrayList<>();

            for (String code : roomCodesList) {
                int status = connectToRoom(code, player.getJoinedRooms().get(code), "RECONNECT", isHost(code));
                if (status == -2) disbandedCodes.add(code);
            }

            if (!disbandedCodes.isEmpty()) {
                for (String code : disbandedCodes) {
                    String name = player.getJoinedRooms().get(code).replace(" (방장)", "");
                    JOptionPane.showMessageDialog(this, "오프라인 동안 [" + name + "] 길드가 해산되었습니다.");
                    player.getJoinedRooms().remove(code);
                    roomCodesList.remove(code);
                }
                saveGameData();
            }

            if (!roomCodesList.isEmpty()) currentRoomIndex = 0;
        }
        updateGuildVisibility();
        updateGuildUI();
    }

    private void setupUI() {
        setTitle("QuestPlanner - RPG Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 750);
        setLayout(new BorderLayout());

        topSplitPanel = new JPanel();
        myPanel = new JPanel(new BorderLayout());
        myPanel.setBorder(BorderFactory.createTitledBorder("내 캐릭터"));

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(140, 140));

        JPanel statsPanel = new JPanel(new GridLayout(3, 1));
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        totalCompletedLabel = new JLabel();
        totalCompletedLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        totalCompletedLabel.setForeground(new Color(150, 200, 150));

        expBar = new JProgressBar(0, 100);
        expBar.setStringPainted(true);

        statsPanel.add(statusLabel);
        statsPanel.add(totalCompletedLabel);
        statsPanel.add(expBar);

        myPanel.add(imageLabel, BorderLayout.WEST);
        myPanel.add(statsPanel, BorderLayout.CENTER);

        guildContainer = new JPanel(new BorderLayout());
        guildContainer.setBorder(BorderFactory.createTitledBorder("길드 현황"));

        JPanel guildHeaderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton prevGuildBtn = new JButton("◀");
        guildNameLabel = new JLabel("가입한 길드 없음", SwingConstants.CENTER);
        guildNameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton nextGuildBtn = new JButton("▶");

        prevGuildBtn.addActionListener(e -> cycleGuild(-1));
        nextGuildBtn.addActionListener(e -> cycleGuild(1));

        guildHeaderPanel.add(prevGuildBtn);
        guildHeaderPanel.add(guildNameLabel);
        guildHeaderPanel.add(nextGuildBtn);

        partyPanel = new JPanel();
        partyPanel.setLayout(new BoxLayout(partyPanel, BoxLayout.Y_AXIS));

        JButton leaveGuildBtn = new JButton("길드 탈퇴/해산");
        leaveGuildBtn.addActionListener(e -> handleLeaveOrDisband());

        guildContainer.add(guildHeaderPanel, BorderLayout.NORTH);
        guildContainer.add(new JScrollPane(partyPanel), BorderLayout.CENTER);
        guildContainer.add(leaveGuildBtn, BorderLayout.SOUTH);

        topSplitPanel.setLayout(new GridLayout(1, 1));
        topSplitPanel.add(myPanel);

        updateStatus();
        add(topSplitPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel centerTopPanel = new JPanel(new BorderLayout());

        JPanel dateNavPanel = new JPanel(new FlowLayout());
        JButton btnPrev = new JButton("◀");
        dateLabel = new JLabel(currentDate.toString());
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        JButton btnNext = new JButton("▶");
        JButton btnCalendar = new JButton("📅");

        btnPrev.addActionListener(e -> { currentDate = currentDate.minusDays(1); renderQuests(); });
        btnNext.addActionListener(e -> { currentDate = currentDate.plusDays(1); renderQuests(); });
        btnCalendar.addActionListener(e -> {
            DatePickerDialog dp = new DatePickerDialog(this, currentDate, quests);
            dp.setVisible(true);
            if (dp.getSelectedDate() != null) {
                currentDate = dp.getSelectedDate();
                renderQuests();
            }
        });

        dateNavPanel.add(btnPrev);
        dateNavPanel.add(dateLabel);
        dateNavPanel.add(btnNext);
        dateNavPanel.add(btnCalendar);

        JPanel addQuestPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        btnRebirth = new JButton("✨ 새로운 알 받기");
        btnRebirth.setBackground(new Color(255, 215, 0));
        btnRebirth.setForeground(Color.BLACK);
        btnRebirth.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRebirth.setVisible(false);
        btnRebirth.addActionListener(e -> checkAndProcessRebirth(0));

        JButton btnAddQuest = new JButton("➕ 할 일 추가");
        btnAddQuest.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnAddQuest.setBackground(new Color(50, 120, 70));
        btnAddQuest.setForeground(Color.WHITE);
        btnAddQuest.addActionListener(e -> addQuest());

        addQuestPanel.add(btnRebirth);
        addQuestPanel.add(btnAddQuest);

        centerTopPanel.add(dateNavPanel, BorderLayout.NORTH);
        centerTopPanel.add(addQuestPanel, BorderLayout.SOUTH);
        centerPanel.add(centerTopPanel, BorderLayout.NORTH);

        JPanel questContainer = new JPanel(new GridLayout(2, 1));

        todoPanel = new JPanel();
        todoPanel.setLayout(new BoxLayout(todoPanel, BoxLayout.Y_AXIS));
        JScrollPane todoScroll = new JScrollPane(todoPanel);
        todoScroll.setBorder(BorderFactory.createTitledBorder("🔥 할 일 (우클릭: 수정/삭제)"));

        completedPanel = new JPanel();
        completedPanel.setLayout(new BoxLayout(completedPanel, BoxLayout.Y_AXIS));
        JScrollPane compScroll = new JScrollPane(completedPanel);
        compScroll.setBorder(BorderFactory.createTitledBorder("✅ 완료된 퀘스트"));

        questContainer.add(todoScroll);
        questContainer.add(compScroll);
        centerPanel.add(questContainer, BorderLayout.CENTER);

        summaryLabel = new JLabel("🏆 완료 퀘스트 - 오늘: 0개 | 이번 주: 0개 | 이번 달: 0개", SwingConstants.CENTER);
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        summaryLabel.setForeground(new Color(150, 200, 150));

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);

        centerPanel.add(summaryPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JButton menuButton = new JButton("플래너 메뉴 ☰");
        menuButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        menuButton.setPreferredSize(new Dimension(0, 50));
        menuButton.addActionListener(e -> showMenu());
        add(menuButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    private void handleLeaveOrDisband() {
        if (roomCodesList.isEmpty()) return;
        String code = roomCodesList.get(currentRoomIndex);
        String roomName = player.getJoinedRooms().get(code).replace(" (방장)", "");

        if (isHost(code)) {
            String[] options = {"길드 해산(영구 삭제)", "길드장 양도 후 탈퇴", "취소"};
            int choice = JOptionPane.showOptionDialog(this,
                    "[" + roomName + "] 길드의 길드장입니다. 어떻게 하시겠습니까?",
                    "길드장 권한 관리", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);

            if (choice == 0) {
                if (JOptionPane.showConfirmDialog(this, "정말 해산하시겠습니까? 모든 데이터가 사라집니다.", "해산 최종 확인", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    activeClients.get(code).sendStatus(code, player, "DISBAND", true);
                    removeGuildLocal(code);
                }
            } else if (choice == 1) {
                Map<String, GameData> members = multiRoomMembers.get(code);

                List<String> otherMembers = new ArrayList<>();
                if (members != null) {
                    for (String name : members.keySet()) {
                        if (!name.equals(player.getNickname())) {
                            otherMembers.add(name);
                        }
                    }
                }

                if (otherMembers.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "양도할 다른 길드원이 없습니다. 해산만 가능합니다.", "오류", JOptionPane.ERROR_MESSAGE);
                } else {
                    String[] memberArray = otherMembers.toArray(new String[0]);
                    String selectedMember = (String) JOptionPane.showInputDialog(
                            this,
                            "길드장을 양도할 길드원을 선택하세요:\n(선택 후 방을 완전히 탈퇴하게 됩니다)",
                            "길드장 양도 및 탈퇴",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            memberArray,
                            memberArray[0]);

                    if (selectedMember != null) {
                        activeClients.get(code).sendStatus(code, player, "TRANSFER:" + selectedMember, false);
                        activeClients.get(code).sendStatus(code, player, "LEAVE", false);
                        removeGuildLocal(code);
                        JOptionPane.showMessageDialog(this, "[" + selectedMember + "]님에게 길드장을 위임하고 탈퇴했습니다.");
                    }
                }
            }
        } else {
            if (JOptionPane.showConfirmDialog(this, "[" + roomName + "] 길드에서 탈퇴하시겠습니까?", "탈퇴 확인", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                activeClients.get(code).sendStatus(code, player, "LEAVE", false);
                removeGuildLocal(code);
            }
        }
    }

    private void updateGuildVisibility() {
        topSplitPanel.removeAll();
        if (roomCodesList.isEmpty()) {
            topSplitPanel.setLayout(new GridLayout(1, 1));
            topSplitPanel.add(myPanel);
        } else {
            topSplitPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 1.0;
            gbc.weightx = 0.65; gbc.gridx = 0; topSplitPanel.add(myPanel, gbc);
            gbc.weightx = 0.35; gbc.gridx = 1; topSplitPanel.add(guildContainer, gbc);
        }
        topSplitPanel.revalidate();
        topSplitPanel.repaint();
    }

    private void updateStatus() {
        int level = player.getLevel();
        String type = player.getCharacterType();
        String emoji = "🥚";
        String imgFileName = "egg.png";
        String statusText = "";

        if ("EGG".equals(type) || level < 15) {
            if (level >= 10) { emoji = "🥚"; imgFileName = "egg_crack_2.png"; statusText = "알 (많이 금 감)"; }
            else if (level >= 5) { emoji = "🥚"; imgFileName = "egg_crack_1.png"; statusText = "알 (살짝 금 감)"; }
            else { emoji = "🥚"; imgFileName = "egg.png"; statusText = "알"; }
        } else {
            statusText = type;
            int stage = 1;
            if (level >= 50) stage = 4;
            else if (level >= 40) stage = 3;
            else if (level >= 25) stage = 2;
            else stage = 1;

            switch (type) {
                case "DRAGON":
                    if (stage == 4) { emoji = "🐉"; imgFileName = "dragon_stage4.png"; statusText += " (최종형)"; }
                    else if (stage == 3) { emoji = "🐊"; imgFileName = "dragon_stage3.png"; statusText += " (청년기)"; }
                    else if (stage == 2) { emoji = "🐍"; imgFileName = "dragon_stage2.png"; statusText += " (유년기)"; }
                    else { emoji = "🐣"; imgFileName = "dragon_stage1.png"; statusText += " (탄생)"; }
                    break;
                case "EAGLE":
                    if (stage == 4) { emoji = "🦅"; imgFileName = "eagle_stage4.png"; statusText += " (최종형)"; }
                    else if (stage == 3) { emoji = "🐦"; imgFileName = "eagle_stage3.png"; statusText += " (청년기)"; }
                    else if (stage == 2) { emoji = "🐤"; imgFileName = "eagle_stage2.png"; statusText += " (유년기)"; }
                    else { emoji = "🐣"; imgFileName = "dragon_stage1.png"; statusText += " (탄생)"; }
                    break;
                case "WOLF":
                    if (stage == 4) { emoji = "🐺"; imgFileName = "wolf_stage4.png"; statusText += " (최종형)"; }
                    else if (stage == 3) { emoji = "🐕"; imgFileName = "wolf_stage3.png"; statusText += " (청년기)"; }
                    else if (stage == 2) { emoji = "🐶"; imgFileName = "wolf_stage2.png"; statusText += " (유년기)"; }
                    else { emoji = "🐾"; imgFileName = "wolf_stage1.png"; statusText += " (탄생)"; }
                    break;
                case "LION":
                    if (stage == 4) { emoji = "🦁"; imgFileName = "lion_stage4.png"; statusText += " (최종형)"; }
                    else if (stage == 3) { emoji = "🐅"; imgFileName = "lion_stage3.png"; statusText += " (청년기)"; }
                    else if (stage == 2) { emoji = "🐯"; imgFileName = "lion_stage2.png"; statusText += " (유년기)"; }
                    else { emoji = "🐱"; imgFileName = "lion_stage1.png"; statusText += " (탄생)"; }
                    break;
                case "WATER":
                    if (stage == 4) { emoji = "🧜"; imgFileName = "water_stage4.png"; statusText += " (최종형)"; }
                    else if (stage == 3) { emoji = "🌊"; imgFileName = "water_stage3.png"; statusText += " (청년기)"; }
                    else if (stage == 2) { emoji = "💦"; imgFileName = "water_stage2.png"; statusText += " (유년기)"; }
                    else { emoji = "💧"; imgFileName = "water_stage1.png"; statusText += " (탄생)"; }
                    break;
            }
        }

        String rebirthText = player.getRebirthCount() > 0 ? "<br><font color='#FFD700' size='3'>✨ 환생 " + player.getRebirthCount() + "회차</font>" : "";
        statusLabel.setText("<html>LV." + level + " " + player.getNickname() + " [" + statusText + "]" + rebirthText + "</html>");

        long totalCompleted = quests.stream().filter(Quest::isCompleted).count();
        totalCompletedLabel.setText("🏆 총 달성 퀘스트: " + totalCompleted + "개");

        if (player.getLevel() >= 50) {
            expBar.setValue(0);
            expBar.setString("MAX LEVEL (진화 성장의 끝)");
        } else {
            expBar.setValue((int) player.getExpPercentage());
            expBar.setString(String.format("%d / %d (%.1f%%)", player.getCurrentExp(), player.getRequiredExp(), player.getExpPercentage()));
        }

        File imgFile = new File("images/" + imgFileName);
        if (imgFile.exists()) {
            ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
            Image scaled = icon.getImage().getScaledInstance(125, 125, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
            imageLabel.setText("");
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText(emoji);
            imageLabel.setFont(new Font("SansSerif", Font.PLAIN, 85));
        }

        if (btnRebirth != null) btnRebirth.setVisible(player.getLevel() >= 50 && !"EGG".equals(player.getCharacterType()));
    }

    private void updateStatsSummary() {
        LocalDate today = LocalDate.now();
        int dailyCount = 0; int weeklyCount = 0; int monthlyCount = 0;
        int dayOfWeekValue = today.getDayOfWeek().getValue();
        LocalDate startOfWeek = today.minusDays(dayOfWeekValue - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        for (Quest q : quests) {
            if (q.isCompleted()) {
                try {
                    LocalDate qDate = LocalDate.parse(q.getDate());
                    if (qDate.equals(today)) dailyCount++;
                    if (!qDate.isBefore(startOfWeek) && !qDate.isAfter(endOfWeek)) weeklyCount++;
                    if (qDate.getYear() == today.getYear() && qDate.getMonth() == today.getMonth()) monthlyCount++;
                } catch (Exception e) {}
            }
        }
        summaryLabel.setText(String.format("🏆 완료 퀘스트 - 오늘: %d개 | 이번 주: %d개 | 이번 달: %d개", dailyCount, weeklyCount, monthlyCount));
    }

    private void showMenu() {
        JDialog menuDialog = new JDialog(this, "플래너 메뉴", true);
        menuDialog.setLayout(new BorderLayout());

        JLabel msgLabel = new JLabel("수행할 동작을 선택하세요.", SwingConstants.CENTER);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        msgLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        menuDialog.add(msgLabel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton btn1 = new JButton("할 일 반복 추가");
        JButton btn2 = new JButton("캐릭터 도감 보기");
        JButton btn3 = new JButton("길드 참가/생성");
        JButton btn4 = new JButton("로그아웃");
        JButton btn5 = new JButton("계정 삭제");
        JButton btn6 = new JButton("닫기");

        btn1.addActionListener(e -> { menuDialog.dispose(); addRepeatQuest(); });
        btn2.addActionListener(e -> { menuDialog.dispose(); showCollection(); });
        btn3.addActionListener(e -> { menuDialog.dispose(); showJoinDialog(); });
        btn4.addActionListener(e -> { menuDialog.dispose(); logout(); });
        btn5.addActionListener(e -> { menuDialog.dispose(); deleteAccount(); });
        btn6.addActionListener(e -> menuDialog.dispose());

        btnPanel.add(btn1); btnPanel.add(btn2); btnPanel.add(btn3);
        btnPanel.add(btn4); btnPanel.add(btn5); btnPanel.add(btn6);

        menuDialog.add(btnPanel, BorderLayout.CENTER);
        menuDialog.pack();
        menuDialog.setLocationRelativeTo(this);
        menuDialog.setVisible(true);
    }

    private void logout() {
        if (JOptionPane.showConfirmDialog(this, "정말 로그아웃 하시겠습니까?", "로그아웃", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            for (SocialClient client : activeClients.values()) {
                client.disconnect();
            }
            activeClients.clear();
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        }
    }

    private void deleteAccount() {
        if (JOptionPane.showConfirmDialog(this, "정말 계정을 삭제하시겠습니까?", "⚠️ 계정 삭제 경고", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
            List<String> codesToProcess = new ArrayList<>(roomCodesList);
            for (String code : codesToProcess) {
                if (isHost(code)) {
                    activeClients.get(code).sendStatus(code, player, "DISBAND", true);
                } else {
                    activeClients.get(code).sendStatus(code, player, "LEAVE", false);
                }
            }
            for (SocialClient client : activeClients.values()) {
                client.disconnect();
            }
            activeClients.clear();
            new File(SAVE_FILE).delete();
            JOptionPane.showMessageDialog(this, "계정이 삭제되었습니다.");
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        }
    }

    private void showCollection() {
        new CollectionDialog(this, player.getUnlockedCollection()).setVisible(true);
    }

    private void addQuest() {
        while (true) {
            JTextField titleField = new JTextField(15);
            JTextField pointField = new JTextField("20", 5);
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("할 일 내용:")); panel.add(titleField);
            panel.add(new JLabel("획득 포인트:")); panel.add(pointField);

            int result = JOptionPane.showConfirmDialog(this, panel, "[" + currentDate.toString() + "] 계획 추가", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "내용을 입력해주세요!", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                int point = 20;
                try {
                    point = Integer.parseInt(pointField.getText().trim());
                } catch (Exception e) {}

                quests.add(new Quest(title, currentDate.toString(), point));
                saveGameData(); renderQuests();

                checkTriggerEggOption();
                break;
            } else break;
        }
    }

    private void addRepeatQuest() {
        while (true) {
            JTextField titleField = new JTextField(15);
            JTextField pointField = new JTextField("20", 5);
            JTextField daysField = new JTextField("7", 5);
            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            panel.add(new JLabel("할 일 내용:")); panel.add(titleField);
            panel.add(new JLabel("획득 포인트:")); panel.add(pointField);
            panel.add(new JLabel("반복 일수:")); panel.add(daysField);

            int result = JOptionPane.showConfirmDialog(this, panel, "할 일 반복 추가", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "내용을 입력해주세요!", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                int point = 20; int days = 7;
                try {
                    point = Integer.parseInt(pointField.getText().trim());
                    days = Integer.parseInt(daysField.getText().trim());
                } catch (Exception e) {}

                for (int i = 0; i < days; i++) {
                    quests.add(new Quest(title, currentDate.plusDays(i).toString(), point));
                }
                saveGameData(); renderQuests();

                checkTriggerEggOption();
                break;
            } else break;
        }
    }

    private void checkTriggerEggOption() {
        if (player.getLevel() >= 50 && !"EGG".equals(player.getCharacterType())) {
            int select = JOptionPane.showConfirmDialog(this,
                    "🎉 현재 최종 진화형(만렙) 단계입니다!\n새로운 알을 분양받아 다음 환생 회차를 시작하시겠습니까?",
                    "신규 동반자 가이드", JOptionPane.YES_NO_OPTION);

            if (select == JOptionPane.YES_OPTION) {
                String[] types = {"DRAGON", "EAGLE", "WOLF", "LION", "WATER"};
                String randomType = types[new Random().nextInt(types.length)];

                checkAndProcessRebirthCustom(randomType);
            }
        }
    }

    private void checkAndProcessRebirthCustom(String targetRandomType) {
        player.setLevel(1);
        player.setCurrentExp(0);
        player.setCharacterType("EGG");
        player.setRebirthCount(player.getRebirthCount() + 1);

        player.getUnlockedCollection().add("EGG");
        player.setCharacterType(targetRandomType);

        updateAndSync();
        renderQuests();
        showEventDialog("알림", "새로운 알을 받았습니다.");
    }

    private void editQuest(Quest q) {
        while (true) {
            JTextField titleField = new JTextField(q.getTitle(), 15);
            JTextField pointField = new JTextField(String.valueOf(q.getPoint()), 5);
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("할 일 내용:")); panel.add(titleField);
            panel.add(new JLabel("획득 포인트:")); panel.add(pointField);

            int result = JOptionPane.showConfirmDialog(this, panel, "할 일 수정", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String title = titleField.getText().trim();
                if (title.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "내용을 입력해주세요!", "입력 오류", JOptionPane.WARNING_MESSAGE);
                    continue;
                }
                q.setTitle(title);
                try { q.setPoint(Integer.parseInt(pointField.getText().trim())); } catch (Exception e) {}
                saveGameData(); renderQuests();
                break;
            } else break;
        }
    }

    private JPopupMenu createQuestMenu(Quest q) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("✏️ 수정");
        JMenuItem deleteItem = new JMenuItem("❌ 삭제");

        editItem.addActionListener(e -> editQuest(q));
        deleteItem.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "정말 이 할 일을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                quests.remove(q);
                saveGameData();
                renderQuests();
            }
        });

        menu.add(editItem);
        menu.add(deleteItem);
        return menu;
    }

    private void cycleGuild(int direction) {
        if (roomCodesList.isEmpty()) return;
        currentRoomIndex = (currentRoomIndex + direction + roomCodesList.size()) % roomCodesList.size();
        updateGuildUI();
    }

    private int connectToRoom(String code, String roomName, String mode, boolean isHostMode) {
        SocialClient client = new SocialClient();
        java.util.function.Consumer<GameData> onData = data -> SwingUtilities.invokeLater(() -> {
            String cmd = data.getCommand();
            String sender = data.getSenderNickname();

            if (player.getNickname().equals(sender) && data.isHost()) {
                String currentRoomName = player.getJoinedRooms().get(code);
                if (currentRoomName != null && !currentRoomName.endsWith(" (방장)")) {
                    String pureRoomName = currentRoomName.replace(" (방장)", "");

                    player.getJoinedRooms().put(code, pureRoomName + " (방장)");
                    saveGameData();

                    if (activeClients.containsKey(code)) {
                        activeClients.get(code).sendStatus(code, player, "UPDATE", true);
                    }

                    new Thread(() -> {
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "🎉 오프라인 기간 중 [" + pureRoomName + "] 길드의 길드장으로 임명되었습니다!\n(이전 길드장의 양도)",
                                "길드장 위임 안내", JOptionPane.INFORMATION_MESSAGE);
                    }).start();
                }
            }

            if (cmd != null && cmd.startsWith("TRANSFER:")) {
                String newHost = cmd.split(":")[1];
                String currentRoomName = player.getJoinedRooms().get(code);
                String pureRoomName = currentRoomName != null ? currentRoomName.replace(" (방장)", "") : "길드";

                if (newHost.equals(player.getNickname())) {
                    if (currentRoomName != null && !currentRoomName.endsWith(" (방장)")) {
                        player.getJoinedRooms().put(code, pureRoomName + " (방장)");
                        saveGameData();

                        if (activeClients.containsKey(code)) {
                            activeClients.get(code).sendStatus(code, player, "UPDATE", true);
                        }

                        new Thread(() -> {
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "🎉 방금 [" + pureRoomName + "] 길드의 길드장을 위임받았습니다!",
                                    "길드장 위임 안내", JOptionPane.INFORMATION_MESSAGE);
                        }).start();
                    }
                } else {
                    if (multiRoomMembers.containsKey(code) && multiRoomMembers.get(code).containsKey(newHost)) {
                        multiRoomMembers.get(code).get(newHost).setHost(true);
                    }
                }
                updateGuildUI();
                return;
            }

            if ("JOIN".equals(cmd) || "UPDATE".equals(cmd)) {
                multiRoomMembers.computeIfAbsent(code, k -> new HashMap<>()).put(sender, data);
                updateGuildUI();
            } else if ("LEAVE".equals(cmd)) {
                if (multiRoomMembers.containsKey(code)) multiRoomMembers.get(code).remove(sender);
                updateGuildUI();
            } else if ("DISBAND".equals(cmd)) {
                JOptionPane.showMessageDialog(this, "[" + roomName.replace(" (방장)", "") + "] 길드가 해산되었습니다.", "안내", JOptionPane.WARNING_MESSAGE);
                removeGuildLocal(code);
            }
            if (!roomCodesList.isEmpty() && code.equals(roomCodesList.get(currentRoomIndex))) {
                updateGuildUI();
            }
        });

        int status = client.connect(code, player, onData, mode, isHostMode);
        if (status == 1) activeClients.put(code, client);
        return status;
    }

    private void showJoinDialog() {
        JoinDialog dialog = new JoinDialog(this);
        dialog.setVisible(true);
    }

    public boolean processJoin(String roomName, String password, boolean isCreate) {
        String code = password.isEmpty() ? roomName : roomName + "_[PW:" + password + "]";
        if (player.getJoinedRooms().containsKey(code)) {
            JOptionPane.showMessageDialog(this, "이미 참가 완료된 길드입니다.", "안내", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        int status = connectToRoom(code, roomName, isCreate ? "CREATE" : "JOIN", isCreate);
        if (isCreate) {
            if (status == 0) {
                JOptionPane.showMessageDialog(this, "이미 존재하는 방 이름입니다.", "생성 오류", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if (status == -1) {
                JOptionPane.showMessageDialog(this, "길드 서버(SocialServer)가 켜져 있지 않습니다. 연결할 수 없습니다.", "서버 연결 실패", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            roomName += " (방장)";
            addGuildLocal(code, roomName);
            return true;
        } else {
            if (status == 0) {
                JOptionPane.showMessageDialog(this, "해당 길드 방을 찾을 수 없거나 비밀번호가 틀립니다.", "참가 실패", JOptionPane.ERROR_MESSAGE);
                return false;
            } else if (status == -1) {
                JOptionPane.showMessageDialog(this, "길드 서버(SocialServer)가 켜져 있지 않습니다. 연결할 수 없습니다.", "서버 연결 실패", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            addGuildLocal(code, roomName);
            return true;
        }
    }

    private void addGuildLocal(String code, String roomName) {
        player.getJoinedRooms().put(code, roomName);
        if (!roomCodesList.contains(code)) roomCodesList.add(code);
        currentRoomIndex = roomCodesList.size() - 1;

        updateAndSync();
        updateGuildVisibility();
        updateGuildUI();
    }

    private void removeGuildLocal(String code) {
        player.getJoinedRooms().remove(code);
        roomCodesList.remove(code);
        SocialClient client = activeClients.remove(code);
        if (client != null) client.disconnect();
        multiRoomMembers.remove(code);
        currentRoomIndex = roomCodesList.isEmpty() ? -1 : 0;

        saveGameData();
        updateGuildVisibility();
        updateGuildUI();
    }

    private void leaveCurrentGuild() {
        if (roomCodesList.isEmpty()) return;
        String code = roomCodesList.get(currentRoomIndex);
        if (isHost(code)) {
            activeClients.get(code).sendStatus(code, player, "DISBAND", true);
        } else {
            activeClients.get(code).sendStatus(code, player, "LEAVE", false);
        }
        removeGuildLocal(code);
    }

    private void updateGuildUI() {
        if (roomCodesList.isEmpty()) {
            guildNameLabel.setText("가입한 길드 없음");
            partyPanel.removeAll();
            partyPanel.revalidate();
            partyPanel.repaint();
            return;
        }
        String currentCode = roomCodesList.get(currentRoomIndex);
        guildNameLabel.setText(player.getJoinedRooms().get(currentCode).replace(" (방장)", ""));
        partyPanel.removeAll();

        Map<String, GameData> currentMembers = new HashMap<>(multiRoomMembers.getOrDefault(currentCode, new HashMap<>()));
        currentMembers.remove(player.getNickname());

        List<GameData> sortedMembers = new ArrayList<>(currentMembers.values());
        GameData me = new GameData();
        me.setSenderNickname(player.getNickname());
        me.setLevel(player.getLevel());
        me.setExpPercentage(player.getExpPercentage());
        me.setHost(isHost(currentCode));
        me.setRebirthCount(player.getRebirthCount());
        sortedMembers.add(me);

        sortedMembers.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));

        for (GameData member : sortedMembers) {
            String hostTag = member.isHost() ? " 👑" : "";
            JLabel lbl = new JLabel("[" + member.getRebirthCount() + "회차] Lv." + member.getLevel() + " " + member.getSenderNickname() + " (" + String.format("%.1f", member.getExpPercentage()) + "%)" + hostTag);
            partyPanel.add(lbl);
        }
        partyPanel.revalidate();
        partyPanel.repaint();
    }

    private void renderQuests() {
        dateLabel.setText(currentDate.toString());
        todoPanel.removeAll();
        completedPanel.removeAll();
        for (Quest q : quests) {
            if (q.getDate().equals(currentDate.toString())) {
                JCheckBox checkBox = new JCheckBox(q.getTitle() + " (" + q.getPoint() + " EXP)");
                checkBox.setSelected(q.isCompleted());

                checkBox.setComponentPopupMenu(createQuestMenu(q));

                checkBox.addActionListener(e -> {
                    if (checkBox.isSelected()) {
                        q.setCompleted(true);
                        handleExpGain(q.getPoint());
                    } else {
                        q.setCompleted(false);
                        handleExpLoss(q.getPoint());
                    }
                    renderQuests();
                });
                if (q.isCompleted()) completedPanel.add(checkBox);
                else todoPanel.add(checkBox);
            }
        }
        updateStatsSummary();
        todoPanel.revalidate();
        todoPanel.repaint();
        completedPanel.revalidate();
        completedPanel.repaint();
    }

    private void handleExpGain(int amount) {
        int oldLevel = player.getLevel();
        player.addExp(amount);
        if (player.getLevel() > oldLevel) checkHatchingOrGrowth(oldLevel);
        updateAndSync();
    }

    private void handleExpLoss(int amount) {
        player.removeExp(amount);
        updateAndSync();
    }

    private boolean checkAndProcessRebirth(int pendingExp) {
        if (JOptionPane.showConfirmDialog(this, "환생하시겠습니까?", "환생", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            player.setLevel(1);
            player.setCurrentExp(0);
            player.setCharacterType("EGG");
            player.setRebirthCount(player.getRebirthCount() + 1);
            player.getUnlockedCollection().add("EGG");
            updateAndSync();
            renderQuests();
            showEventDialog("알림", "새로운 알을 받았습니다.");
            return true;
        }
        return false;
    }

    private void checkHatchingOrGrowth(int oldLevel) {
        int newLevel = player.getLevel();
        String type = player.getCharacterType();

        if (oldLevel < 15 && newLevel >= 15) {
            if ("EGG".equals(type)) {
                String[] types = {"DRAGON", "EAGLE", "WOLF", "LION", "WATER"};
                player.setCharacterType(types[new Random().nextInt(types.length)]);
            }
            player.getUnlockedCollection().add(player.getCharacterType() + "_1");
            showEventDialog("부화 완료!", player.getCharacterType() + " 탄생!");
        }

        if (!"EGG".equals(player.getCharacterType())) {
            if (oldLevel < 25 && newLevel >= 25) {
                player.getUnlockedCollection().add(player.getCharacterType() + "_1");
                player.getUnlockedCollection().add(player.getCharacterType() + "_2");
                showEventDialog("진화!", "당신의 [" + player.getCharacterType() + "](이)가 유년기로 성장했습니다!");
            }
            if (oldLevel < 40 && newLevel >= 40) {
                player.getUnlockedCollection().add(player.getCharacterType() + "_1");
                player.getUnlockedCollection().add(player.getCharacterType() + "_2");
                player.getUnlockedCollection().add(player.getCharacterType() + "_3");
                showEventDialog("진화!", "당신의 [" + player.getCharacterType() + "](이)가 늠름한 청년기로 성장했습니다!");
            }
            if (oldLevel < 50 && newLevel >= 50) {
                player.getUnlockedCollection().add(player.getCharacterType() + "_1");
                player.getUnlockedCollection().add(player.getCharacterType() + "_2");
                player.getUnlockedCollection().add(player.getCharacterType() + "_3");
                player.getUnlockedCollection().add(player.getCharacterType() + "_4");
                showEventDialog("각성!", "당신의 [" + player.getCharacterType() + "](이)가 완벽한 최종형에 마주했습니다!");
            }
        }
    }

    private void showEventDialog(String title, String message) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 26));
        label.setForeground(new Color(255, 215, 0));
        panel.add(label, BorderLayout.CENTER);
        panel.add(new JLabel(message, SwingConstants.CENTER), BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, "모험의 이정표", JOptionPane.PLAIN_MESSAGE);
    }

    private void updateAndSync() {
        updateStatus();
        updateGuildUI();
        saveGameData();
        for (Map.Entry<String, SocialClient> entry : activeClients.entrySet()) {
            entry.getValue().sendStatus(entry.getKey(), player, "UPDATE", isHost(entry.getKey()));
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();

        new Thread(() -> {
            try {
                org.example.network.SocialServer.main(new String[0]);
            } catch (Exception e) {
                System.out.println("로컬 서버 연동 자동 백그라운드 대기 (이미 켜져 있을 수 있음): " + e.getMessage());
            }
        }).start();

        try { Thread.sleep(300); } catch (InterruptedException e) {}

        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}