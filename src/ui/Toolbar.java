package ui;

import javax.swing.*;
import java.awt.event.ActionListener;

/** Common writing actions available without opening menus. */
public final class Toolbar extends JToolBar {
    public Toolbar(ActionListener newProject, ActionListener open, ActionListener save, ActionListener newChapter, ActionListener toggleTheme) {
        setFloatable(false);
        addButton("New Project", newProject);
        addButton("Open", open);
        addButton("Save", save);
        addSeparator();
        addButton("+ Chapter", newChapter);
        addSeparator();

        JToggleButton themeToggle = new JToggleButton("🌙 Dark Mode");
        themeToggle.setFocusable(false);
        themeToggle.addActionListener(e -> {
            themeToggle.setText(themeToggle.isSelected() ? "☀️ Light Mode" : "🌙 Dark Mode");
            toggleTheme.actionPerformed(e);
        });
        add(themeToggle);
    }

    /** Overloaded constructor without theme toggle */
    public Toolbar(ActionListener newProject, ActionListener open, ActionListener save, ActionListener newChapter) {
        this(newProject, open, save, newChapter, e -> {});
    }

    private void addButton(String title, ActionListener action) {
        JButton button = new JButton(title);
        button.addActionListener(action);
        add(button);
    }
}
