import java.util.*;

class Node implements Comparable<Node> {
    Node parent;
    char dir;
    int i, j;
    int g;
    HashMap<Integer, int[]> boxPositions;

    public Node(Node parent, char dir, int i, int j, int g, HashMap<Integer, int[]> boxPositions) {
        this.parent = parent;
        this.dir = dir;
        this.i = i;
        this.j = j;
        this.g = g;
        this.boxPositions = new HashMap<>(boxPositions);
    }

    @Override
    public int compareTo(Node o) {
        return (this.g + this.heuristic()) - (o.g + o.heuristic());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return i == node.i && j == node.j && boxPositions.equals(node.boxPositions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(i, j, boxPositions);
    }

    public List<Node> children() {
        List<Node> children = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        char[] dirChars = {'U', 'D', 'L', 'R'};

        for (int d = 0; d < 4; d++) {
            int ni = i + directions[d][0];
            int nj = j + directions[d][1];

            if (SokobanSolver.board[ni][nj] != 'X') {
                HashMap<Integer, int[]> newBoxPositions = new HashMap<>(boxPositions);
                int key = SokobanSolver.cantorPair(ni, nj);

                if (newBoxPositions.containsKey(key)) {
                    int nni = ni + directions[d][0];
                    int nnj = nj + directions[d][1];
                    if (SokobanSolver.board[nni][nnj] != 'X' && !newBoxPositions.containsKey(SokobanSolver.cantorPair(nni, nnj))) {
                        newBoxPositions.put(SokobanSolver.cantorPair(nni, nnj), new int[]{nni, nnj});
                        newBoxPositions.remove(key);
                        children.add(new Node(this, dirChars[d], ni, nj, g + 1, newBoxPositions));
                    }
                } else {
                    children.add(new Node(this, dirChars[d], ni, nj, g + 1, newBoxPositions));
                }
            }
        }
        return children;
    }

    public boolean hasDeadlock() {
        for (int[] box : boxPositions.values()) {
            if (isBoxStuck(box[0], box[1]) && !SokobanSolver.goalPositions.containsKey(SokobanSolver.cantorPair(box[0], box[1]))) {
                return true;
            }
        }
        return hasGroupStuck();
    }

    private boolean hasGroupStuck() {
        Set<Integer> checkedBoxes = new HashSet<>();
        for (int[] box : boxPositions.values()) {
            int boxKey = SokobanSolver.cantorPair(box[0], box[1]);
            if (!checkedBoxes.contains(boxKey) && !SokobanSolver.goalPositions.containsKey(boxKey)) {
                Set<Integer> group = new HashSet<>();
                if (dfsGroupStuck(box[0], box[1], group) && !canMoveAny(group)) {
                    return true;
                }
                checkedBoxes.addAll(group);
            }
        }
        return false;
    }

    private boolean dfsGroupStuck(int bi, int bj, Set<Integer> group) {
        int key = SokobanSolver.cantorPair(bi, bj);
        if (group.contains(key) || SokobanSolver.goalPositions.containsKey(key)) {
            return false;
        }
        if (!boxPositions.containsKey(key)) {
            return false;
        }

        group.add(key);

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int ni = bi + dir[0];
            int nj = bj + dir[1];
            dfsGroupStuck(ni, nj, group);
        }

        return true;
    }

    private boolean canMoveAny(Set<Integer> group) {
        for (int key : group) {
            int[] pos = SokobanSolver.inverseCantorPair(key);
            if (canMoveBox(pos[0], pos[1], group)) {
                return true;
            }
        }
        return false;
    }

