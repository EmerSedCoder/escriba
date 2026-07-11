package ui;

import model.Book;
import model.Chapter;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.function.Consumer;

/** Navigator for the book manuscript. */
public final class ProjectTree extends JPanel {
    private final JTree tree = new JTree();
    private Consumer<Chapter> selectionListener = ignored -> { };

    public ProjectTree() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        tree.setRootVisible(true);
        tree.addTreeSelectionListener(this::selectionChanged);
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    public void showBook(Book book, Chapter selected) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(book.getTitle());
        DefaultMutableTreeNode selectedNode = null;
        for (Chapter chapter : book.getChapters()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(chapter);
            root.add(node);
            if (chapter == selected) selectedNode = node;
        }
        tree.setModel(new DefaultTreeModel(root));
        for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        if (selectedNode != null) tree.setSelectionPath(new javax.swing.tree.TreePath(selectedNode.getPath()));
    }

    public void setSelectionListener(Consumer<Chapter> listener) { selectionListener = listener; }
    private void selectionChanged(TreeSelectionEvent event) {
        Object selected = tree.getLastSelectedPathComponent();
        if (selected instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Chapter chapter) selectionListener.accept(chapter);
    }
}
