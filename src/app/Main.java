package app;

import model.Book;
import model.Chapter;
import service.ProjectService;
import service.SaveService;
import storage.JsonStorage;
import ui.MainWindow;

import javax.swing.*;
import java.nio.file.Path;

/** Application entry point and coordinator for the writing workspace. */
public final class Main {
    private final ProjectService projects = new ProjectService();
    private final SaveService saves = new SaveService(new JsonStorage());
    private final MainWindow window = new MainWindow();
    private Path projectFile;
    private Chapter selectedChapter;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        window.setActions(new MainWindow.Actions() {
            public void newProject() { createProject(); }
            public void openProject() { Main.this.openProject(); }
            public void saveProject() { Main.this.saveProject(false); }
            public void saveProjectAs() { Main.this.saveProject(true); }
            public void addChapter() { Main.this.addChapter(); }
            public void chapterSelected(Chapter chapter) { selectChapter(chapter); }
            public void exit() { window.dispose(); }
        });
        createProject();
        window.showWindow();
    }

    private void createProject() {
        projects.newProject("Untitled Book");
        projectFile = null;
        selectedChapter = projects.getBook().getChapters().get(0);
        refreshWorkspace();
        window.setStatus("New writing project");
    }

    private void openProject() {
        Path file = window.chooseProjectToOpen();
        if (file == null) return;
        try {
            projects.setBook(saves.open(file));
            projectFile = file;
            selectedChapter = projects.getBook().getChapters().get(0);
            refreshWorkspace();
            window.setStatus("Opened " + file.getFileName());
        } catch (Exception exception) {
            window.showError("Could not open this project.", exception);
        }
    }

    private void saveProject(boolean saveAs) {
        saveCurrentChapter();
        window.saveReferenceChanges();
        if (saveAs || projectFile == null) projectFile = window.chooseProjectToSave(projectFile);
        if (projectFile == null) return;
        try {
            saves.save(projectFile, projects.getBook());
            window.setStatus("Saved " + projectFile.getFileName());
        } catch (Exception exception) {
            window.showError("Could not save this project.", exception);
        }
    }

    private void addChapter() {
        String title = window.askForChapterTitle();
        if (title == null || title.isBlank()) return;
        saveCurrentChapter();
        selectedChapter = projects.addChapter(title.trim());
        refreshWorkspace();
        window.setStatus("Added " + selectedChapter.getTitle());
    }

    private void selectChapter(Chapter chapter) {
        if (chapter == null || chapter == selectedChapter) return;
        saveCurrentChapter();
        selectedChapter = chapter;
        window.showChapter(chapter);
    }

    private void saveCurrentChapter() {
        window.saveCurrentDocument();
    }

    private void refreshWorkspace() {
        Book book = projects.getBook();
        window.showBook(book, selectedChapter);
        window.setTitle(book.getTitle() + " — Escriba");
    }
}
