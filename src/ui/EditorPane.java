package ui;

import model.Chapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Main editor with IDE-style, closable document tabs, formatting toolbar, and rich-text editing. */
public final class EditorPane extends JPanel {
    private final JTabbedPane documents = new JTabbedPane();
    private final Map<Object, RichTextPane> editors = new IdentityHashMap<>();
    private Runnable textChanged = () -> { };
    private BiConsumer<Object, String> documentChanged = (document, content) -> { };

    // Toolbar components
    private JComboBox<String> fontCombo;
    private JComboBox<Integer> sizeCombo;
    private JComboBox<String> formatCombo;
    private JToggleButton boldBtn;
    private JToggleButton italicBtn;
    private JToggleButton underlineBtn;
    private JToggleButton strikethroughBtn;
    private JToggleButton alignLeft;
    private JToggleButton alignCenter;
    private JToggleButton alignRight;
    private JToggleButton alignJustify;

    // Bottom panels
    private JPanel searchPanel;
    private JTextField findField;
    private JTextField replaceField;
    private JLabel statsLabel;

    private boolean isUpdatingToolbar = false;
    private int lastSearchPos = 0;

    public EditorPane() {
        super(new BorderLayout());

        // Build top toolbar wrapper
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buildToolbar(), BorderLayout.NORTH);
        add(topPanel, BorderLayout.NORTH);

        // Add standard center tabbed pane
        add(documents, BorderLayout.CENTER);

