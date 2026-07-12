package ui;

import model.Book;
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
    private final JTextArea linkedCharacters = new JTextArea();
    private List<Goal> goals;
    private Goal selected;
    private Book book;

    public GoalPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton("+ Goal");
        add.addActionListener(e -> addGoal());
        JButton delete = new JButton("Delete Goal");
        delete.addActionListener(e -> deleteGoal());
        buttonPanel.add(add);
        buttonPanel.add(delete);
        add(buttonPanel, BorderLayout.NORTH);
        list.addListSelectionListener(this::selectGoal);
        
        linkedCharacters.setEditable(false);
        linkedCharacters.setLineWrap(true);
        linkedCharacters.setWrapStyleWord(true);

        JPanel fields = new JPanel(new GridLayout(3, 1, 0, 6));
        fields.add(labeled("Outcome", outcome));
        fields.add(labeled("Conflict", conflict));
        fields.add(labeled("Linked Characters", linkedCharacters));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(list), fields);
        split.setResizeWeight(.35);
        add(split, BorderLayout.CENTER);
    }

    public void showEntries(List<Goal> goals) {
        saveSelected();
        this.goals = goals;
        listModel.clear(); goals.forEach(listModel::addElement);
        selected = null; outcome.setText(""); conflict.setText(""); linkedCharacters.setText("");
    }

    public void setBook(Book book) {
        this.book = book;
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
    private void deleteGoal() {
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a goal to delete", "Delete Goal", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete goal '" + selected.getTitle() + "'?\nThis will also clear this goal from all characters.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            goals.remove(selected);
            listModel.removeElement(selected);
            if (book != null) {
                for (model.Character character : book.getCharacters()) {
                    if (selected.getTitle().equals(character.getGoalTitle())) {
                        character.setGoalTitle("");
                    }
                }
            }
            selected = null;
            outcome.setText("");
            conflict.setText("");
            refreshLinkedCharacters();
        }
    }
    private void selectGoal(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        saveSelected(); selected = list.getSelectedValue();
        outcome.setText(selected == null ? "" : selected.getOutcome());
        conflict.setText(selected == null ? "" : selected.getConflict());
        refreshLinkedCharacters();
    }
    public void refreshLinkedCharacters() {
        if (selected == null || book == null) {
            linkedCharacters.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        book.getCharacters().stream()
                .filter(character -> selected.getTitle().equals(character.getGoalTitle()))
                .forEach(character -> sb.append("• ").append(character.getTitle()).append("\n"));
        if (sb.length() == 0) {
            linkedCharacters.setText("No characters linked to this goal.");
        } else {
            linkedCharacters.setText(sb.toString());
        }
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
