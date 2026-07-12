# ✍️ Escriba

> A modern writing environment for authors, novelists, and world builders.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-UI-blue)
![License](https://img.shields.io/badge/License-MIT-green)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

---

## 📖 About

Escriba is a desktop writing application designed for fiction authors.

Inspired by tools such as **Scrivener**, **Obsidian**, and **Campfire**, Escriba aims to provide everything a writer needs inside a single application.

The goal is to create a distraction-free environment for planning, organizing, writing, and exporting complete novels.

---

## ✨ Features

### Writing

- Rich text editor
- Multiple chapters
- Scene management
- Word count
- Character count
- Estimated reading time
- Undo / Redo

### Project Management

- Multiple writing projects
- Chapter organization
- Project tree
- Automatic saving
- JSON project storage

### World Building

- Characters
- Locations
- Timeline
- Notes
- Goals
- Items

### Export

- Save projects
- Load projects
- JSON serialization

---

## 🚀 Planned Features

- RichTextFX editor
- Markdown support
- DOCX export
- PDF export
- EPUB export
- AI writing assistant
- Local AI integration (LM Studio)
- Grammar suggestions
- Writing statistics
- Daily writing goals
- Full-screen focus mode
- Themes
- Plugin system
- Cloud synchronization
- Version history
- Custom dictionaries

---

## 🛠️ Technologies

- Java 21
- Java Swing
- Jackson JSON
- Maven
- Git

Future:

- JavaFX
- RichTextFX
- SQLite
- Local LLM Integration

---

## 📂 Project Structure

```
src
├── app
├── model
├── service
├── storage
├── ui
└── export
```

---

## 🏗️ Architecture

Escriba follows a modular architecture.

```
Main
    │
    ▼
ProjectService
    │
    ▼
Book
    │
    ▼
Chapter
    │
    ▼
Scene
```

Persistence is isolated inside the storage layer.

```
UI
 │
 ▼
Services
 │
 ▼
Storage
 │
 ▼
JSON
```

This separation keeps the application maintainable and easy to extend.

---

## 🎯 Project Vision

Escriba is not just another text editor.

The long-term vision is to become a complete creative writing platform combining:

- Writing
- World building
- Research
- Timeline management
- AI assistance
- Professional publishing

All inside one application.

---

## 📸 Screenshots

*(Coming soon)*

---

## 📦 Installation

Clone the repository:

```bash
git clone https://github.com/yourusername/escriba.git
```

Open the project in IntelliJ IDEA or VS Code.

Run:

```bash
mvn clean package
```

or

```bash
mvn javafx:run
```

---

## 🤝 Contributing

Contributions, ideas and suggestions are welcome.

Feel free to open an issue or submit a pull request.

---

## 📜 License

This project is licensed under the MIT License.

---

## 👤 Author

**EmerSedCoder**

Passionate about software engineering, artificial intelligence and creative writing.

---

> *"Every great story deserves a great writing tool."*
