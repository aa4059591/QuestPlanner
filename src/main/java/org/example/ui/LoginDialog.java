package org.example.ui;

import org.example.util.SecurityUtil;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LoginDialog extends JDialog {
    private JTextField nameField;
    private JPasswordField pwField;
    private String nickname = null;
    private String hashedPassword = null;
    private boolean isNewUser = false;

    public LoginDialog() {
        setTitle("QuestPlanner - 접속");
        setModal(true);
        setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel(new GridLayout(4, 1));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        centerPanel.add(new JLabel("캐릭터 닉네임:", SwingConstants.CENTER));
        nameField = new JTextField();
        nameField.setHorizontalAlignment(JTextField.CENTER);
        centerPanel.add(nameField);

        centerPanel.add(new JLabel("비밀번호:", SwingConstants.CENTER));
        pwField = new JPasswordField();
        pwField.setHorizontalAlignment(JTextField.CENTER);
        centerPanel.add(pwField);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton loginBtn = new JButton("로그인");
        JButton createBtn = new JButton("새로 생성");

        loginBtn.addActionListener(e -> handleLogin(false));
        createBtn.addActionListener(e -> handleLogin(true));

        btnPanel.add(loginBtn);
        btnPanel.add(createBtn);

        add(centerPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setSize(320, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void handleLogin(boolean createNew) {
        String inputName = nameField.getText().trim();
        String inputPw = new String(pwField.getPassword()).trim();

        if (inputName.isEmpty() || inputPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임과 비밀번호를 모두 입력해주세요!");
            return;
        }

        this.nickname = inputName;
        this.hashedPassword = SecurityUtil.hashPassword(inputPw);

        File saveFile = new File(nickname + "_data.json");

        if (createNew) {
            if (saveFile.exists()) {
                JOptionPane.showMessageDialog(this, "이미 존재하는 닉네임입니다. 로그인을 해주세요.");
                this.nickname = null;
                return;
            }
            isNewUser = true;
        } else {
            if (!saveFile.exists()) {
                JOptionPane.showMessageDialog(this, "저장된 데이터가 없습니다. 새로 생성을 눌러주세요.");
                this.nickname = null;
                return;
            }
            isNewUser = false;
        }
        dispose();
    }

    public String getNickname() { return nickname; }
    public String getHashedPassword() { return hashedPassword; }
    public boolean isNewUser() { return isNewUser; }
}