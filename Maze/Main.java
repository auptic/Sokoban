package Maze;

import java.util.*;

class State {
    int x, y;
    char[][] maze;
    Set<String> visitedStates;

    public State(int x, int y, char[][] maze, Set<String> visitedStates) {
        this.x = x;
        this.y = y;
        this.maze = maze;
        this.visitedStates = new HashSet<>(visitedStates);
        this.visitedStates.add(getStateString());
    }

    public boolean isGoal() {
        return maze[x][y] == 'G';
    }

    public List<State> getPossibleMoves() {
        List<State> moves = new ArrayList<>();
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        for (int i = 0; i < 4; i++) {
            int newX = x + dx[i];
            int newY = y + dy[i];
            if (isValidMove(newX, newY)) {
                char[][] newMaze = copyMaze();
                newMaze[x][y] = '.';
                if (maze[newX][newY] != 'G') {
                    newMaze[newX][newY] = 'P';
                }
                moves.add(new State(newX, newY, newMaze, visitedStates));
            }
        }
        return moves;
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < maze.length && y >= 0 && y < maze[0].length && maze[x][y] != 'X';
    }

    private char[][] copyMaze() {
        char[][] copy = new char[maze.length][];
        for (int i = 0; i < maze.length; i++) {
            copy[i] = maze[i].clone();
        }
        return copy;
    }

    public String getStateString() {
        StringBuilder sb = new StringBuilder();
        sb.append(x).append(",").append(y).append(";");
        for (char[] row : maze) {
        sb.append(new String(row));
        }
        return sb.toString();
    }
}

class Node {
    State state;
    Node parent;
    List<Node> children;
    int visits;
    double value;

    Set<String> exploredStates;

    public Node(State state, Node parent) {
        this.state = state;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.visits = 0;
        this.value = 0.0;
        this.exploredStates = new HashSet<>(state.visitedStates);
    }

    public Node selectChild() {
        return children.stream()
                .max(Comparator.comparingDouble(c -> c.getUCTValue(this.visits)))
                .orElse(null);
    }

    private double getUCTValue(int parentVisits) {
        double explorationParameter = Math.sqrt(2.5) //Try something between 2.5 - 5.0 for High Exploration    
        if (visits == 0) return Double.MAX_VALUE;
        return (value / visits) + explorationParameter * Math.sqrt(Math.log(parentVisits) / visits);
    }

    public void expand() {
        List<State> possibleMoves = state.getPossibleMoves();
        int maxChildren = Math.min(possibleMoves.size(), this.visits / 10 + 1); // Progressive widening
        
        for (int i = 0; i < maxChildren; i++) {
            State move = possibleMoves.get(i);
            if (!exploredStates.contains(move.getStateString())) {
            children.add(new Node(move, this));
            }
        }
    }

    public double simulate() {
        State simState = this.state;
        Random random = new Random();
        int steps = 0;
        int maxSteps = 3000;
        Set<String> newExploredStates = new HashSet<>(exploredStates);

        while (!simState.isGoal() && steps < maxSteps) {
            List<State> moves = simState.getPossibleMoves();
            if (moves.isEmpty()) {
                break;
            }

            // State nextState = moves.get(random.nextInt(moves.size()));
            // newExploredStates.add(nextState.getStateString());
            // simState = nextState;
            // steps++;

            // Select the best move based on a heuristic (e.g., Manhattan distance to the goal)
            State nextState = moves.stream()
                .min(Comparator.comparingInt(s -> getManhattanDistance(s.x, s.y, goal[0], goal[1])))
                .orElse(moves.get(new Random().nextInt(moves.size())));

            newExploredStates.add(nextState.getStateString());
            simState = nextState;
            steps++;
        }

        // Reward based on exploration and goal achievement
        double explorationReward = (double) (newExploredStates.size() - exploredStates.size()) / state.maze.length / state.maze[0].length;
        double goalReward = simState.isGoal() ? 1.0 : 0.0;

        return 0.3 * explorationReward + 0.7 * goalReward; //Adjusted weights
    }

    private int getManhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public void backpropagate(double result) {
        Node current = this;
        while (current != null) {
            current.visits++;
            current.value += result;
            current = current.parent;
        }
    }
}

class MonteCarloTreeSearch {
    public List<Node> runMCTS(State initialState, int iterations) {
        Node root = new Node(initialState, null);
        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            Node selectedNode = root;

            // Selection
            while (!selectedNode.children.isEmpty()) {
                selectedNode = selectedNode.selectChild();
            }

            // Expansion
            selectedNode.expand();

            // Simulation
            Node toSimulate = selectedNode;
            if (!selectedNode.children.isEmpty()) {
                toSimulate = selectedNode.children.get(random.nextInt(selectedNode.children.size()));
            }
            double result = toSimulate.simulate();

            // Backpropagation
            toSimulate.backpropagate(result);
        }

        // Select the path with the most explored states
        List<Node> path = new ArrayList<>();
        Node current = root;
        while (!current.children.isEmpty()) {
            current = current.children.stream()
                    .max(Comparator.comparingInt(c -> c.exploredStates.size()))
                    .orElse(null);
            path.add(current);
            if (current.state.isGoal()) break;
        }

        return path;
    }
}


public class Main {
    static int[] goal = {5, 8};

    public static void main(String[] args) {
        char[][] maze = {
                {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
                {'X', '.', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
                {'X', 'P', 'X', 'X', 'G', '.', '.', '.', '.', 'X'},
                {'X', '.', 'X', 'X', 'X', 'X', 'X', 'X', '.', 'X'},
                {'X', '.', 'X', 'X', 'X', 'X', 'X', 'X', '.', 'X'},
                {'X', '.', '.', '.', '.', '.', '.', '.', '.', 'X'},
                {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
        };

        State initialState = new State(1, 1, maze, new HashSet<>());
        MonteCarloTreeSearch mcts = new MonteCarloTreeSearch();
        List<Node> path = mcts.runMCTS(initialState, 10000);

        // Print out the resulting path
        for (Node node : path) {
            printMaze(node.state.maze);
        }
    }

    public static void printMaze(char[][] maze) {
        for (char[] row : maze) {
            for (char cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
