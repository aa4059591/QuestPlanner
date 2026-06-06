package org.example.ui;

import javax.swing.*;
import java.awt.*;

public class JoinDialog extends JDialog {

    public JoinDialog(MainFrame parent) {
        super(parent, "길드 방 참가 / 생성", true);
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 10, 15));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        centerPanel.add(new JLabel("방 이름:"));
        JTextField nameField = new JTextField();
        centerPanel.add(nameField);

        centerPanel.add(new JLabel("비밀번호 (선택):"));
        JPasswordField pwField = new JPasswordField();
        centerPanel.add(pwField);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton createBtn = new JButton("✨ 방 생성");
        JButton joinBtn = new JButton("🚪 방 참가");
        JButton cancelBtn = new JButton("취소");

        createBtn.setBackground(new Color(50, 120, 70));
        createBtn.setForeground(Color.WHITE);

        joinBtn.setBackground(new Color(60, 100, 160));
        joinBtn.setForeground(Color.WHITE);

        createBtn.addActionListener(e -> {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "방 이름을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = parent.processJoin(nameField.getText().trim(), new String(pwField.getPassword()).trim(), true);
            if (success) dispose();
        });

        joinBtn.addActionListener(e -> {
            if (nameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "참가할 방 이름을 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean success = parent.processJoin(nameField.getText().trim(), new String(pwField.getPassword()).trim(), false);
            if (success) dispose();
        });

        cancelBtn.addActionListener(e -> dispose());

        bottomPanel.add(createBtn);
        bottomPanel.add(joinBtn);
        bottomPanel.add(cancelBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        setSize(350, 200);
        setLocationRelativeTo(parent);
    }
}