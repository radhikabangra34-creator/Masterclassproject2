package in.radhikatries;
import java.sql.SQLException;
import java.util.*;
public class AppMain {
    public static void main(String[] args) {
        AutoCompleteTrie trie = new AutoCompleteTrie();
        Scanner sc = new Scanner(System.in);
        try (DatabaseManager db = new DatabaseManager()) {
            // 1. DB se words trie me load karo (DB khali ho to seed words save karo)
            Map<String, Integer> saved = db.loadAll();
            if (saved.isEmpty()) {
                String[] seed = {"java", "javascript", "java", "javelin", "python", "pytorch",
                        "python", "python", "program", "programming", "programming", "project"};
                for (String w : seed) {
                    trie.add(w, 1);
                    db.save(w, 1);
                }
            } else {
                saved.forEach(trie::add);
            }
            System.out.println("Loaded " + trie.size() + " words from database.");

            // 2. Menu: har change trie AUR database dono me hoga
            while (true) {
                System.out.println("\n===== Trie Autocomplete =====");
                System.out.println("1. Add word   2. Search/frequency   3. Autocomplete   4. Delete   5. Count   0. Exit");
                System.out.print("Choice: ");
                if (!sc.hasNextLine()) break;
                String c = sc.nextLine().trim();
                switch (c) {
                    case "1" -> {
                        System.out.print("Word: ");
                        String w = sc.nextLine().trim().toLowerCase();
                        if (w.isEmpty() || w.length() > 100) {
                            System.out.println("Invalid word.");
                        } else {
                            trie.add(w, 1);
                            db.save(w, 1);
                            System.out.println("Added.");
                        }
                    }
                    case "2" -> {
                        System.out.print("Word: ");
                        int f = trie.frequency(sc.nextLine());
                        System.out.println(f > 0 ? "Found, used " + f + " time(s)" : "Not found");
                    }
                    case "3" -> {
                        System.out.print("Prefix: ");
                        List<String> s = trie.suggest(sc.nextLine(), 5);
                        System.out.println(s.isEmpty() ? "No suggestions" : "Suggestions: " + s);
                    }
                    case "4" -> {
                        System.out.print("Word: ");
                        String w = sc.nextLine().trim().toLowerCase();
                        if (trie.remove(w)) {
                            db.delete(w);
                            System.out.println("Deleted.");
                        } else {
                            System.out.println("Not found");
                        }
                    }
                    case "5" -> System.out.println("Total words: " + trie.size());
                    case "0" -> {
                        System.out.println("Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
class AutoCompleteTrie {
    private static class Node {
        Map<Character, Node> next = new TreeMap<>();
        int freq;
    }
    private final Node root = new Node();
    private int totalWords = 0;
    public void add(String word, int times) {
        word = word.toLowerCase().trim();
        if (word.isEmpty()) return;
        Node curr = root;
        for (char ch : word.toCharArray())
            curr = curr.next.computeIfAbsent(ch, k -> new Node());
        if (curr.freq == 0) totalWords++;
        curr.freq += times;
    }
    public boolean remove(String word) {
        Node n = find(word.toLowerCase());
        if (n == null || n.freq == 0)
            return false;
        n.freq = 0;
        totalWords--;
        return true;
    }
    public int frequency(String word) {
        Node n = find(word.toLowerCase());
        return n == null ? 0 : n.freq;
    }

    public List<String> suggest(String prefix, int k) {
        prefix = prefix.toLowerCase();
        Node start = find(prefix);
        if (start == null) return List.of();

        List<Map.Entry<String, Integer>> found = new ArrayList<>();
        collect(start, new StringBuilder(prefix), found);
        found.sort((a, b) -> b.getValue() != a.getValue().intValue()
                ? b.getValue() - a.getValue()
                : a.getKey().compareTo(b.getKey()));

        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, found.size()); i++)
            out.add(found.get(i).getKey() + " (" + found.get(i).getValue() + ")");
        return out;
    }

    public int size() {
        return totalWords;
    }

    private void collect(Node node, StringBuilder sb, List<Map.Entry<String, Integer>> out) {
        if (node.freq > 0) out.add(Map.entry(sb.toString(), node.freq));
        for (Map.Entry<Character, Node> e : node.next.entrySet()) {
            sb.append(e.getKey());
            collect(e.getValue(), sb, out);
            sb.setLength(sb.length() - 1);
        }
    }

    private Node find(String s) {
        Node curr = root;
        for (char ch : s.toCharArray()) {
            curr = curr.next.get(ch);
            if (curr == null) return null;
        }
        return curr;
    }
}