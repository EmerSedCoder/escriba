package ui;

import model.Book;
import model.Chapter;
import model.Location;

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
    private final GoalPanel goals = new GoalPanel();
    private final JLabel status = new JLabel("  Ready");
    private Actions actions;
    private Location activeLocation;

    public MainWindow() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(createMenuBar());
        frame.add(new Toolbar(e -> actions.newProject(), e -> actions.openProject(), e -> actions.saveProject(), e -> actions.addChapter()), BorderLayout.NORTH);
        JTabbedPane references = new JTabbedPane();
        references.addTab("Manuscript", projectTree);
        references.addTab("Characters", characters);
        references.addTab("Scenes", scenes);
        references.addTab("Items", items);
        references.addTab("Locations", locations);
        references.addTab("Goals", goals);
        references.addTab("Notes", notes);
        references.addChangeListener(event -> {
            if (references.getSelectedComponent() == characters) {
                scenes.saveChanges();
                characters.refreshHistory();
            }
        });
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
        editor.setTextChanged(() -> setStatus(wordCount() + " words  •  " + editor.getText().length() + " characters"));
    }

    public void setActions(Actions actions) { this.actions = actions; }
    public void showWindow() { frame.setVisible(true); editor.focusEditor(); }
    public void dispose() { frame.dispose(); }
    public void setTitle(String title) { frame.setTitle(title); }
    public void setStatus(String message) { status.setText("  " + message); }
    public String getEditorText() { return editor.getText(); }
    public boolean isEditingChapter() { return activeLocation == null; }
    public void saveReferenceChanges() {
        saveActiveLocation();
        characters.saveChanges(); scenes.saveChanges(); items.saveChanges(); locations.saveChanges();
        goals.saveChanges(); notes.saveChanges();
    }
    public void showChapter(Chapter chapter) { saveActiveLocation(); activeLocation = null; editor.showChapter(chapter); }
    public void showBook(Book book, Chapter selected) {
        saveActiveLocation(); activeLocation = null;
        projectTree.showBook(book, selected); editor.showChapter(selected);
        characters.showBook(book); scenes.showBook(book);
        items.showEntries(book.getItems()); locations.showBook(book);
        goals.showEntries(book.getGoals()); notes.showEntries(book.getNotes());
    }
    private void showLocation(Location location) {
        saveActiveLocation(); activeLocation = location;
        editor.showDocument(location.getTitle() + " — description.txt", location.getContent());
        editor.focusEditor();
    }
    private void saveActiveLocation() { if (activeLocation != null) activeLocation.setContent(editor.getText()); }

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