    private boolean canMoveBox(int bi, int bj, Set<Integer> group) {
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int ni = bi + dir[0];
            int nj = bj + dir[1];
            int oppositeI = bi - dir[0];
            int oppositeJ = bj - dir[1];

            if (SokobanSolver.board[ni][nj] != 'X' &&
                    !group.contains(SokobanSolver.cantorPair(ni, nj)) &&
                    (SokobanSolver.board[oppositeI][oppositeJ] != 'X' ||
                            SokobanSolver.goalPositions.containsKey(SokobanSolver.cantorPair(oppositeI, oppositeJ))) &&
                    !group.contains(SokobanSolver.cantorPair(oppositeI, oppositeJ))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoxStuck(int bi, int bj) {
        boolean horizontalBlocked = (isBlocked(bi, bj - 1) && isBlocked(bi, bj + 1));
        boolean verticalBlocked = (isBlocked(bi - 1, bj) && isBlocked(bi + 1, bj));
        return horizontalBlocked && verticalBlocked;
    }

    private boolean isBlocked(int i, int j) {
        return SokobanSolver.board[i][j] == 'X' ||
                (boxPositions.containsKey(SokobanSolver.cantorPair(i, j)) &&
                        !SokobanSolver.goalPositions.containsKey(SokobanSolver.cantorPair(i, j)));
    }

    public int heuristic() {
        int totalDistance = 0;
        int minBoxDistance = Integer.MAX_VALUE;

        for (Map.Entry<Integer, int[]> entry : boxPositions.entrySet()) {
            int[] box = entry.getValue();
            int minGoalDistance = Integer.MAX_VALUE;

            for (int[] goal : SokobanSolver.goalPositions.values()) {
                int distance = Math.abs(box[0] - goal[0]) + Math.abs(box[1] - goal[1]);
                minGoalDistance = Math.min(minGoalDistance, distance);
            }

            totalDistance += minGoalDistance;

            if (!SokobanSolver.goalPositions.containsKey(entry.getKey())) {
                int boxDistance = Math.abs(i - box[0]) + Math.abs(j - box[1]);
                minBoxDistance = Math.min(minBoxDistance, boxDistance);
            }
        }

        return totalDistance + (minBoxDistance == Integer.MAX_VALUE ? 0 : minBoxDistance);
    }
}

class SokobanSolver {
    /*
    static char[][] board = {
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
            {'X', 'E', 'E', '.', '.', 'X', '.', '.', '.', '.', '.', 'X', 'X', 'X'},
            {'X', 'E', 'E', '.', '.', 'X', '.', '#', '.', '.', '#', '.', '.', 'X'},
            {'X', 'E', 'E', '.', '.', 'X', '#', 'X', 'X', 'X', 'X', '.', '.', 'X'},
            {'X', 'E', 'E', '.', '.', '.', '.', 'S', '.', 'X', 'X', '.', '.', 'X'},
            {'X', 'E', 'E', '.', '.', 'X', '.', 'X', '.', '.', '#', '.', 'X', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', '.', 'X', 'X', '#', '.', '#', '.', 'X'},
            {'X', 'X', 'X', '.', '#', '.', '.', '#', '.', '#', '.', '#', '.', 'X'},
            {'X', 'X', 'X', '.', '.', '.', '.', 'X', '.', '.', '.', '.', '.', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'}
    };
     */

    static char[][] board = {
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
            {'X', 'S', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', '.', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', '.', '.', '.', '.', '#', 'E', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'}
    };

    static HashMap<Integer, int[]> goalPositions = getGoalPositions();
    static HashMap<Integer, int[]> initBoxPositions = getInitBoxPositions();
    static int[] sokoban = getSokobanPosition();

    public static void main(String[] args) {
        Node solution = a_star();
        if (solution == null)
            System.out.println("No solution.");
        else
            System.out.println(getSequence(solution));
    }

    private static Node a_star() {
        Node node = new Node(null, '\0', sokoban[0], sokoban[1], 0, initBoxPositions);

        PriorityQueue<Node> open = new PriorityQueue<>();
        HashSet<Node> visited = new HashSet<>();

        open.add(node);
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.hasDeadlock())
                continue;
            if (current.heuristic() == 0)
                return current;

            for (Node c : current.children()) {
                if (!visited.contains(c))
                    open.add(c);
            }
            visited.add(current);
        }

        return null;
    }

    private static String getSequence(Node solution) {
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

    private static int[] getSokobanPosition() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 'S')
                    return new int[]{i, j};
            }
        }
        return new int[]{-1, -1};
    }

    private static HashMap<Integer, int[]> getGoalPositions() {
        HashMap<Integer, int[]> goalPositions = new HashMap<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == 'E')
                    goalPositions.put(cantorPair(i, j), new int[]{i, j});
            }
        }
        return goalPositions;
    }

    private static HashMap<Integer, int[]> getInitBoxPositions() {
        HashMap<Integer, int[]> boxPositions = new HashMap<>();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (board[i][j] == '#')
                    boxPositions.put(cantorPair(i, j), new int[]{i, j});
            }
        }
        return boxPositions;
    }

    public static int cantorPair(int x, int y) {
        return (x + y) * (x + y + 1) / 2 + y;
    }

    public static int[] inverseCantorPair(int z) {
        int w = (int) Math.floor((Math.sqrt(8 * z + 1) - 1) / 2);
        int t = (w * (w + 1)) / 2;
        int y = z - t;
        int x = w - y;
        return new int[]{x, y};
    }
}