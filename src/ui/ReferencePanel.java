package ui;

import model.NamedDescription;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.List;
import java.util.function.Function;

/** Shared editor for named descriptions such as characters and locations. */
public final class ReferencePanel<T extends NamedDescription> extends JPanel {
    private final String kind;
    private final Function<String, T> factory;
    private final DefaultListModel<T> listModel = new DefaultListModel<>();
    private final JList<T> list = new JList<>(listModel);
    private final JTextArea description = new JTextArea();
    private List<T> entries;
    private T selected;

    public ReferencePanel(String kind, Function<String, T> factory) {
        super(new BorderLayout(6, 6));
        this.kind = kind;
        this.factory = factory;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton add = new JButton("+ " + kind);
        add.addActionListener(e -> addEntry());
        add(add, BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(this::selectEntry);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(list), new JScrollPane(description));
        split.setResizeWeight(.35);
        add(split, BorderLayout.CENTER);
    }

    public void showEntries(List<T> entries) {
        saveSelected();
        this.entries = entries;
        listModel.clear();
        entries.forEach(listModel::addElement);
        selected = null;
        description.setText("");
    }
    public void saveChanges() { saveSelected(); }

    private void addEntry() {
        String title = JOptionPane.showInputDialog(this, kind + " name:", "New " + kind, JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.isBlank()) return;
        saveSelected();
        T entry = factory.apply(title.trim());
        entries.add(entry);
        listModel.addElement(entry);
        list.setSelectedValue(entry, true);
    }

    private void selectEntry(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) return;
        saveSelected();
        selected = list.getSelectedValue();
        description.setText(selected == null ? "" : selected.getContent());
    }

    private void saveSelected() {
        if (selected != null) selected.setContent(description.getText());
    }
}
