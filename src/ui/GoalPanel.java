package ui;

import model.Goal;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.List;

/** Goal editor with distinct outcome and conflict fields. */
public final class GoalPanel extends JPanel {
    private final DefaultListModel<Goal> listModel = new DefaultListModel<>();
    private final JList<Goal> list = new JList<>(listModel);
    private final JTextArea outcome = textArea();
    private final JTextArea conflict = textArea();
    private List<Goal> goals;
    private Goal selected;

    public GoalPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton add = new JButton("+ Goal");
        add.addActionListener(e -> addGoal());
        add(add, BorderLayout.NORTH);
        list.addListSelectionListener(this::selectGoal);
        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 6));
        fields.add(labeled("Outcome", outcome));
        fields.add(labeled("Conflict", conflict));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(list), fields);
        split.setResizeWeight(.35);
        add(split, BorderLayout.CENTER);
    }

    public void showEntries(List<Goal> goals) {
        saveSelected();
        this.goals = goals;
        listModel.clear(); goals.forEach(listModel::addElement);
        selected = null; outcome.setText(""); conflict.setText("");
    }
    public void saveChanges() { saveSelected(); }
    private void addGoal() {
        String title = JOptionPane.showInputDialog(this, "Goal name:", "New Goal", JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.isBlank()) return;
        saveSelected();
        Goal goal = new Goal(title.trim());
        goals.add(goal);
        listModel.addElement(goal); list.setSelectedValue(goal, true);
    }
    private void selectGoal(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        saveSelected(); selected = list.getSelectedValue();
        outcome.setText(selected == null ? "" : selected.getOutcome());
        conflict.setText(selected == null ? "" : selected.getConflict());
    }
    private void saveSelected() {
        if (selected != null) { selected.setOutcome(outcome.getText()); selected.setConflict(conflict.getText()); }
    }
    private JTextArea textArea() { JTextArea area = new JTextArea(); area.setLineWrap(true); area.setWrapStyleWord(true); return area; }
    private JPanel labeled(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(new JScrollPane(field), BorderLayout.CENTER);
        return panel;
    }
}