        // Add bottom search & statistics panels
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buildSearchPanel(), BorderLayout.NORTH);
        bottomPanel.add(buildStatsPanel(), BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Setup ChangeListener for switching document tabs
        documents.addChangeListener(e -> updateToolbarAndStats());

        // Keybindings (Ctrl+F for find)
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke("control F"), "find");
        am.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchPanel.setVisible(true);
                findField.requestFocusInWindow();
                findField.selectAll();
            }
        });
    }

    public void showChapter(Chapter chapter) { showDocument(chapter, chapter.getTitle(), chapter.getContent()); }

    /** Compatibility overload for document types that do not yet supply an identity. */
    public void showDocument(String title, String content) { showDocument(new Object(), title, content); }

    public void showDocument(Object document, String title, String content) {
        RichTextPane text = editors.get(document);
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
        updateToolbarAndStats();
    }

    public void clearDocuments() { editors.clear(); documents.removeAll(); }

    public Object getSelectedDocument() {
        int index = documents.getSelectedIndex();
        if (index < 0) return null;
        Component component = documents.getComponentAt(index);
        for (Map.Entry<Object, RichTextPane> entry : editors.entrySet()) {
            if (((JScrollPane) component).getViewport().getView() == entry.getValue()) return entry.getKey();
        }
        return null;
    }

    public String getText() {
        Object document = getSelectedDocument();
        RichTextPane text = document == null ? null : editors.get(document);
        return text == null ? "" : text.getText();
    }

    public String getPlainText() {
        Object document = getSelectedDocument();
        RichTextPane text = document == null ? null : editors.get(document);
        if (text == null) return "";
        try {
            return text.getDocument().getText(0, text.getDocument().getLength());
        } catch (Exception ex) {
            return "";
        }
    }

    public void setTextChanged(Runnable listener) { textChanged = listener; }
    public void setDocumentChanged(BiConsumer<Object, String> listener) { documentChanged = listener; }

    public void undo() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        Object undo = pane.getClientProperty("undo");
        if (undo instanceof UndoManager manager && manager.canUndo()) manager.undo();
    }

    public void redo() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        Object undo = pane.getClientProperty("undo");
        if (undo instanceof UndoManager manager && manager.canRedo()) manager.redo();
    }

    public void cut() { editor().cut(); }
    public void copy() { editor().copy(); }
    public void paste() { editor().paste(); }
    public void selectAll() { editor().selectAll(); }

    public void focusEditor() {
        RichTextPane text = editors.get(getSelectedDocument());
        if (text != null) text.requestFocusInWindow();
    }

    private RichTextPane currentRichTextPane() {
        int index = documents.getSelectedIndex();
        if (index < 0) return null;
        Component component = documents.getComponentAt(index);
        if (component instanceof JScrollPane scroll) {
            Component view = scroll.getViewport().getView();
            if (view instanceof RichTextPane pane) {
                return pane;
            }
        }
        return null;
    }

    private RichTextPane editor() {
        RichTextPane text = editors.get(getSelectedDocument());
        return text == null ? new RichTextPane() : text;
    }

    private RichTextPane textArea(Object document) {
        RichTextPane text = new RichTextPane();
        text.configureForMainEditor();

        UndoManager undo = new UndoManager();
        text.getDocument().addUndoableEditListener(undo);
        text.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { changed(); }
            public void removeUpdate(DocumentEvent event) { changed(); }
            public void changedUpdate(DocumentEvent event) { changed(); }
            private void changed() {
                documentChanged.accept(document, text.getText());
                textChanged.run();
                updateToolbarAndStats();
            }
        });
        text.addCaretListener(e -> updateToolbarAndStats());
        text.putClientProperty("undo", undo);
        return text;
    }

    private JPanel tabTitle(String title, Object document) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tab.setOpaque(false);
        tab.add(new JLabel(title));
        JButton close = new JButton("×");
        close.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        close.setFocusable(false);
        close.addActionListener(event -> close(document));
        tab.add(close);
        return tab;
    }

    private void close(Object document) {
        int index = indexOf(document);
        if (index >= 0) {
            editors.remove(document);
            documents.removeTabAt(index);
        }
    }

    private int indexOf(Object document) {
        RichTextPane text = editors.get(document);
        if (text == null) return -1;
        for (int index = 0; index < documents.getTabCount(); index++) {
            if (((JScrollPane) documents.getComponentAt(index)).getViewport().getView() == text) return index;
        }
        return -1;
    }

    // Formatting Actions helper logic
    private void applyFontFamily(String family) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setFontFamily(sas, family);
        pane.setCharacterAttributes(sas, false);
    }

    private void applyFontSize(int size) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setFontSize(sas, size);
        pane.setCharacterAttributes(sas, false);
    }

    private void toggleStrikethrough() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        StyledDocument doc = pane.getStyledDocument();
        int start = pane.getSelectionStart();
        int end = pane.getSelectionEnd();
        
        SimpleAttributeSet sas = new SimpleAttributeSet();
        if (start == end) {
            AttributeSet inputAttr = pane.getInputAttributes();
            boolean currentStrike = StyleConstants.isStrikeThrough(inputAttr);
            StyleConstants.setStrikeThrough(sas, !currentStrike);
            pane.setCharacterAttributes(sas, false);
        } else {
            AttributeSet attr = doc.getCharacterElement(start).getAttributes();
            boolean currentStrike = StyleConstants.isStrikeThrough(attr);
            StyleConstants.setStrikeThrough(sas, !currentStrike);
            doc.setCharacterAttributes(start, end - start, sas, false);
        }
        updateToolbarAndStats();
    }

    private void chooseTextColor() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        Color color = JColorChooser.showDialog(this, "Select Text Color", Color.BLACK);
        if (color != null) {
            SimpleAttributeSet sas = new SimpleAttributeSet();
            StyleConstants.setForeground(sas, color);
            pane.setCharacterAttributes(sas, false);
            updateToolbarAndStats();
        }
    }

    private void chooseHighlightColor() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        Color color = JColorChooser.showDialog(this, "Select Highlight Color", Color.YELLOW);
        if (color != null) {
            SimpleAttributeSet sas = new SimpleAttributeSet();
            StyleConstants.setBackground(sas, color);
            pane.setCharacterAttributes(sas, false);
            updateToolbarAndStats();
        }
    }

    private void setParagraphTag(String tag) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        try {
            int caretPos = pane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            Element elem = doc.getParagraphElement(caretPos);
            
            // Find the nearest block element (p, h1-4, li, implied)
            Element block = elem;
            while (block != null) {
                String name = block.getName();
                if ("p".equalsIgnoreCase(name) || "h1".equalsIgnoreCase(name) || "h2".equalsIgnoreCase(name) || 
                    "h3".equalsIgnoreCase(name) || "h4".equalsIgnoreCase(name) || "li".equalsIgnoreCase(name) ||
                    "implied".equalsIgnoreCase(name)) {
                    break;
                }
                block = block.getParentElement();
            }

            if (block != null) {
                int start = block.getStartOffset();
                int end = block.getEndOffset();
                int len = end - start;
                String text = doc.getText(start, len).trim();
                text = text.replace("\n", "").replace("\r", "");
                String html = "<" + tag + ">" + escapeHtml(text) + "</" + tag + ">";
                doc.setOuterHTML(block, html);
            } else {
                // Fallback
                int start = elem.getStartOffset();
                int end = elem.getEndOffset();
                int length = end - start;
                String text = doc.getText(start, length).trim();
                text = text.replace("\n", "").replace("\r", "");
                String html = "<" + tag + ">" + escapeHtml(text) + "</" + tag + ">";
                doc.remove(start, length);
                HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();
                kit.insertHTML(doc, start, html, 0, 0, null);
            }
            
            pane.setCaretPosition(Math.min(caretPos, doc.getLength()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void insertList(boolean numbered) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String tag = numbered ? "ol" : "ul";
        String selection = pane.getSelectedText();
        String html;
        if (selection != null && !selection.isBlank()) {
            html = "<" + tag + "><li>" + escapeHtml(selection) + "</li></" + tag + ">";
        } else {
            html = "<" + tag + "><li>&nbsp;</li></" + tag + ">";
        }
        insertHTML(html);
    }

    private void modifyIndentation(boolean increase) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        int pos = pane.getCaretPosition();
        HTMLDocument doc = (HTMLDocument) pane.getDocument();
        Element paragraph = doc.getParagraphElement(pos);
        AttributeSet attrs = paragraph.getAttributes();
        float indent = 0;
        Object value = attrs.getAttribute(StyleConstants.LeftIndent);
        if (value instanceof Float f) {
            indent = f;
        }
        indent = increase ? indent + 15.0f : Math.max(0.0f, indent - 15.0f);
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(sas, indent);
        pane.setParagraphAttributes(sas, false);
    }

    private void applyLineSpacing(float spacing) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(sas, spacing);
        pane.setParagraphAttributes(sas, false);
    }

    private void applyPageMargins(int margin) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        pane.setMargin(new Insets(margin, margin + 10, margin, margin + 10));
    }

    private void insertHyperlink() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String url = JOptionPane.showInputDialog(this, "URL (e.g., https://example.com):", "Insert Hyperlink", JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.trim().isEmpty()) return;
        String text = pane.getSelectedText();
        if (text == null || text.trim().isEmpty()) {
            text = JOptionPane.showInputDialog(this, "Link Text:", "Insert Hyperlink", JOptionPane.PLAIN_MESSAGE);
            if (text == null || text.trim().isEmpty()) text = url;
        }
        String html = "<a href=\"" + url.trim() + "\">" + escapeHtml(text) + "</a>";
        insertHTML(html);
    }

    private void insertImage() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Image to Insert");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().toURI().toString();
            String html = "<img src=\"" + path + "\" width=\"300\"/>";
            insertHTML(html);
        }
    }

    private void insertTable() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String rowsStr = JOptionPane.showInputDialog(this, "Number of rows:", "Insert Table", JOptionPane.PLAIN_MESSAGE);
        String colsStr = JOptionPane.showInputDialog(this, "Number of columns:", "Insert Table", JOptionPane.PLAIN_MESSAGE);
        if (rowsStr == null || colsStr == null) return;
        try {
            int rows = Integer.parseInt(rowsStr.trim());
            int cols = Integer.parseInt(colsStr.trim());
            StringBuilder sb = new StringBuilder("<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\" style=\"width:100%; border-collapse:collapse;\">");
            for (int r = 0; r < rows; r++) {
                sb.append("<tr>");
                for (int c = 0; c < cols; c++) {
                    sb.append("<td>&nbsp;</td>");
                }
                sb.append("</tr>");
            }
            sb.append("</table><p>&nbsp;</p>");
            insertHTML(sb.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printDocument() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;

        String headerText = JOptionPane.showInputDialog(this, "Document Header text (optional):", "Print Document", JOptionPane.PLAIN_MESSAGE);
        String footerText = JOptionPane.showInputDialog(this, "Document Footer text (optional):", "Print Document", JOptionPane.PLAIN_MESSAGE);

        try {
            java.text.MessageFormat header = null;
            if (headerText != null && !headerText.isBlank()) {
                header = new java.text.MessageFormat(headerText);
            }

            java.text.MessageFormat footer = new java.text.MessageFormat("Page {0}");
            if (footerText != null && !footerText.isBlank()) {
                footer = new java.text.MessageFormat(footerText + " - Page {0}");
            }

            pane.print(header, footer);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void insertHTML(String html) {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        try {
            int pos = pane.getCaretPosition();
            HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            kit.insertHTML(doc, pos, html, 0, 0, null);
            updateToolbarAndStats();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateToolbarAndStats() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) {
            statsLabel.setText("Words: 0  •  Characters: 0  •  Est. Read Time: 0 sec");
            return;
        }

        isUpdatingToolbar = true;
        try {
            // 1. Stats updates
            HTMLDocument doc = (HTMLDocument) pane.getDocument();
            String plainText = doc.getText(0, doc.getLength());
            int charCount = plainText.length();
            String trimmed = plainText.trim();
            int wordCount = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            
            int min = wordCount / 200;
            int sec = (int) (((wordCount % 200) / 200.0) * 60);
            String readTimeStr = (min > 0) ? String.format("%d min %d sec", min, sec) : String.format("%d sec", sec);
            
            statsLabel.setText(String.format("Words: %d  •  Characters: %d  •  Est. Read Time: %s", wordCount, charCount, readTimeStr));

            // 2. Caret attribute updates
            int pos = pane.getCaretPosition();
            AttributeSet attr = doc.getCharacterElement(pos).getAttributes();

            // Font Family
            String family = StyleConstants.getFontFamily(attr);
            fontCombo.setSelectedItem(family);

            // Font Size
            int size = StyleConstants.getFontSize(attr);
            sizeCombo.setSelectedItem(size);

            // Bold, Italic, Underline, Strikethrough
            boldBtn.setSelected(StyleConstants.isBold(attr));
            italicBtn.setSelected(StyleConstants.isItalic(attr));
            underlineBtn.setSelected(StyleConstants.isUnderline(attr));
            strikethroughBtn.setSelected(StyleConstants.isStrikeThrough(attr));

            // Alignment
            Element paragraph = doc.getParagraphElement(pos);
            AttributeSet pAttr = paragraph.getAttributes();
            int align = StyleConstants.getAlignment(pAttr);
            alignLeft.setSelected(align == StyleConstants.ALIGN_LEFT);
            alignCenter.setSelected(align == StyleConstants.ALIGN_CENTER);
            alignRight.setSelected(align == StyleConstants.ALIGN_RIGHT);
            alignJustify.setSelected(align == StyleConstants.ALIGN_JUSTIFIED);

            // Format (Headings)
            String tagName = paragraph.getName();
            if ("h1".equalsIgnoreCase(tagName)) formatCombo.setSelectedItem("Heading 1");
            else if ("h2".equalsIgnoreCase(tagName)) formatCombo.setSelectedItem("Heading 2");
            else if ("h3".equalsIgnoreCase(tagName)) formatCombo.setSelectedItem("Heading 3");
            else if ("h4".equalsIgnoreCase(tagName)) formatCombo.setSelectedItem("Heading 4");
            else formatCombo.setSelectedItem("Normal");

        } catch (Exception ex) {
            // Ignore temporary caret offset or parsing issues
        } finally {
            isUpdatingToolbar = false;
        }
    }

    // Find and Replace logic
    private void findNext() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String findText = findField.getText();
        if (findText.isEmpty()) return;

        try {
            String docText = pane.getDocument().getText(0, pane.getDocument().getLength());
            int index = docText.toLowerCase().indexOf(findText.toLowerCase(), lastSearchPos);
            if (index == -1) {
                index = docText.toLowerCase().indexOf(findText.toLowerCase(), 0); // wrap around
            }
            if (index != -1) {
                pane.select(index, index + findText.length());
                pane.requestFocusInWindow();
                lastSearchPos = index + findText.length();
            } else {
                JOptionPane.showMessageDialog(this, "Text not found", "Find", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void replace() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String findText = findField.getText();
        String replaceText = replaceField.getText();
        if (findText.isEmpty() || pane.getSelectedText() == null) return;

        if (pane.getSelectedText().equalsIgnoreCase(findText)) {
            pane.replaceSelection(replaceText);
        }
        findNext();
    }

    private void replaceAll() {
        RichTextPane pane = currentRichTextPane();
        if (pane == null) return;
        String findText = findField.getText();
        String replaceText = replaceField.getText();
        if (findText.isEmpty()) return;

        try {
            int startPos = 0;
            int count = 0;
            String docText = pane.getDocument().getText(0, pane.getDocument().getLength());
            while (true) {
                int index = docText.toLowerCase().indexOf(findText.toLowerCase(), startPos);
                if (index == -1) break;
                pane.select(index, index + findText.length());
                pane.replaceSelection(replaceText);
                count++;
                docText = pane.getDocument().getText(0, pane.getDocument().getLength());
                startPos = index + replaceText.length();
            }
            JOptionPane.showMessageDialog(this, "Replaced " + count + " occurrences", "Replace All", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Component Builders
    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Undo / Redo
        toolbar.add(createToolbarButton("↶", "Undo", e -> undo()));
        toolbar.add(createToolbarButton("↷", "Redo", e -> redo()));
        toolbar.addSeparator();

        // Font Family
        String[] fonts = {"Arial", "Courier New", "Georgia", "Times New Roman", "Verdana"};
        fontCombo = new JComboBox<>(fonts);
        fontCombo.setToolTipText("Font Family");
        fontCombo.setMaximumSize(new Dimension(120, 25));
        fontCombo.addActionListener(e -> {
            if (!isUpdatingToolbar) {
                String fam = (String) fontCombo.getSelectedItem();
                if (fam != null) applyFontFamily(fam);
            }
        });
        toolbar.add(fontCombo);
        toolbar.addSeparator();

        // Font Size
        Integer[] sizes = {8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72};
        sizeCombo = new JComboBox<>(sizes);
        sizeCombo.setToolTipText("Font Size");
        sizeCombo.setMaximumSize(new Dimension(60, 25));
        sizeCombo.addActionListener(e -> {
            if (!isUpdatingToolbar) {
                Integer sz = (Integer) sizeCombo.getSelectedItem();
                if (sz != null) applyFontSize(sz);
            }
        });
        toolbar.add(sizeCombo);
        toolbar.addSeparator();

        // Headings / Formats
        String[] formats = {"Normal", "Heading 1", "Heading 2", "Heading 3", "Heading 4"};
        formatCombo = new JComboBox<>(formats);
        formatCombo.setToolTipText("Paragraph Format");
        formatCombo.setMaximumSize(new Dimension(100, 25));
        formatCombo.addActionListener(e -> {
            if (!isUpdatingToolbar) {
                String fmt = (String) formatCombo.getSelectedItem();
                if (fmt != null) {
                    String tag = switch (fmt) {
                        case "Heading 1" -> "h1";
                        case "Heading 2" -> "h2";
                        case "Heading 3" -> "h3";
                        case "Heading 4" -> "h4";
                        default -> "p";
                    };
                    setParagraphTag(tag);
                }
            }
        });
        toolbar.add(formatCombo);
        toolbar.addSeparator();

        // Character Styles
        boldBtn = createToolbarToggleButton("B", "Bold", e -> new HTMLEditorKit.BoldAction().actionPerformed(e));
        boldBtn.setFont(boldBtn.getFont().deriveFont(Font.BOLD));
        italicBtn = createToolbarToggleButton("I", "Italic", e -> new HTMLEditorKit.ItalicAction().actionPerformed(e));
        italicBtn.setFont(italicBtn.getFont().deriveFont(Font.ITALIC));
        underlineBtn = createToolbarToggleButton("<html><u>U</u></html>", "Underline", e -> new HTMLEditorKit.UnderlineAction().actionPerformed(e));
        strikethroughBtn = createToolbarToggleButton("<html><s>S</s></html>", "Strikethrough", e -> toggleStrikethrough());

        toolbar.add(boldBtn);
        toolbar.add(italicBtn);
        toolbar.add(underlineBtn);
        toolbar.add(strikethroughBtn);
        toolbar.addSeparator();

        // Text & Background Colors
        toolbar.add(createToolbarButton("🎨 Text", "Text Color", e -> chooseTextColor()));
        toolbar.add(createToolbarButton("✏️ Highlight", "Highlight Color", e -> chooseHighlightColor()));
        toolbar.addSeparator();

        // Alignments
        alignLeft = createToolbarToggleButton("L", "Align Left", e -> new StyledEditorKit.AlignmentAction("Left", StyleConstants.ALIGN_LEFT).actionPerformed(e));
        alignCenter = createToolbarToggleButton("C", "Align Center", e -> new StyledEditorKit.AlignmentAction("Center", StyleConstants.ALIGN_CENTER).actionPerformed(e));
        alignRight = createToolbarToggleButton("R", "Align Right", e -> new StyledEditorKit.AlignmentAction("Right", StyleConstants.ALIGN_RIGHT).actionPerformed(e));
        alignJustify = createToolbarToggleButton("J", "Align Justify", e -> new StyledEditorKit.AlignmentAction("Justify", StyleConstants.ALIGN_JUSTIFIED).actionPerformed(e));

        ButtonGroup alignGroup = new ButtonGroup();
        alignGroup.add(alignLeft);
        alignGroup.add(alignCenter);
        alignGroup.add(alignRight);
        alignGroup.add(alignJustify);

        toolbar.add(alignLeft);
        toolbar.add(alignCenter);
        toolbar.add(alignRight);
        toolbar.add(alignJustify);
        toolbar.addSeparator();

        // Lists
        toolbar.add(createToolbarButton("• List", "Bullet List", e -> insertList(false)));
        toolbar.add(createToolbarButton("1. List", "Numbered List", e -> insertList(true)));
        toolbar.addSeparator();

        // Indentations
        toolbar.add(createToolbarButton("⇤", "Decrease Indent", e -> modifyIndentation(false)));
        toolbar.add(createToolbarButton("⇥", "Increase Indent", e -> modifyIndentation(true)));
        toolbar.addSeparator();

        // Line Spacing
        Double[] spacings = {1.0, 1.15, 1.5, 2.0};
        JComboBox<Double> spacingCombo = new JComboBox<>(spacings);
        spacingCombo.setToolTipText("Line Spacing");
        spacingCombo.setMaximumSize(new Dimension(60, 25));
        spacingCombo.addActionListener(e -> {
            Double sp = (Double) spacingCombo.getSelectedItem();
            if (sp != null) applyLineSpacing(sp.floatValue());
        });
        toolbar.add(spacingCombo);
        toolbar.addSeparator();

        // Margins
        String[] margins = {"Margin: 30px", "Margin: 15px", "Margin: 50px"};
        JComboBox<String> marginCombo = new JComboBox<>(margins);
        marginCombo.setToolTipText("Page Margins");
        marginCombo.setMaximumSize(new Dimension(110, 25));
        marginCombo.addActionListener(e -> {
            String mg = (String) marginCombo.getSelectedItem();
            if (mg != null) {
                int margin = switch (mg) {
                    case "Margin: 15px" -> 15;
                    case "Margin: 50px" -> 50;
                    default -> 30;
                };
                applyPageMargins(margin);
            }
        });
        toolbar.add(marginCombo);
        toolbar.addSeparator();

        // Insert Links, Images, Tables
        toolbar.add(createToolbarButton("🔗 Link", "Insert Link", e -> insertHyperlink()));
        toolbar.add(createToolbarButton("🖼 Image", "Insert Image", e -> insertImage()));
        toolbar.add(createToolbarButton("⊞ Table", "Insert Table", e -> insertTable()));
        toolbar.addSeparator();

        // Print document
        toolbar.add(createToolbarButton("🖨 Print", "Print Document", e -> printDocument()));

        return toolbar;
    }

    private JPanel buildSearchPanel() {
        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        searchPanel.setVisible(false);

        searchPanel.add(new JLabel("Find:"));
        findField = new JTextField(12);
        findField.addActionListener(e -> findNext());
        searchPanel.add(findField);

        searchPanel.add(new JLabel("Replace:"));
        replaceField = new JTextField(12);
        searchPanel.add(replaceField);

        JButton nextBtn = new JButton("Find Next");
        nextBtn.addActionListener(e -> findNext());
        searchPanel.add(nextBtn);

        JButton repBtn = new JButton("Replace");
        repBtn.addActionListener(e -> replace());
        searchPanel.add(repBtn);

        JButton allBtn = new JButton("Replace All");
        allBtn.addActionListener(e -> replaceAll());
        searchPanel.add(allBtn);

        JButton closeBtn = new JButton("×");
        closeBtn.setToolTipText("Close Search");
        closeBtn.setFocusable(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        closeBtn.addActionListener(e -> searchPanel.setVisible(false));
        searchPanel.add(closeBtn);

        return searchPanel;
    }

    private JPanel buildStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        statsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        statsLabel = new JLabel("Words: 0  •  Characters: 0  •  Est. Read Time: 0 sec");
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        statsPanel.add(statsLabel);
        return statsPanel;
    }

    private JButton createToolbarButton(String text, String tooltip, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.addActionListener(action);
        btn.setFocusable(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
        return btn;
    }

    private JToggleButton createToolbarToggleButton(String text, String tooltip, ActionListener action) {
        JToggleButton btn = new JToggleButton(text);
        btn.setToolTipText(tooltip);
        btn.addActionListener(action);
        btn.setFocusable(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
        return btn;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
