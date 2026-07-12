package ui;

import model.Book;
import model.Character;
import model.Scene;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.function.Consumer;

/** Character profile editor, including appearance and participation history. */
public final class CharacterPanel extends JPanel {
    private final DefaultListModel<Character> listModel = new DefaultListModel<>();
    private final JList<Character> list = new JList<>(listModel);
    private final RichTextPane description = new RichTextPane();
    private final RichTextPane appearance = new RichTextPane();
    private final JComboBox<String> goalDropdown = new JComboBox<>();
    private final JTextArea history = area(false);
    private Book book;
    private Character selected;
    private Consumer<Character> characterViewer = character -> {};
    private boolean isUpdatingDropdown = false;

    public CharacterPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton("+ Character");
        add.addActionListener(e -> addCharacter());
        JButton view = new JButton("View in Editor");
        view.addActionListener(e -> {
            if (selected != null) characterViewer.accept(selected);
        });
        buttonPanel.add(add);
        buttonPanel.add(view);
        add(buttonPanel, BorderLayout.NORTH);
        list.addListSelectionListener(this::selectCharacter);
        
        goalDropdown.addActionListener(e -> {
            if (!isUpdatingDropdown && selected != null) {
                selected.setGoalTitle((String) goalDropdown.getSelectedItem());
            }
        });

        JPanel fields = new JPanel(new GridLayout(4, 1, 0, 6));
        fields.add(labeled("Description", description));
        fields.add(labeled("Appearance", appearance));
        fields.add(labeled("Goal", goalDropdown));
        fields.add(labeled("Scenes & locations", history));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(list), fields);
        split.setResizeWeight(.3); add(split, BorderLayout.CENTER);
    }
    public void showBook(Book book) {
        saveSelected(); this.book = book; listModel.clear(); book.getCharacters().forEach(listModel::addElement);
        selected = null; description.setText(""); appearance.setText(""); history.setText("");
        refreshGoalDropdown();
    }
    public void saveChanges() { saveSelected(); }
    public void setCharacterViewer(Consumer<Character> viewer) { characterViewer = viewer; }
    public void refreshHistory() { if (selected != null) history.setText(participationHistory(selected)); }
    private void addCharacter() {
        String name = JOptionPane.showInputDialog(this, "Character name:", "New Character", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        saveSelected(); Character character = new Character(name.trim()); book.getCharacters().add(character);
        listModel.addElement(character); list.setSelectedValue(character, true);
    }
    private void selectCharacter(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        saveSelected(); selected = list.getSelectedValue();
        description.setText(selected == null ? "" : selected.getContent());
        appearance.setText(selected == null ? "" : selected.getAppearance());
        history.setText(selected == null ? "" : participationHistory(selected));
        refreshGoalDropdown();
    }
    private void saveSelected() {
        if (selected != null) {
            selected.setContent(description.getText());
            selected.setAppearance(appearance.getText());
            selected.setGoalTitle((String) goalDropdown.getSelectedItem());
        }
    }
    private void refreshGoalDropdown() {
        isUpdatingDropdown = true;
        goalDropdown.removeAllItems();
        goalDropdown.addItem("");
        if (book != null) {
            book.getGoals().forEach(goal -> goalDropdown.addItem(goal.getTitle()));
        }
        if (selected != null) {
            goalDropdown.setSelectedItem(selected.getGoalTitle());
        } else {
            goalDropdown.setSelectedIndex(0);
        }
        isUpdatingDropdown = false;
    }
    private String participationHistory(Character character) {
        StringBuilder result = new StringBuilder("Scenes participated:\n");
        LinkedHashSet<String> locations = new LinkedHashSet<>();
        for (Scene scene : book.getScenes()) if (scene.getParticipantNames().contains(character.getTitle()) || mentions(scene.getContent(), character.getTitle())) {
            result.append("• ").append(scene.getTitle());
            result.append('\n');
        }
        book.getLocations().stream()
                .filter(location -> location.getVisitorNames().contains(character.getTitle()))
                .forEach(location -> locations.add(location.getTitle()));
        result.append("\nLocations frequented:\n");
        if (locations.isEmpty()) result.append("No scene locations yet."); else locations.forEach(location -> result.append("• ").append(location).append('\n'));
        return result.toString();
    }
    private boolean mentions(String text, String name) {
        if (text == null) return false;
        String plainText = text.replaceAll("<[^>]*>", "");
        return plainText.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
    }
    private JTextArea area(boolean editable) { JTextArea value = new JTextArea(); value.setEditable(editable); value.setLineWrap(true); value.setWrapStyleWord(true); return value; }
    private JPanel labeled(String label, JComponent component) { JPanel panel = new JPanel(new BorderLayout(0, 3)); panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(new JScrollPane(component), BorderLayout.CENTER); return panel; }
}
