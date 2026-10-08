package in.radhikatries;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class TrieSearchUI extends JFrame {

    private final AutoCompleteTrie trie;
    private DatabaseManager db;

    private JTextField searchField;
    private DefaultListModel<String> suggestionListModel;
    private JList<String> suggestionList;
    private JLabel statusLabel;
    private JLabel countLabel;

    public TrieSearchUI() {
        trie = new AutoCompleteTrie();
        initDatabaseAndData();
        setupUI();
    }

    private void initDatabaseAndData() {
        try {
            db = new DatabaseManager();
            Map<String, Integer> saved = db.loadAll();
            if (saved == null || saved.isEmpty()) {
                String[] seed = {"java", "javascript", "javelin", "python", "pytorch",
                        "program", "programming", "project", "react", "redux", "radhika"};
                for (String w : seed) {
                    trie.add(w, 1);
                    if (db != null) {
                        try { db.save(w, 1); } catch (Exception ignored) {}
                    }
                }
            } else {
                saved.forEach(trie::add);
            }
        } catch (Exception e) {
            System.err.println("DB Connection note: Running in-memory Trie mode (" + e.getMessage() + ")");
            // Fallback seed words in case MySQL is offline
            String[] seed = {"java", "javascript", "javelin", "python", "pytorch",
                    "program", "programming", "project", "react", "redux", "radhika"};
            for (String w : seed) {
                trie.add(w, 1);
            }
        }
    }

    private void setupUI() {
        setTitle("Google Search Engine - Trie Autocomplete & MySQL");
        setSize(650, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(24, 27, 33));
        setLayout(new BorderLayout(15, 15));

        // 1. Top Header with Google-like Title & Search Bar
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(new Color(24, 27, 33));
        topPanel.setBorder(new EmptyBorder(25, 30, 10, 30));

        JLabel titleLabel = new JLabel("🔍 Smart Search Engine");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(240, 246, 252));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Real-time Trie Prefix Search with O(L) Lookups & MySQL Persistence");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(new Color(139, 148, 158));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(subLabel);
        topPanel.add(Box.createVerticalStrut(20));

        // Search Input Box
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchField.setBackground(new Color(33, 38, 45));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(56, 139, 253), 2, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        topPanel.add(searchField);

        add(topPanel, BorderLayout.NORTH);

        // 2. Suggestions List (Live Dropdown)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(24, 27, 33));
        centerPanel.setBorder(new EmptyBorder(0, 30, 0, 30));

        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel);
        suggestionList.setFont(new Font("Consolas", Font.PLAIN, 15));
        suggestionList.setBackground(new Color(13, 17, 23));
        suggestionList.setForeground(new Color(88, 166, 255));
        suggestionList.setSelectionBackground(new Color(30, 41, 59));
        suggestionList.setSelectionForeground(Color.WHITE);
        suggestionList.setFixedCellHeight(35);
        suggestionList.setBorder(new EmptyBorder(8, 12, 8, 12));

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(48, 54, 61), 1));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Action Buttons and Status
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(new Color(24, 27, 33));
        bottomPanel.setBorder(new EmptyBorder(15, 30, 20, 30));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonBar.setBackground(new Color(24, 27, 33));

        JButton btnAdd = createStyledButton("➕ Add / Search Word", new Color(35, 134, 54));
        JButton btnDelete = createStyledButton("🗑️ Delete Word", new Color(218, 54, 51));
        JButton btnClear = createStyledButton("Clear", new Color(48, 54, 61));

        buttonBar.add(btnAdd);
        buttonBar.add(btnDelete);
        buttonBar.add(btnClear);

        JPanel statusContainer = new JPanel(new BorderLayout());
        statusContainer.setBackground(new Color(24, 27, 33));
        statusContainer.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusLabel = new JLabel("● Ready: Start typing above to get live instant suggestions");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(63, 185, 80));

        countLabel = new JLabel("Words in Trie: " + trie.size());
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        countLabel.setForeground(new Color(139, 148, 158));

        statusContainer.add(statusLabel, BorderLayout.WEST);
        statusContainer.add(countLabel, BorderLayout.EAST);

        bottomPanel.add(buttonBar, BorderLayout.NORTH);
        bottomPanel.add(statusContainer, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Real-time Keystroke Listener
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateSuggestions(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateSuggestions(); }
        });

        // Click suggestion to auto-fill
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selected = suggestionList.getSelectedValue();
                    if (selected != null && selected.contains(" (")) {
                        String word = selected.substring(0, selected.indexOf(" ("));
                        searchField.setText(word);
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> handleAddWord());
        searchField.addActionListener(e -> handleAddWord()); // Press Enter
        btnDelete.addActionListener(e -> handleDeleteWord());
        btnClear.addActionListener(e -> {
            searchField.setText("");
            suggestionListModel.clear();
            statusLabel.setText("Cleared search box.");
        });

        updateSuggestions();
    }

    private void updateSuggestions() {
        String query = searchField.getText().trim().toLowerCase();
        suggestionListModel.clear();

        if (query.isEmpty()) {
            statusLabel.setText("● Type something (e.g. 'ja', 'py', 'prog')...");
            return;
        }

        long startTime = System.nanoTime();
        List<String> suggestions = trie.suggest(query, 8);
        long duration = (System.nanoTime() - startTime) / 1000; // in microseconds

        if (suggestions == null || suggestions.isEmpty()) {
            suggestionListModel.addElement("No matching suggestions found for '" + query + "'");
            statusLabel.setText("Lookup time: " + duration + " µs (Not found)");
        } else {
            for (String s : suggestions) {
                suggestionListModel.addElement(s);
            }
            statusLabel.setText("Found " + suggestions.size() + " suggestions in " + duration + " µs (Instant Trie Lookup)");
        }
    }

    private void handleAddWord() {
        String word = searchField.getText().trim().toLowerCase();
        if (word.isEmpty()) return;

        trie.add(word, 1);
        if (db != null) {
            try {
                db.save(word, 1);
            } catch (Exception ex) {
                System.err.println("DB Save error: " + ex.getMessage());
            }
        }
        countLabel.setText("Words in Trie: " + trie.size());
        statusLabel.setText("Saved '" + word + "' to Trie & MySQL (Frequency incremented)");
        updateSuggestions();
    }

    private void handleDeleteWord() {
        String word = searchField.getText().trim().toLowerCase();
        if (word.isEmpty()) return;

        if (trie.remove(word)) {
            if (db != null) {
                try {
                    db.delete(word);
                } catch (Exception ex) {
                    System.err.println("DB Delete error: " + ex.getMessage());
                }
            }
            countLabel.setText("Words in Trie: " + trie.size());
            statusLabel.setText("Deleted '" + word + "' from Trie and MySQL.");
            updateSuggestions();
        } else {
            statusLabel.setText("Word '" + word + "' not found to delete.");
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TrieSearchUI().setVisible(true);
        });
    }
}
