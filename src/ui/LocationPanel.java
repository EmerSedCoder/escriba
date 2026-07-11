package ui;

import model.Book;
import model.Location;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Hierarchical location editor: continents can contain countries, cities, and streets. */
public final class LocationPanel extends JPanel {
    private final JTree locationTree = new JTree();
    private final JLabel parent = new JLabel("Top-level location");
    private final DefaultListModel<String> characterModel = new DefaultListModel<>();
    private final JList<String> visitors = new JList<>(characterModel);
    private Book book;
    private Location selected;
    private Consumer<Location> selectionListener = ignored -> { };

    public LocationPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton add = new JButton("+ Location"); add.addActionListener(e -> addLocation("")); actions.add(add);
        JButton addChild = new JButton("+ Sub-location"); addChild.addActionListener(e -> addLocation(selected == null ? "" : selected.getTitle())); actions.add(addChild);
        add(actions, BorderLayout.NORTH);
        locationTree.setRootVisible(true); locationTree.addTreeSelectionListener(this::selectLocation);
        visitors.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        visitors.setCellRenderer((source, name, index, chosen, focused) -> visitorLabel(source, name, chosen));
        JPanel fields = new JPanel(new GridLayout(2, 1, 0, 6));
        fields.add(labeled("Inside", parent)); fields.add(labeled("Visitors", new JScrollPane(visitors)));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(locationTree), fields); split.setResizeWeight(.35); add(split, BorderLayout.CENTER);
    }

    public void showBook(Book book) { saveSelected(); this.book = book; selected = null; refreshCharacters(); rebuildTree(null); }
    public void saveChanges() { saveSelected(); }
    public void setSelectionListener(Consumer<Location> listener) { selectionListener = listener; }

    private void addLocation(String parentName) {
        String name = JOptionPane.showInputDialog(this, "Location name:", parentName.isEmpty() ? "New Location" : "New Sub-location", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        saveSelected();
        Location location = new Location(name.trim()); location.setParentLocationName(parentName); book.getLocations().add(location);
        rebuildTree(location);
    }
    private void selectLocation(TreeSelectionEvent event) {
        if (event.getPath() == null) return;
        saveSelected();
        Object object = ((DefaultMutableTreeNode) event.getPath().getLastPathComponent()).getUserObject();
        selected = object instanceof Location location ? location : object instanceof DescriptionFile file ? file.location() : null;
        if (selected == null) { parent.setText("Top-level location"); visitors.clearSelection(); return; }
        parent.setText(selected.getParentLocationName().isBlank() ? "Top-level location" : selected.getParentLocationName());
        int[] indexes = selected.getVisitorNames().stream().mapToInt(characterModel::indexOf).filter(index -> index >= 0).toArray(); visitors.setSelectedIndices(indexes);
        selectionListener.accept(selected);
    }
    private void saveSelected() { if (selected != null) { selected.getVisitorNames().clear(); selected.getVisitorNames().addAll(visitors.getSelectedValuesList()); } }
    private void refreshCharacters() { characterModel.clear(); book.getCharacters().forEach(character -> characterModel.addElement(character.getTitle())); }

    private void rebuildTree(Location select) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Locations");
        Set<Location> added = new HashSet<>();
        for (Location location : book.getLocations()) if (location.getParentLocationName().isBlank() || find(location.getParentLocationName()) == null) addBranch(root, location, added);
        for (Location location : book.getLocations()) if (!added.contains(location)) addBranch(root, location, added);
        locationTree.setModel(new DefaultTreeModel(root));
        for (int row = 0; row < locationTree.getRowCount(); row++) locationTree.expandRow(row);
        if (select != null) selectNode(root, select);
    }
    private void addBranch(DefaultMutableTreeNode parentNode, Location location, Set<Location> added) {
        if (!added.add(location)) return;
        // Each location is a folder. Its description is represented as a text file inside it.
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(location); parentNode.add(node);
        node.add(new DefaultMutableTreeNode(new DescriptionFile(location)));
        for (Location child : book.getLocations()) if (location.getTitle().equals(child.getParentLocationName())) addBranch(node, child, added);
    }
    private Location find(String name) { return book.getLocations().stream().filter(location -> location.getTitle().equals(name)).findFirst().orElse(null); }
    private void selectNode(DefaultMutableTreeNode root, Location target) {
        java.util.Enumeration<?> nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) { DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement(); if (node.getUserObject() == target) { locationTree.setSelectionPath(new TreePath(node.getPath())); return; } }
    }
    private record DescriptionFile(Location location) {
        @Override public String toString() { return "description.txt"; }
    }
    private JLabel visitorLabel(JList<? extends String> source, String name, boolean chosen) { JLabel label = new JLabel((chosen ? "✓ " : "+ ") + name); label.setOpaque(true); label.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5)); label.setBackground(chosen ? source.getSelectionBackground() : source.getBackground()); label.setForeground(chosen ? source.getSelectionForeground() : source.getForeground()); return label; }
    private JPanel labeled(String label, JComponent component) { JPanel panel = new JPanel(new BorderLayout(0, 3)); panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(component, BorderLayout.CENTER); return panel; }
}
