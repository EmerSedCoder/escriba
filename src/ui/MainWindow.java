package ui;

import model.Book;
import model.Chapter;
import model.Location;
import model.CustomTab;
import model.CustomText;
import model.Character;
import model.Scene;
import model.Timeline;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import javax.swing.JToggleButton;
import java.nio.file.Path;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/** The top-level professional writing workspace. */
public final class MainWindow {
    public interface Actions {
        void newProject(); void openProject(); void saveProject(); void saveProjectAs();
        void addChapter(); void chapterSelected(Chapter chapter); void exit();
    }
    private final JFrame frame = new JFrame("Escriba");
    private final ProjectTree projectTree = new ProjectTree();
    private final EditorPane editor = new EditorPane();
    private final CharacterPanel characters = new CharacterPanel();
    private final ScenePanel scenes = new ScenePanel();
    private final ReferencePanel<model.Item> items = new ReferencePanel<>("Item", model.Item::new);
    private final LocationPanel locations = new LocationPanel();
    private final ReferencePanel<model.Note> notes = new ReferencePanel<>("Note", model.Note::new);
    private final ReferencePanel<model.Race> sentientRaces = new ReferencePanel<>("Race", model.Race::new);
    private final ReferencePanel<model.Race> semiSentientRaces = new ReferencePanel<>("Race", model.Race::new);
    private final ReferencePanel<model.Race> nonSentientRaces = new ReferencePanel<>("Race", model.Race::new);
    private final GoalPanel goals = new GoalPanel();
    private final JTabbedPane timelinesPane = new JTabbedPane();
    private final JTabbedPane references = new JTabbedPane();
    private final JLabel status = new JLabel("  Ready");
    private Actions actions;
    private Book book;
    private Location activeLocation;
    private boolean creatingCustomTab;

    public MainWindow() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(createMenuBar());
        frame.add(new Toolbar(e -> actions.newProject(), e -> actions.openProject(), e -> actions.saveProject(), e -> actions.addChapter(), e -> toggleTheme(e)), BorderLayout.NORTH);
        references.addTab("Manuscript", projectTree);
        references.addTab("Characters", characters);
        references.addTab("Scenes", scenes);
        references.addTab("Items", items);
        references.addTab("Locations", locations);
        references.addTab("Goals", goals);
        references.addTab("Notes", notes);
        JTabbedPane races = new JTabbedPane();
        races.addTab("Sentient", sentientRaces);
        races.addTab("Semi-Sentient", semiSentientRaces);
        races.addTab("Not Sentient", nonSentientRaces);
        references.addTab("Races/Species", races);
        
        JPanel timelinesPanel = new JPanel(new BorderLayout(6, 6));
        timelinesPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel timelineButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton addTimeline = new JButton("+ Timeline");
        addTimeline.addActionListener(e -> addTimeline());
        JButton deleteTimeline = new JButton("Delete Timeline");
        deleteTimeline.addActionListener(e -> deleteTimeline());
        timelineButtons.add(addTimeline);
        timelineButtons.add(deleteTimeline);
        timelinesPanel.add(timelineButtons, BorderLayout.NORTH);
        timelinesPanel.add(timelinesPane, BorderLayout.CENTER);
        references.addTab("Timelines", timelinesPanel);
        
        references.addChangeListener(event -> {
            if (references.getSelectedComponent() == characters) {
                scenes.saveChanges();
                characters.refreshHistory();
            }
            if (references.getSelectedComponent() == goals) {
                characters.saveChanges();
                goals.refreshLinkedCharacters();
            }
            if (!creatingCustomTab && references.getSelectedIndex() >= 0 && "+".equals(references.getTitleAt(references.getSelectedIndex()))) createCustomTab();
        });
        
