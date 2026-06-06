package org.example.ui;

import org.example.model.Quest;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;


public class DatePickerDialog extends JDialog {
    private LocalDate selectedDate;
    private YearMonth currentYearMonth;
    private JPanel daysPanel;
    private JLabel monthLabel;
    private List<Quest> quests; // ✨ 퀘스트 목록을 받아옵니다

    public DatePickerDialog(JFrame parent, LocalDate initialDate, List<Quest> quests) {
        super(parent, "날짜 선택", true);
        this.selectedDate = initialDate;
        this.currentYearMonth = YearMonth.from(initialDate);
        this.quests = quests;

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        JButton prevBtn = new JButton("◀");
        JButton nextBtn = new JButton("▶");
        monthLabel = new JLabel();
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        prevBtn.addActionListener(e -> { currentYearMonth = currentYearMonth.minusMonths(1); updateCalendar(); });
        nextBtn.addActionListener(e -> { currentYearMonth = currentYearMonth.plusMonths(1); updateCalendar(); });

        topPanel.add(prevBtn);
        topPanel.add(monthLabel);
        topPanel.add(nextBtn);
        add(topPanel, BorderLayout.NORTH);

        daysPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        daysPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(daysPanel, BorderLayout.CENTER);

        updateCalendar();

        setSize(500, 450);
        setLocationRelativeTo(parent);
    }

    private void updateCalendar() {
        daysPanel.removeAll();
        monthLabel.setText(currentYearMonth.getYear() + "년 " + currentYearMonth.getMonthValue() + "월");

        String[] daysOfWeek = {"일", "월", "화", "수", "목", "금", "토"};
        for (String day : daysOfWeek) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setForeground(Color.GRAY);
            daysPanel.add(lbl);
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < dayOfWeekValue; i++) {
            daysPanel.add(new JLabel(""));
        }

        for (int i = 1; i <= currentYearMonth.lengthOfMonth(); i++) {
            LocalDate date = currentYearMonth.atDay(i);

            long totalCount = quests.stream().filter(q -> q.getDate().equals(date.toString())).count();
            long completedCount = quests.stream().filter(q -> q.getDate().equals(date.toString()) && q.isCompleted()).count();

            JButton dayBtn = new JButton();
            dayBtn.setMargin(new Insets(2, 2, 2, 2));

            if (totalCount > 0) {
                if (totalCount == completedCount) {
                    dayBtn.setText("<html><center>" + i + "<br><font size='2' color='#4CAF50'>✔ 완료 " + completedCount + "</font></center></html>");
                } else {
                    dayBtn.setText("<html><center>" + i + "<br><font size='2' color='#FF9800'>진행 " + completedCount + "/" + totalCount + "</font></center></html>");
                }
            } else {
                dayBtn.setText(String.valueOf(i));
            }

            if (date.equals(LocalDate.now())) {
                dayBtn.setBackground(new Color(60, 100, 160));
            }

            dayBtn.addActionListener(e -> {
                selectedDate = date;
                dispose();
            });
            daysPanel.add(dayBtn);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    public LocalDate getSelectedDate() { return selectedDate; }
}