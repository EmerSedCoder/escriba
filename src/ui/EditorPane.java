package ui;

import model.Chapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Main editor with IDE-style, closable document tabs. */
public final class EditorPane extends JPanel {
    private final JTabbedPane documents = new JTabbedPane();
    private final Map<Object, JTextArea> editors = new IdentityHashMap<>();
    private Runnable textChanged = () -> { };
    private BiConsumer<Object, String> documentChanged = (document, content) -> { };

    public EditorPane() { super(new BorderLayout()); add(documents, BorderLayout.CENTER); }

    public void showChapter(Chapter chapter) { showDocument(chapter, chapter.getTitle(), chapter.getContent()); }
    /** Compatibility overload for document types that do not yet supply an identity. */
    public void showDocument(String title, String content) { showDocument(new Object(), title, content); }
    public void showDocument(Object document, String title, String content) {
        JTextArea text = editors.get(document);
        if (text == null) {
            text = textArea(document);
            editors.put(document, text);
            documents.addTab(title, new JScrollPane(text));
            int index = documents.indexOfComponent(documents.getComponentAt(documents.getTabCount() - 1));
            documents.setTabComponentAt(index, tabTitle(title, document));
        }
        int index = indexOf(document);
        documents.setSelectedIndex(index);
        text.setText(content);
        text.setCaretPosition(0);
    }
    public void clearDocuments() { editors.clear(); documents.removeAll(); }
    public Object getSelectedDocument() {
        int index = documents.getSelectedIndex();
        if (index < 0) return null;
        Component component = documents.getComponentAt(index);
        for (Map.Entry<Object, JTextArea> entry : editors.entrySet()) if (((JScrollPane) component).getViewport().getView() == entry.getValue()) return entry.getKey();
        return null;
    }
    public String getText() {
        Object document = getSelectedDocument();
        JTextArea text = document == null ? null : editors.get(document);
        return text == null ? "" : text.getText();
    }
    public void setTextChanged(Runnable listener) { textChanged = listener; }
    public void setDocumentChanged(BiConsumer<Object, String> listener) { documentChanged = listener; }
    public void undo() {
        Object undo = editor().getClientProperty("undo");
        if (undo instanceof UndoManager manager && manager.canUndo()) manager.undo();
    }
    public void cut() { editor().cut(); }
    public void copy() { editor().copy(); }
    public void paste() { editor().paste(); }
    public void selectAll() { editor().selectAll(); }
    public void focusEditor() { JTextArea text = editors.get(getSelectedDocument()); if (text != null) text.requestFocusInWindow(); }

    private JTextArea textArea(Object document) {
        JTextArea text = new JTextArea();
        text.setFont(new Font(Font.SERIF, Font.PLAIN, 18)); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setMargin(new Insets(22, 34, 22, 34));
        UndoManager undo = new UndoManager(); text.getDocument().addUndoableEditListener(undo);
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { changed(); }
            public void removeUpdate(DocumentEvent event) { changed(); }
            public void changedUpdate(DocumentEvent event) { changed(); }
            private void changed() { documentChanged.accept(document, text.getText()); textChanged.run(); }
        });
        text.putClientProperty("undo", undo);
        return text;
    }
    private JPanel tabTitle(String title, Object document) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); tab.setOpaque(false);
        tab.add(new JLabel(title));
        JButton close = new JButton("×"); close.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3)); close.setFocusable(false);
        close.addActionListener(event -> close(document)); tab.add(close); return tab;
    }
    private void close(Object document) { int index = indexOf(document); if (index >= 0) { editors.remove(document); documents.removeTabAt(index); } }
    private int indexOf(Object document) {
        JTextArea text = editors.get(document); if (text == null) return -1;
        for (int index = 0; index < documents.getTabCount(); index++) if (((JScrollPane) documents.getComponentAt(index)).getViewport().getView() == text) return index;
        return -1;
    }
    private JTextArea editor() { JTextArea text = editors.get(getSelectedDocument()); return text == null ? new JTextArea() : text; }
}