        characters.setCharacterViewer(this::showCharacterInEditor);
        scenes.setSceneViewer(this::showSceneInEditor);
        items.setEntryViewer(this::showItemInEditor);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, references, editor);
        split.setResizeWeight(0);
        split.setDividerLocation(235);
        frame.add(split, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(1100, 720);
        frame.setMinimumSize(new Dimension(720, 480));
        frame.setLocationByPlatform(true);
        projectTree.setSelectionListener(chapter -> actions.chapterSelected(chapter));
        locations.setSelectionListener(this::showLocation);
        locations.setLocationsChanged(scenes::refreshProjectChoices);
        editor.setDocumentChanged(this::saveDocumentContent);
        editor.setTextChanged(() -> setStatus(wordCount() + " words  •  " + editor.getPlainText().length() + " characters"));
    }

    public void setActions(Actions actions) { this.actions = actions; }
    public void showWindow() { frame.setVisible(true); editor.focusEditor(); }
    public void dispose() { frame.dispose(); }
    public void setTitle(String title) { frame.setTitle(title); }
    private boolean darkMode = false;

    private void toggleTheme(ActionEvent e) {
        darkMode = !darkMode;
        applyCurrentTheme();
    }

    private void applyCurrentTheme() {
        Color bg, inputBg, controlBg, fg, caretColor;
        if (darkMode) {
            bg         = new Color(30, 30, 30);   // #1E1E1E
            inputBg    = new Color(40, 40, 40);   // #282828
            controlBg  = new Color(51, 51, 51);   // #333333
            fg         = Color.WHITE;
            caretColor = Color.WHITE;
        } else {
            bg         = javax.swing.UIManager.getLookAndFeelDefaults().getColor("Panel.background");
            inputBg    = Color.WHITE;
            controlBg  = javax.swing.UIManager.getLookAndFeelDefaults().getColor("Button.background");
            fg         = Color.BLACK;
            caretColor = Color.BLACK;
            if (bg == null) bg = new Color(238, 238, 238);
            if (controlBg == null) controlBg = new Color(238, 238, 238);
        }
        // Apply to every component recursively
        applyThemeToComponent(frame.getJMenuBar(), bg, controlBg, inputBg, fg, caretColor);
        applyThemeToComponent(frame.getContentPane(), bg, controlBg, inputBg, fg, caretColor);
        // Status bar
        status.setBackground(bg);
        status.setForeground(fg);
        status.setOpaque(true);
        frame.getContentPane().setBackground(bg);
        frame.repaint();
    }

    private void applyThemeToComponent(java.awt.Component comp, Color bg, Color controlBg,
                                        Color inputBg, Color fg, Color caretColor) {
        if (comp == null) return;

        if (comp instanceof RichTextPane rtp) {
            // HTML editor: set component colors + update stylesheet
            rtp.setBackground(inputBg);
            rtp.setForeground(fg);
            rtp.setCaretColor(caretColor);
            rtp.setOpaque(true);
            updateRichTextPaneTheme(rtp, inputBg, fg);
        } else if (comp instanceof javax.swing.text.JTextComponent tc) {
            tc.setBackground(inputBg);
            tc.setForeground(fg);
            tc.setCaretColor(caretColor);
            tc.setOpaque(true);
        } else if (comp instanceof JButton || comp instanceof JToggleButton) {
            comp.setBackground(controlBg);
            comp.setForeground(fg);
        } else if (comp instanceof JComboBox) {
            comp.setBackground(controlBg);
            comp.setForeground(fg);
        } else if (comp instanceof JList) {
            comp.setBackground(inputBg);
            comp.setForeground(fg);
        } else if (comp instanceof JTree) {
            comp.setBackground(inputBg);
            comp.setForeground(fg);
        } else if (comp instanceof JTable table) {
            table.setBackground(inputBg);
            table.setForeground(fg);
            table.setGridColor(darkMode ? new Color(60, 60, 60) : new Color(204, 204, 204));
        } else if (comp instanceof JLabel) {
            comp.setForeground(fg);
        } else if (comp instanceof JMenuBar || comp instanceof JMenu || comp instanceof JMenuItem) {
            comp.setBackground(bg);
            comp.setForeground(fg);
        } else if (comp instanceof JPanel || comp instanceof JSplitPane
                   || comp instanceof JToolBar || comp instanceof JScrollPane) {
            comp.setBackground(bg);
            comp.setForeground(fg);
        } else if (comp instanceof JTabbedPane tp) {
            tp.setBackground(bg);
            tp.setForeground(fg);
        } else if (comp instanceof JScrollBar) {
            comp.setBackground(bg);
        } else if (comp instanceof JViewport) {
            comp.setBackground(inputBg);
        }

        // Recurse into children
        if (comp instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                applyThemeToComponent(child, bg, controlBg, inputBg, fg, caretColor);
            }
        }
        // Also handle JMenu items
        if (comp instanceof JMenu menu) {
            for (int i = 0; i < menu.getItemCount(); i++) {
                JMenuItem item = menu.getItem(i);
                if (item != null) applyThemeToComponent(item, bg, controlBg, inputBg, fg, caretColor);
            }
        }
    }

    private void updateRichTextPaneTheme(RichTextPane rtp, Color bg, Color fg) {
        try {
            javax.swing.text.html.HTMLDocument doc = (javax.swing.text.html.HTMLDocument) rtp.getDocument();
            javax.swing.text.html.StyleSheet ss = doc.getStyleSheet();
            String bgHex = String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue());
            String fgHex = String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue());
            ss.addRule("body { color: " + fgHex + "; background-color: " + bgHex + "; }");
            ss.addRule("p, h1, h2, h3, h4, li, td, th, span, div, a { color: " + fgHex + "; }");
            rtp.repaint();
        } catch (Exception ex) {
            // Ignore if document type doesn't match
        }
    }
    public void setStatus(String message) { status.setText("  " + message); }
    public String getEditorText() { return editor.getText(); }
    public boolean isEditingChapter() { return editor.getSelectedDocument() instanceof Chapter; }
    public void saveCurrentDocument() { }
    public void saveReferenceChanges() {
        saveActiveLocation();
        characters.saveChanges(); scenes.saveChanges(); items.saveChanges(); locations.saveChanges();
        goals.saveChanges(); notes.saveChanges();
        sentientRaces.saveChanges(); nonSentientRaces.saveChanges();
        semiSentientRaces.saveChanges();
        for (Component component : references.getComponents()) if (component instanceof CustomTabPanel tab) tab.saveChanges();
    }
    public void showChapter(Chapter chapter) { activeLocation = null; editor.showChapter(chapter); }
    public void showBook(Book book, Chapter selected) {
        this.book = book;
        activeLocation = null;
        editor.clearDocuments(); projectTree.showBook(book, selected); editor.showChapter(selected);
        characters.showBook(book); scenes.showBook(book);
        items.showEntries(book.getItems()); locations.showBook(book);
        goals.setBook(book);
        goals.showEntries(book.getGoals()); notes.showEntries(book.getNotes());
        sentientRaces.showEntries(book.getSentientRaces());
        semiSentientRaces.showEntries(book.getSemiSentientRaces());
        nonSentientRaces.showEntries(book.getNonSentientRaces());
        showTimelines(book);
        showCustomTabs(book);
    }
    private void showLocation(Location location) {
        activeLocation = location;
        editor.showDocument(location, location.getTitle() + " — description.txt", location.getContent());
        editor.focusEditor();
    }
    private void saveActiveLocation() { }

    private void showSceneInEditor(Scene scene) {
        activeLocation = null;
        editor.showDocument(scene, "Scene: " + scene.getTitle(), scene.getContent());
        editor.focusEditor();
    }

    private void showCharacterInEditor(Character character) {
        activeLocation = null;
        editor.showDocument(character, "Character: " + character.getTitle(), character.getContent());
        editor.focusEditor();
    }

    private void showItemInEditor(model.Item item) {
        activeLocation = null;
        editor.showDocument(item, "Item: " + item.getTitle(), item.getContent());
        editor.focusEditor();
    }

    private void showCustomText(CustomText text) {
        activeLocation = null;
        editor.showDocument(text, text.getTitle(), text.getContent());
        editor.focusEditor();
    }
    
    private void showTimelines(Book book) {
        timelinesPane.removeAll();
        for (Timeline timeline : book.getTimelines()) {
            TimelinePanel panel = new TimelinePanel();
            panel.showTimeline(timeline, book);
            timelinesPane.addTab(timeline.getTitle(), panel);
        }
    }
    
    private void addTimeline() {
        if (book == null) return;
        String title = JOptionPane.showInputDialog(frame, "Timeline name:", "New Timeline", JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.isBlank()) return;
        Timeline timeline = new Timeline(title.trim());
        book.getTimelines().add(timeline);
        TimelinePanel panel = new TimelinePanel();
        panel.showTimeline(timeline, book);
        timelinesPane.addTab(timeline.getTitle(), panel);
    }
    
    private void deleteTimeline() {
        if (book == null) return;
        int selectedIndex = timelinesPane.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= book.getTimelines().size()) {
            JOptionPane.showMessageDialog(frame, "Please select a timeline to delete", "Delete Timeline", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Timeline selected = book.getTimelines().get(selectedIndex);
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete timeline '" + selected.getTitle() + "'?\nThis will also clear timeline references from all scenes.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            book.getTimelines().remove(selectedIndex);
            for (model.Scene scene : book.getScenes()) {
                if (scene.getTimelineName().equals(selected.getTitle())) {
                    scene.setTimelineName("");
                    scene.setTimelineDateName("");
                }
            }
            showTimelines(book);
        }
    }
    
    private void saveDocumentContent(Object document, String content) {
        if (document instanceof Chapter chapter) chapter.setContent(content);
        else if (document instanceof Location location) location.setContent(content);
        else if (document instanceof Scene scene) scene.setContent(content);
        else if (document instanceof Character character) character.setContent(content);
        else if (document instanceof model.Item item) item.setContent(content);
        else if (document instanceof CustomText text) text.setContent(content);
    }

    private void showCustomTabs(Book book) {
        for (int index = references.getTabCount() - 1; index >= 0; index--) {
            if (references.getComponentAt(index) instanceof CustomTabPanel panel) {
                panel.saveChanges();
                references.removeTabAt(index);
            } else if ("+".equals(references.getTitleAt(index))) references.removeTabAt(index);
        }
        for (CustomTab tab : book.getCustomTabs()) references.addTab(tab.getTitle(), new CustomTabPanel(tab, this::showCustomText));
        references.addTab("+", new JPanel());
    }

    private void createCustomTab() {
        String title = JOptionPane.showInputDialog(frame, "Tab name:", "New Custom Tab", JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.isBlank()) {
            references.setSelectedIndex(0);
            return;
        }
        CustomTab tab = new CustomTab(title.trim());
        // A custom tab is only created after a project has been loaded into the workspace.
        if (book == null) return;
        book.getCustomTabs().add(tab);
        creatingCustomTab = true;
        try {
            int plusIndex = references.getSelectedIndex();
            references.insertTab(tab.getTitle(), null, new CustomTabPanel(tab, this::showCustomText), null, plusIndex);
            references.setSelectedIndex(plusIndex);
        } finally {
            creatingCustomTab = false;
        }
    }

    public Path chooseProjectToOpen() { return choose(false, null); }
    public Path chooseProjectToSave(Path current) { return choose(true, current); }
    public String askForChapterTitle() { return JOptionPane.showInputDialog(frame, "Chapter title:", "New Chapter", JOptionPane.PLAIN_MESSAGE); }
    public void showError(String message, Exception exception) { JOptionPane.showMessageDialog(frame, message + "\n" + exception.getMessage(), "Escriba", JOptionPane.ERROR_MESSAGE); }

    private Path choose(boolean saving, Path current) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Escriba projects (*.escriba)", "escriba"));
        if (current != null) chooser.setSelectedFile(current.toFile());
        int result = saving ? chooser.showSaveDialog(frame) : chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        Path chosen = chooser.getSelectedFile().toPath();
        return saving && !chosen.toString().endsWith(".escriba") ? Path.of(chosen + ".escriba") : chosen;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        file.add(item("New Project", 'N', e -> actions.newProject()));
        file.add(item("Open Project…", 'O', e -> actions.openProject()));
        file.add(item("Save", 'S', e -> actions.saveProject()));
        file.add(item("Save As…", 0, e -> actions.saveProjectAs()));
        file.addSeparator(); file.add(item("Exit", 0, e -> actions.exit()));
        JMenu project = new JMenu("Project");
        project.add(item("New Chapter", 0, e -> actions.addChapter()));
        JMenu edit = new JMenu("Edit");
        edit.add(item("Undo", 'Z', e -> editor.undo()));
        edit.addSeparator(); edit.add(item("Cut", 'X', e -> editor.cut()));
        edit.add(item("Copy", 'C', e -> editor.copy())); edit.add(item("Paste", 'V', e -> editor.paste()));
        edit.addSeparator(); edit.add(item("Select All", 'A', e -> editor.selectAll()));
        bar.add(file); bar.add(edit); bar.add(project);
        return bar;
    }
    private JMenuItem item(String label, int key, ActionListener listener) {
        JMenuItem item = new JMenuItem(label);
        if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        item.addActionListener(listener); return item;
    }
    private int wordCount() {
        String trimmed = editor.getPlainText().trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }
}
