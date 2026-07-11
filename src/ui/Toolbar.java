package ui;

import javax.swing.*;
import java.awt.event.ActionListener;

/** Common writing actions available without opening menus. */
public final class Toolbar extends JToolBar {
    public Toolbar(ActionListener newProject, ActionListener open, ActionListener save, ActionListener newChapter) {
        setFloatable(false);
        addButton("New Project", newProject);
        addButton("Open", open);
        addButton("Save", save);
        addSeparator();
        addButton("+ Chapter", newChapter);
    }
    private void addButton(String title, ActionListener action) {
        JButton button = new JButton(title);
        button.addActionListener(action);
        add(button);
    }
}
