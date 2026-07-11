package ui;

import model.Book;
import model.Timeline;
import model.TimelineDate;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Visual timeline editor showing dates as a horizontal number line. */
public final class TimelinePanel extends JPanel {
    private final JPanel timelineCanvas = new JPanel();
    private Timeline timeline;
    private Book book;
    private TimelineDate selectedDate;
    private Consumer<TimelineDate> dateAdded = date -> {};

    public TimelinePanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton addDate = new JButton("+ Add Date");
        addDate.addActionListener(e -> addDate());
        add(addDate, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(timelineCanvas);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        timelineCanvas.setLayout(null);
        timelineCanvas.setBackground(Color.WHITE);
        timelineCanvas.setPreferredSize(new Dimension(2000, 150));
    }

    public void showTimeline(Timeline timeline, Book book) {
        this.timeline = timeline;
        this.book = book;
        this.selectedDate = null;
        refreshTimeline();
    }

    public void setDateAddedListener(Consumer<TimelineDate> listener) {
        dateAdded = listener;
    }

    private void refreshTimeline() {
        timelineCanvas.removeAll();
        if (timeline == null) return;

        drawTimelineAxis();
        drawDates();

        timelineCanvas.revalidate();
        timelineCanvas.repaint();
    }

    private void drawTimelineAxis() {
        JPanel axis = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                
                int centerX = 1000;
                g2d.drawLine(50, 75, 1950, 75);
                g2d.drawLine(centerX, 70, centerX, 80);
                
                g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g2d.drawString("0", centerX - 10, 95);
                
                int minPos = timeline == null || timeline.getDates().isEmpty() ? -10 : 
                    timeline.getDates().stream().mapToInt(TimelineDate::getPosition).min().orElse(-10);
                int maxPos = timeline == null || timeline.getDates().isEmpty() ? 10 : 
                    timeline.getDates().stream().mapToInt(TimelineDate::getPosition).max().orElse(10);
                
                int range = Math.max(1, Math.max(Math.abs(minPos), Math.abs(maxPos)));
                int pixelsPerUnit = 80;
                
                for (int i = -range; i <= range; i++) {
                    if (i == 0) continue;
                    int x = centerX + i * pixelsPerUnit;
                    if (x >= 50 && x <= 1950) {
                        g2d.drawLine(x, 70, x, 80);
                        if (i % 5 == 0 || Math.abs(i) <= 2) {
                            g2d.drawString(String.valueOf(i), x - 10, 95);
                        }
                    }
                }
            }
        };
        axis.setBounds(0, 0, 2000, 150);
        axis.setOpaque(false);
        timelineCanvas.add(axis);
    }

    private void drawDates() {
        if (timeline == null || timeline.getDates().isEmpty()) return;

        int centerX = 1000;
        int pixelsPerUnit = 80;

        for (TimelineDate date : timeline.getDates()) {
            int position = date.getPosition();
            int x = centerX + position * pixelsPerUnit;

            JPanel dateBox = createDateBox(date, x, position);
            timelineCanvas.add(dateBox);
        }
    }

    private JPanel createDateBox(TimelineDate date, int x, int position) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        
        boolean isSelected = date == selectedDate;
        box.setBorder(new LineBorder(isSelected ? Color.RED : Color.BLUE, isSelected ? 3 : 1));
        box.setBackground(isSelected ? new Color(255, 200, 200) : new Color(220, 240, 255));
        box.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(date.getName());
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);

        int y = position % 2 == 0 ? 10 : 100;
        box.setBounds(Math.max(10, Math.min(1960, x - 40)), y, 80, 35);
        
        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    showDateContextMenu(date, e.getLocationOnScreen());
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                showTooltip(date, e.getLocationOnScreen());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hideTooltip();
            }
        });
        
        return box;
    }

    private void showDateContextMenu(TimelineDate date, Point location) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem editItem = new JMenuItem("Edit");
        editItem.addActionListener(e -> {
            selectedDate = date;
            editSelectedDate();
        });
        
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            selectedDate = date;
            deleteSelectedDate();
        });
        
        menu.add(editItem);
        menu.add(deleteItem);
        
        menu.show(timelineCanvas, location.x - timelineCanvas.getLocationOnScreen().x, location.y - timelineCanvas.getLocationOnScreen().y + 20);
    }

    private JWindow tooltipWindow;

    private void showTooltip(TimelineDate date, Point location) {
        if (book == null || timeline == null) return;
        
        String scenes = book.getScenes().stream()
            .filter(s -> s.getTimelineName().equals(timeline.getTitle()) && 
                         s.getTimelineDateName().equals(date.getName()))
            .map(s -> "• " + s.getTitle())
            .collect(Collectors.joining("\n"));
        
        if (scenes.isEmpty()) scenes = "(No scenes on this date)";
        
        hideTooltip();
        tooltipWindow = new JWindow();
        JLabel tooltipLabel = new JLabel("<html>" + scenes.replace("\n", "<br>") + "</html>");
        tooltipLabel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        tooltipLabel.setBackground(new Color(255, 255, 220));
        tooltipLabel.setOpaque(true);
        tooltipWindow.add(tooltipLabel);
        tooltipWindow.pack();
        tooltipWindow.setLocation(location.x + 10, location.y + 10);
        tooltipWindow.setVisible(true);
    }

    private void hideTooltip() {
        if (tooltipWindow != null) {
            tooltipWindow.dispose();
            tooltipWindow = null;
        }
    }

    private void editSelectedDate() {
        String newName = JOptionPane.showInputDialog(this, "Date name:", selectedDate.getName(), JOptionPane.PLAIN_MESSAGE);
        if (newName == null) return;
        if (!newName.isBlank()) selectedDate.setName(newName.trim());

        String posStr = JOptionPane.showInputDialog(this, "Position on timeline (number):", String.valueOf(selectedDate.getPosition()), JOptionPane.PLAIN_MESSAGE);
        if (posStr == null) return;
        try {
            selectedDate.setPosition(Integer.parseInt(posStr.trim()));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid position number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        selectedDate = null;
        refreshTimeline();
    }

    private void deleteSelectedDate() {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete '" + selectedDate.getName() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            timeline.getDates().remove(selectedDate);
            selectedDate = null;
            refreshTimeline();
        }
    }

    private void addDate() {
        if (timeline == null) return;

        String name = JOptionPane.showInputDialog(this, "Date name:", "Add Date", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;

        String posStr = JOptionPane.showInputDialog(this, "Position on timeline (number):", "0", JOptionPane.PLAIN_MESSAGE);
        if (posStr == null || posStr.isBlank()) return;

        try {
            int position = Integer.parseInt(posStr.trim());
            TimelineDate timelineDate = new TimelineDate(name.trim(), position);
            timeline.getDates().add(timelineDate);
            dateAdded.accept(timelineDate);
            refreshTimeline();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid position number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
