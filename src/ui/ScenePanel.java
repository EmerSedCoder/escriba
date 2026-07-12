package ui;

import model.Book;
import model.Character;
import model.Scene;
import model.Timeline;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.function.Consumer;

/** Scene planning editor that connects a scene to a location and characters. */
public final class ScenePanel extends JPanel {
    private final DefaultListModel<Scene> listModel = new DefaultListModel<>();
    private final JList<Scene> list = new JList<>(listModel);
    private final RichTextPane description = new RichTextPane();
    private final JComboBox<String> location = new JComboBox<>();
    private final JComboBox<String> timeline = new JComboBox<>();
    private final JComboBox<String> timelineDate = new JComboBox<>();
    private final DefaultListModel<String> characterModel = new DefaultListModel<>();
    private final JList<String> participants = new JList<>(characterModel);
    private Book book;
    private Scene selected;
    private Consumer<Scene> sceneViewer = scene -> {};

    public ScenePanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton add = new JButton("+ Scene");
        add.addActionListener(e -> addScene());
        JButton view = new JButton("View in Editor");
        view.addActionListener(e -> {
            if (selected != null) sceneViewer.accept(selected);
        });
        buttonPanel.add(add);
        buttonPanel.add(view);
        add(buttonPanel, BorderLayout.NORTH);
        list.addListSelectionListener(this::selectScene);
        // JTextPane handles wrapping automatically
        participants.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        participants.setCellRenderer((source, name, index, selected, focused) -> {
            JLabel label = new JLabel((selected ? "✓ " : "+ ") + name);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            label.setBackground(selected ? source.getSelectionBackground() : source.getBackground());
            label.setForeground(selected ? source.getSelectionForeground() : source.getForeground());
            return label;
        });
        timeline.addActionListener(e -> refreshTimelineDates());
        JPanel fields = new JPanel(new GridLayout(5, 1, 0, 6));
        fields.add(labeled("Description", new JScrollPane(description)));
        fields.add(labeled("Location", location));
        fields.add(labeled("Timeline", timeline));
        fields.add(labeled("Timeline Date", timelineDate));
        fields.add(labeled("Participating characters", new JScrollPane(participants)));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(list), fields);
        split.setResizeWeight(.3); add(split, BorderLayout.CENTER);
    }
    public void showBook(Book book) {
        saveSelected(); this.book = book; listModel.clear(); book.getScenes().forEach(listModel::addElement);
        refreshProjectChoices();
        selected = null; description.setText(""); participants.clearSelection();
    }
    public void saveChanges() { saveSelected(); }
    public void setSceneViewer(Consumer<Scene> viewer) { sceneViewer = viewer; }
    public void refreshProjectChoices() {
        if (book == null) return;
        refreshChoices();
    }
    private void addScene() {
        String name = JOptionPane.showInputDialog(this, "Scene name:", "New Scene", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        saveSelected(); refreshChoices();
        Scene scene = new Scene(name.trim()); book.getScenes().add(scene); listModel.addElement(scene); list.setSelectedValue(scene, true);
    }
    private void selectScene(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        saveSelected(); refreshChoices(); selected = list.getSelectedValue();
        if (selected == null) {
            description.setText(""); location.setSelectedIndex(0); timeline.setSelectedIndex(0); timelineDate.setSelectedIndex(0);
            participants.clearSelection(); return;
        }
        description.setText(selected.getContent());
        location.setSelectedItem(selected.getLocationName());
        timeline.setSelectedItem(selected.getTimelineName());
        refreshTimelineDates();
        timelineDate.setSelectedItem(selected.getTimelineDateName());
        int[] indexes = selected.getParticipantNames().stream().mapToInt(name -> characterModel.indexOf(name)).filter(index -> index >= 0).toArray();
        participants.setSelectedIndices(indexes);
    }
    private void saveSelected() {
        if (selected == null) return;
        selected.setContent(description.getText());
        selected.setLocationName((String) location.getSelectedItem());
        selected.setTimelineName((String) timeline.getSelectedItem());
        selected.setTimelineDateName((String) timelineDate.getSelectedItem());
        selected.getParticipantNames().clear();
        selected.getParticipantNames().addAll(participants.getSelectedValuesList());
        registerVisitorsAtSceneLocation();
    }
    /** Participating in a scene automatically counts as visiting that scene's location. */
    private void registerVisitorsAtSceneLocation() {
        if (selected.getLocationName().isBlank()) return;
        book.getLocations().stream()
                .filter(item -> item.getTitle().equals(selected.getLocationName()))
                .findFirst()
                .ifPresent(location -> selected.getParticipantNames().forEach(name -> {
                    if (!location.getVisitorNames().contains(name)) location.getVisitorNames().add(name);
                }));
    }
    /** Reloads the location dropdown and character checklist from the current project. */
    private void refreshChoices() {
        String currentLocation = (String) location.getSelectedItem();
        String currentTimeline = (String) timeline.getSelectedItem();
        java.util.List<String> currentParticipants = participants.getSelectedValuesList();
        characterModel.clear(); book.getCharacters().forEach(character -> characterModel.addElement(character.getTitle()));
        location.removeAllItems(); location.addItem(""); book.getLocations().forEach(item -> location.addItem(item.getTitle()));
        location.setSelectedItem(currentLocation);
        timeline.removeAllItems(); timeline.addItem(""); book.getTimelines().forEach(tl -> timeline.addItem(tl.getTitle()));
        timeline.setSelectedItem(currentTimeline);
        int[] indexes = currentParticipants.stream().mapToInt(characterModel::indexOf).filter(index -> index >= 0).toArray();
        participants.setSelectedIndices(indexes);
    }
    private void refreshTimelineDates() {
        String selectedTimelineName = (String) timeline.getSelectedItem();
        timelineDate.removeAllItems();
        timelineDate.addItem("");
        if (selectedTimelineName == null || selectedTimelineName.isBlank()) return;
        book.getTimelines().stream()
                .filter(tl -> tl.getTitle().equals(selectedTimelineName))
                .findFirst()
                .ifPresent(tl -> tl.getDates().forEach(date -> timelineDate.addItem(date.getName())));
    }
    private JPanel labeled(String label, JComponent component) { JPanel panel = new JPanel(new BorderLayout(0, 3)); panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(component, BorderLayout.CENTER); return panel; }
}
