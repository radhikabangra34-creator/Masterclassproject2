package in.radhikatries;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseManager implements AutoCloseable {

    private static final String SERVER_URL = "jdbc:mysql://localhost:3306/"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/trie_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Radhika*123";

    private final Connection conn;

    public DatabaseManager() throws SQLException {
        try (Connection tmp = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement st = tmp.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS trie_db");
        }
        conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        createTable();
        System.out.println("Connected to MySQL database trie_db, table 'words' is ready.");
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS words ("
                + "word VARCHAR(100) PRIMARY KEY, "
                + "freq INT NOT NULL DEFAULT 1)";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    public Map<String, Integer> loadAll() throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT word, freq FROM words")) {
            while (rs.next()) {
                result.put(rs.getString("word"), rs.getInt("freq"));
            }
        }
        return result;
    }

    public void save(String word, int times) throws SQLException {
        String sql = "INSERT INTO words (word, freq) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE freq = freq + ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, word);
            ps.setInt(2, times);
            ps.setInt(3, times);
            ps.executeUpdate();
        }
    }

    public void delete(String word) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM words WHERE word = ?")) {
            ps.setString(1, word);
            ps.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}