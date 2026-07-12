package ui;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * A styled text editor component based on JTextPane and HTMLEditorKit.
 * Used for rich text editing in Escriba.
 */
public final class RichTextPane extends JTextPane {
    public RichTextPane() {
        HTMLEditorKit kit = new HTMLEditorKit();
        setEditorKit(kit);
        setContentType("text/html");
        
        // Load clean default CSS styles
        HTMLDocument doc = (HTMLDocument) getDocument();
        StyleSheet styleSheet = doc.getStyleSheet();
        styleSheet.addRule("body { font-family: Arial, sans-serif; font-size: 12pt; margin: 8px; line-height: 1.3; }");
        styleSheet.addRule("h1 { font-size: 18pt; font-weight: bold; margin-top: 10px; margin-bottom: 6px; }");
        styleSheet.addRule("h2 { font-size: 16pt; font-weight: bold; margin-top: 8px; margin-bottom: 5px; }");
        styleSheet.addRule("h3 { font-size: 14pt; font-weight: bold; margin-top: 6px; margin-bottom: 4px; }");
        styleSheet.addRule("h4 { font-size: 12pt; font-weight: bold; margin-top: 4px; margin-bottom: 4px; }");
        styleSheet.addRule("ul, ol { margin-left: 15px; margin-top: 4px; margin-bottom: 4px; }");
        styleSheet.addRule("table { width: 100%; border-collapse: collapse; margin-top: 8px; margin-bottom: 8px; }");
        styleSheet.addRule("td, th { border: 1px solid #ccc; padding: 4px; }");
    }

    /**
     * Configures the component with custom margins and typography appropriate for the main manuscript editor.
     */
    public void configureForMainEditor() {
        HTMLDocument doc = (HTMLDocument) getDocument();
        StyleSheet styleSheet = doc.getStyleSheet();
        styleSheet.addRule("body { font-family: Arial, sans-serif; font-size: 14pt; margin: 30px; line-height: 1.4; }");
        setMargin(new Insets(20, 30, 20, 30));
    }
}
