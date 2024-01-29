import java.util.*;

class Location {
    int x, y;

    public Location() {
    }

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class Box extends Location {
    int h;

    public Box(int x, int y) {
        super(x, y);
        this.h = Integer.MAX_VALUE;

        for (Location exit : Main.exits) {
            this.h = Math.min(this.h, Math.abs(exit.x - x) + Math.abs(exit.y - y));
        }
    }
}

class Node implements Comparable<Node> {
    Node parent;

    int x, y;

    int g, h;

    char dir;

    TreeMap<String, Box> boxes;

    public Node(Node parent, int x, int y, int g, int h, char dir, TreeMap<String, Box> boxes) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.g = g;
        this.h = h;
        this.dir = dir;
        this.boxes = new TreeMap<>(boxes);
    }

    public void setH() {
        int h = 0;
        for (Box box : boxes.values()) {
            h += box.h;
        }
        this.h = h;
    }

    public List<Node> children() {
        List<Node> children = new ArrayList<>();

        // UP
        if (x - 1 >= 0 && Main.grid[x - 1][y] != 'X') {
            // If UP is box we must move the box if we can
            Box b = boxes.get((x - 1) + ":" + y);
            Box b2 = boxes.get((x - 2) + ":" + y);
            if (b != null) {
                if (x - 2 >= 0 && b2 == null && Main.grid[x - 2][y] != 'X') {
                    Node node = new Node(this, x - 1, y, this.g + 1, this.h, 'U', boxes);
                    TreeMap<String, Box> boxes = node.boxes;
                    boxes.remove((x - 1) + ":" + y);
                    Box other = new Box(x - 2, y);
                    boxes.put((x - 2) + ":" + y, other);
                    node.h = this.h - b.h + other.h;

                    children.add(node);
                }

            } else {// Box did not move so heuristic remains the same
                children.add(new Node(this, x - 1, y, this.g + 1, this.h, 'U', boxes));
            }
        }

        // DOWN
        if (x + 1 < Main.grid.length && Main.grid[x + 1][y] != 'X') {
            // If UP is box we must move the box if we can
            Box b = boxes.get((x + 1) + ":" + y);
            Box b2 = boxes.get((x + 2) + ":" + y);
            if (b != null) {
                if (x + 2 < Main.grid.length && b2 == null && Main.grid[x + 2][y] != 'X') {
                    Node node = new Node(this, x + 1, y, this.g + 1, this.h, 'D', boxes);
                    TreeMap<String, Box> boxes = node.boxes;
                    boxes.remove((x + 1) + ":" + y);
                    Box other = new Box(x + 2, y);
                    boxes.put((x + 2) + ":" + y, other);
                    node.h = this.h - b.h + other.h;

                    children.add(node);
                }
            } else {// Box did not move so heuristic remains the same
                children.add(new Node(this, x + 1, y, this.g + 1, this.h, 'D', boxes));
            }
        }

        // LEFT
        if (y - 1 >= 0 && Main.grid[x][y - 1] != 'X') {
            // If UP is box we must move the box if we can
            Box b = boxes.get(x + ":" + (y - 1));
            Box b2 = boxes.get(x + ":" + (y - 2));
            if (b != null) {
                if (y - 2 >= 0 && b2 == null && Main.grid[x][y - 2] != 'X') {
                    Node node = new Node(this, x, y - 1, this.g + 1, this.h, 'L', boxes);
                    TreeMap<String, Box> boxes = node.boxes;
                    boxes.remove(x + ":" + (y - 1));
                    Box other = new Box(x, (y - 2));
                    boxes.put(x + ":" + (y - 2), other);
                    node.h = this.h - b.h + other.h;

                    children.add(node);
                }
            } else {// Box did not move so heuristic remains the same
                children.add(new Node(this, x, y - 1, this.g + 1, this.h, 'L', boxes));
            }
        }


        // RIGHT
        if (y + 1 < Main.grid[x].length && Main.grid[x][y + 1] != 'X') {
            // If UP is box we must move the box if we can
            Box b = boxes.get(x + ":" + (y + 1));
            Box b2 = boxes.get(x + ":" + (y + 2));
            if (b != null) {
                if (y + 2 < Main.grid[x].length && b2 == null && Main.grid[x][y + 2] != 'X') {
                    Node node = new Node(this, x, y + 1, this.g + 1, this.h, 'R', boxes);
                    TreeMap<String, Box> boxes = node.boxes;
                    boxes.remove(x + ":" + (y + 1));
                    Box other = new Box(x, (y + 2));
                    boxes.put(x + ":" + (y + 2), other);
                    node.h = this.h - b.h + other.h;

                    children.add(node);
                }
            } else {// Box did not move so heuristic remains the same
                children.add(new Node(this, x, y + 1, this.g + 1, this.h, 'R', boxes));
            }
        }


        return children;
    }

