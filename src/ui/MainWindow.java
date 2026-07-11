package ui;

import model.Book;
import model.Chapter;
import model.Location;
import model.CustomTab;
import model.CustomText;
import model.Character;
import model.Scene;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;

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
        frame.add(new Toolbar(e -> actions.newProject(), e -> actions.openProject(), e -> actions.saveProject(), e -> actions.addChapter()), BorderLayout.NORTH);
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
        references.addChangeListener(event -> {
            if (references.getSelectedComponent() == characters) {
                scenes.saveChanges();
                characters.refreshHistory();
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
        editor.setTextChanged(() -> setStatus(wordCount() + " words  •  " + editor.getText().length() + " characters"));
    }

    public void setActions(Actions actions) { this.actions = actions; }
    public void showWindow() { frame.setVisible(true); editor.focusEditor(); }
    public void dispose() { frame.dispose(); }
    public void setTitle(String title) { frame.setTitle(title); }
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
        goals.showEntries(book.getGoals()); notes.showEntries(book.getNotes());
        sentientRaces.showEntries(book.getSentientRaces());
        semiSentientRaces.showEntries(book.getSemiSentientRaces());
        nonSentientRaces.showEntries(book.getNonSentientRaces());
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
        String trimmed = editor.getText().trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }
}
