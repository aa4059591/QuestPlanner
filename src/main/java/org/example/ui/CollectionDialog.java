package org.example.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Set;

public class CollectionDialog extends JDialog {

    private final String[][] DICTIONARY = {
            {"DRAGON_1", "🐣", "DRAGON (탄생)", "dragon_stage1.png"},
            {"DRAGON_2", "🐍", "DRAGON (유년)", "dragon_stage2.png"},
            {"DRAGON_3", "🐊", "DRAGON (청년)", "dragon_stage3.png"},
            {"DRAGON_4", "🐉", "DRAGON (최종)", "dragon_stage4.png"},

            {"EAGLE_1", "🐣", "EAGLE (탄생)", "eagle_stage1.png"},
            {"EAGLE_2", "🐤", "EAGLE (유년)", "eagle_stage2.png"},
            {"EAGLE_3", "🐦", "EAGLE (청년)", "eagle_stage3.png"},
            {"EAGLE_4", "🦅", "EAGLE (최종)", "eagle_stage4.png"},

            {"WOLF_1", "🐾", "WOLF (탄생)", "wolf_stage1.png"},
            {"WOLF_2", "🐶", "WOLF (유년)", "wolf_stage2.png"},
            {"WOLF_3", "🐕", "WOLF (청년)", "wolf_stage3.png"},
            {"WOLF_4", "🐺", "WOLF (최종)", "wolf_stage4.png"},

            {"LION_1", "🐱", "LION (탄생)", "lion_stage1.png"},
            {"LION_2", "🐯", "LION (유년)", "lion_stage2.png"},
            {"LION_3", "🐅", "LION (청년)", "lion_stage3.png"},
            {"LION_4", "🦁", "LION (최종)", "lion_stage4.png"},

            {"WATER_1", "💧", "WATER (탄생)", "water_stage1.png"},
            {"WATER_2", "💦", "WATER (유년)", "water_stage2.png"},
            {"WATER_3", "🌊", "WATER (청년)", "water_stage3.png"},
            {"WATER_4", "🧜", "WATER (최종)", "water_stage4.png"}
    };

    public CollectionDialog(JFrame parent, Set<String> unlockedKeys) {
        super(parent, "내 캐릭터 도감", true);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();

        int unlockedCount = 0;
        for (String key : unlockedKeys) {
            if (key != null && !key.startsWith("EGG")) unlockedCount++;
        }

        JLabel headerLabel = new JLabel("✨ 진화 도감 (" + unlockedCount + " / " + DICTIONARY.length + ") ✨");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(5, 4, 8, 8));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        for (String[] entry : DICTIONARY) {
            String key = entry[0];
            String emoji = entry[1];
            String name = entry[2];
            String imgFileName = entry[3];

            JPanel slot = new JPanel(new BorderLayout());
            slot.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            slot.setBackground(new Color(40, 44, 52));

            JLabel iconLabel = new JLabel();
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));

            JLabel nameLabel = new JLabel();
            nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 10));

            if (unlockedKeys.contains(key)) {
                File imgFile = new File("images/" + imgFileName);
                if (imgFile.exists()) {
                    ImageIcon icon = new ImageIcon(imgFile.getAbsolutePath());
                    Image scaled = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                    iconLabel.setIcon(new ImageIcon(scaled));
                    iconLabel.setText("");
                } else {
                    iconLabel.setIcon(null);
                    iconLabel.setText(emoji);
                }
                nameLabel.setText(name);
                nameLabel.setForeground(Color.WHITE);
            } else {
                iconLabel.setIcon(null);
                iconLabel.setText("❓");
                nameLabel.setText("???");
                nameLabel.setForeground(Color.GRAY);
            }

            slot.add(iconLabel, BorderLayout.CENTER);
            slot.add(nameLabel, BorderLayout.SOUTH);
            gridPanel.add(slot);
        }

        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        setSize(550, 650);
        setLocationRelativeTo(parent);
    }
}