    @Override
    public int compareTo(Node o) {
        int f1 = this.g + this.h;
        int f2 = o.g + o.h;

        if (f1 == f2)
            return o.g - this.g;
        return f1 - f2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(x).append(":").append(y).append("\n");
        for (String key : boxes.keySet()) {
            sb.append(key).append(" ");
        }

        return sb.toString();
    }
}

public class Main {
    /*
    static char[][] grid = {
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
            {'X', 'X', 'X', 'O', 'O', 'O', 'X', 'X'},
            {'X', '.', 'S', '#', 'O', 'O', 'X', 'X'},
            {'X', 'X', 'X', 'O', '#', '.', 'X', 'X'},
            {'X', '.', 'X', 'X', '#', 'O', 'X', 'X'},
            {'X', 'O', 'X', 'O', '.', 'O', 'X', 'X'},
            {'X', '#', 'O', '#', '#', '#', '.', 'X'},
            {'X', 'O', 'O', 'O', '.', 'O', 'O', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'}
    };*/

    static char[][] grid = {
            {'S', 'O', 'O', 'O', 'O', '#', '.'},
            {'O', 'O', 'O', 'O', 'O', '#', '.'},
            {'O', 'O', 'O', 'O', 'O', '#', '.'}
    };

    /*
     static char[][] grid = {
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
            {'X', '.', '.', 'O', 'O', 'X', 'O', 'O', 'O', 'O', 'O', 'X', 'X', 'X'},
            {'X', '.', '.', 'O', 'O', 'X', 'O', '#', 'O', 'O', '#', 'O', 'O', 'X'},
            {'X', '.', '.', 'O', 'O', 'X', '#', 'X', 'X', 'X', 'X', 'O', 'O', 'X'},
            {'X', '.', '.', 'O', 'O', 'O', 'O', 'S', 'O', 'X', 'X', 'O', 'O', 'X'},
            {'X', '.', '.', 'O', 'O', 'X', 'O', 'X', 'O', 'O', '#', 'O', 'X', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'O', 'X', 'X', '#', 'O', '#', 'O', 'X'},
            {'X', 'X', 'X', 'O', '#', 'O', 'O', '#', 'O', '#', 'O', '#', 'O', 'X'},
            {'X', 'X', 'X', 'O', 'O', 'O', 'O', 'X', 'O', 'O', 'O', 'O', 'O', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'}
    };


     */
    static List<Location> exits;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        exits = findExits();
        exits.add(new Location(6, 3));
        TreeMap<String, Box> boxes = findBoxes();

        int x = 0, y = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == 'S') {
                    x = i;
                    y = j;
                }
            }
        }

        Node solution = a_star(new Node(null, x, y, 0, 0, '\0', boxes));
        if (solution == null)
            System.out.println("No solution.");
        else
            System.out.println(getSequenceOf(solution));

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Time taken: " + totalTime + " milliseconds");
    }

    private static Node a_star(Node node) {
        node.setH();

        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<String> visited = new HashSet<>();

        open.add(node);
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.h == 0)
                return current;

            for (Node c : current.children()) {
                if (!visited.contains(c.toString()))
                    open.add(c);
            }
            visited.add(current.toString());
        }

        return null;
    }

    private static String getSequenceOf(Node solution) {
        StringBuilder sequence = new StringBuilder();
        Stack<Character> stack = new Stack<>();

        while (solution.parent != null) {
            stack.add(solution.dir);
            solution = solution.parent;
        }

        while (!stack.isEmpty()) {
            sequence.append(stack.pop());
        }
        return sequence.toString();
    }

    private static TreeMap<String, Box> findBoxes() {
        TreeMap<String, Box> boxes = new TreeMap<>();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == '#')
                    boxes.put(i + ":" + j, new Box(i, j));
            }
        }

        return boxes;
    }

    private static List<Location> findExits() {
        List<Location> exits = new ArrayList<>();

        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j] == '.')
                    exits.add(new Location(i, j));
            }
        }

        return exits;
    }
}