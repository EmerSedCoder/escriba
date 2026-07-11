package ui;

import model.Timeline;
import model.TimelineDate;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Visual timeline editor showing dates as a horizontal number line. */
public final class TimelinePanel extends JPanel {
    private final JPanel timelineCanvas = new JPanel();
    private Timeline timeline;
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

    public void showTimeline(Timeline timeline) {
        this.timeline = timeline;
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
                g2d.drawLine(50, 75, 1950, 75);

                for (int i = 0; i <= 20; i++) {
                    int x = 50 + i * 95;
                    g2d.drawLine(x, 70, x, 80);
                }
            }
        };
        axis.setBounds(0, 0, 2000, 150);
        axis.setOpaque(false);
        timelineCanvas.add(axis);
    }

    private void drawDates() {
        if (timeline == null || timeline.getDates().isEmpty()) return;

        int minPos = timeline.getDates().stream().mapToInt(TimelineDate::getPosition).min().orElse(0);
        int maxPos = timeline.getDates().stream().mapToInt(TimelineDate::getPosition).max().orElse(10);
        int range = Math.max(1, maxPos - minPos);

        for (TimelineDate date : timeline.getDates()) {
            int relativePos = (date.getPosition() - minPos) * 1900 / range;
            int x = 50 + relativePos;

            JPanel dateBox = createDateBox(date, x);
            timelineCanvas.add(dateBox);
        }
    }

    private JPanel createDateBox(TimelineDate date, int x) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new LineBorder(Color.BLUE, 1));
        box.setBackground(new Color(220, 240, 255));

        JLabel label = new JLabel(date.getName());
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);

        box.setBounds(x - 40, date.getPosition() % 2 == 0 ? 10 : 100, 80, 35);
        return box;
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
