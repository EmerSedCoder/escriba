package ui;

import model.Chapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.*;

/** The distraction-free manuscript editing surface. */
public final class EditorPane extends JPanel {
    private final JTextArea text = new JTextArea();
    private final JLabel chapterName = new JLabel("Chapter");
    private final UndoManager undo = new UndoManager();
    private Runnable textChanged = () -> { };

    public EditorPane() {
        super(new BorderLayout());
        chapterName.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        chapterName.setFont(chapterName.getFont().deriveFont(Font.BOLD, 14f));
        text.setFont(new Font(Font.SERIF, Font.PLAIN, 18));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setMargin(new Insets(22, 34, 22, 34));
        text.getDocument().addUndoableEditListener(undo);
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { textChanged.run(); }
            public void removeUpdate(DocumentEvent e) { textChanged.run(); }
            public void changedUpdate(DocumentEvent e) { textChanged.run(); }
        });
        add(chapterName, BorderLayout.NORTH);
        add(new JScrollPane(text), BorderLayout.CENTER);
    }

    public void showChapter(Chapter chapter) {
        showDocument(chapter.getTitle(), chapter.getContent());
    }
    public void showDocument(String title, String content) {
        chapterName.setText(title);
        text.setText(content);
        undo.discardAllEdits();
        text.setCaretPosition(0);
    }
    public String getText() { return text.getText(); }
    public void setTextChanged(Runnable listener) { textChanged = listener; }
    public void undo() { if (undo.canUndo()) undo.undo(); }
    public void cut() { text.cut(); }
    public void copy() { text.copy(); }
    public void paste() { text.paste(); }
    public void selectAll() { text.selectAll(); }
    public void focusEditor() { text.requestFocusInWindow(); }
}
