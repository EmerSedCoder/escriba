package ui;

import model.CustomFolder;
import model.CustomTab;
import model.CustomText;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.function.Consumer;

/** Folder navigator for a user-created tab; files open in the main editor. */
public final class CustomTabPanel extends JPanel {
    private final CustomTab tab;
    private final Consumer<CustomText> textSelected;
    private final DefaultMutableTreeNode root;
    private final DefaultTreeModel treeModel;
    private final JTree tree;

    public CustomTabPanel(CustomTab tab, Consumer<CustomText> textSelected) {
        super(new BorderLayout(6, 6)); this.tab = tab; this.textSelected = textSelected;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root = new DefaultMutableTreeNode(tab.getTitle()); treeModel = new DefaultTreeModel(root); tree = new JTree(treeModel); tree.setRootVisible(false);
        JButton addFolder = new JButton("+ Add Folder"); addFolder.addActionListener(e -> addFolder());
        JButton addText = new JButton("+ Add Text"); addText.addActionListener(e -> addText());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); actions.add(addFolder); actions.add(Box.createHorizontalStrut(6)); actions.add(addText); add(actions, BorderLayout.NORTH);
        tree.addTreeSelectionListener(event -> { Object selected = selectedObject(); if (selected instanceof CustomText text) textSelected.accept(text); });
        add(new JScrollPane(tree), BorderLayout.CENTER); rebuildTree(null);
    }
    public void saveChanges() { }
    private void addFolder() {
        String title = prompt("Folder name:", "New Folder"); if (title == null) return;
        CustomFolder folder = new CustomFolder(title); Object selected = selectedObject();
        if (selected instanceof CustomFolder parent) parent.getFolders().add(folder); else tab.getFolders().add(folder);
        rebuildTree(folder);
    }
    private void addText() {
        String title = prompt("Text file name:", "New Text File"); if (title == null) return;
        CustomText text = new CustomText(title); Object selected = selectedObject();
        if (selected instanceof CustomFolder folder) folder.getTexts().add(text); else tab.getTexts().add(text);
        rebuildTree(text); textSelected.accept(text);
    }
    private String prompt(String message, String dialogTitle) { String title = JOptionPane.showInputDialog(this, message, dialogTitle, JOptionPane.PLAIN_MESSAGE); return title == null || title.isBlank() ? null : title.trim(); }
    private Object selectedObject() { DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent(); return node == null ? null : node.getUserObject(); }
    private void rebuildTree(Object select) {
        root.removeAllChildren(); tab.getFolders().forEach(folder -> root.add(folderNode(folder))); tab.getTexts().forEach(text -> root.add(textNode(text))); treeModel.reload();
        for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        if (select != null) { DefaultMutableTreeNode node = findNode(root, select); if (node != null) tree.setSelectionPath(new TreePath(node.getPath())); }
    }
    private DefaultMutableTreeNode findNode(DefaultMutableTreeNode node, Object value) { if (node.getUserObject() == value) return node; for (int i = 0; i < node.getChildCount(); i++) { DefaultMutableTreeNode result = findNode((DefaultMutableTreeNode) node.getChildAt(i), value); if (result != null) return result; } return null; }
    private DefaultMutableTreeNode folderNode(CustomFolder folder) { DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder) { @Override public boolean isLeaf() { return false; } @Override public String toString() { return "Folder: " + folder.getTitle(); } }; folder.getFolders().forEach(child -> node.add(folderNode(child))); folder.getTexts().forEach(text -> node.add(textNode(text))); return node; }
    private DefaultMutableTreeNode textNode(CustomText text) { return new DefaultMutableTreeNode(text) { @Override public String toString() { return "Text: " + text.getTitle(); } }; }
}
