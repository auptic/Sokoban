package Sokoban;

import java.util.*;

class Population {
    int population_size;
    double initial_mutation_rate;
    Element[] population;
    Element bestElement;
    Random random;
    int stagnationCounter;
    double bestFitness;

    public Population(int population_size, double initial_mutation_rate, int dnaLength) {
        this.population_size = population_size;
        this.initial_mutation_rate = initial_mutation_rate;
        this.bestElement = null;
        this.random = new Random();
        this.stagnationCounter = 0;
        this.bestFitness = Double.NEGATIVE_INFINITY;

        population = new Element[population_size];
        for (int i = 0; i < population_size; i++) {
            population[i] = Element.getRandom(dnaLength);
        }
    }

    public void evaluate_fitness() {
        for (Element element : population) {
            double fitness = 0.0;
            int[] sokobanPosition = Main.sokoban.clone();
            HashMap<Integer, int[]> boxPositions = new HashMap<>(Main.initBoxPositions);

            for (int i = 0; i < element.dna.length; i++) {
                char move = element.dna[i];
                int[] newSokobanPosition = moveSokoban(sokobanPosition, move);

                if (isValidMove(newSokobanPosition, boxPositions)) {
                    sokobanPosition = newSokobanPosition;
                    updateBoxPositions(sokobanPosition, boxPositions, move);

                    // Penalize each move slightly to encourage shorter solutions
                    fitness -= 0.5;

                    // Calculate minimum distance from Sokoban to the closest box
                    int minBoxDistance = Integer.MAX_VALUE;
                    for (int[] boxPos : boxPositions.values()) {
                        int boxDistance = Math.abs(sokobanPosition[0] - boxPos[0]) + Math.abs(sokobanPosition[1] - boxPos[1]);
                        minBoxDistance = Math.min(minBoxDistance, boxDistance);
                    }

                    // Reward being closer to boxes
                    fitness += boxPositions.size() / (minBoxDistance + 0.0000001); // +1 to avoid division by zero

                    // Check if a box was pushed
                    int boxPair = Main.cantorPair(newSokobanPosition[0], newSokobanPosition[1]);
                    if (boxPositions.containsKey(boxPair)) {
                        int[] newBoxPos = moveSokoban(newSokobanPosition, move);
                        int newBoxPair = Main.cantorPair(newBoxPos[0], newBoxPos[1]);

                        // Reward pushing boxes towards goals
                        if (Main.goalPositions.containsKey(newBoxPair)) {
                            fitness += 100;
                        } else if (isCloserToGoal(newBoxPos, boxPositions.get(boxPair))) {
                            fitness += 20;
                        } else {
                            // Penalize pushing boxes away from goals
                            fitness -= 10;
                        }

                        // Check for deadlocks
                        if (isDeadlock(boxPositions)) {
                            fitness -= 1000;
                            break;
                        }
                    }
                } else {
                    element.dna[i] = ' ';
                    fitness = 0; // Penalty for invalid moves
                    break; // Stop evaluating further moves
                }

                // Penalize based on the Manhattan distance of boxes to goals
                fitness -= calculateTotalManhattanDistance(boxPositions) * 0.1;
            }

            int placedBoxes = countPlacedBoxes(boxPositions);
            fitness += placedBoxes * 1000; // Reward for boxes placed on goals

            // Terminate if a solution is found
            if (placedBoxes == Main.goalPositions.size()) {
                element.fitness = Double.MAX_VALUE;
                this.bestElement = element;
                return;
            } else {
                element.fitness = fitness;
            }
        }
    }


    private boolean isDeadlock(HashMap<Integer, int[]> boxPositions) {
        for (int[] boxPos : boxPositions.values()) {
            if (!Main.goalPositions.containsKey(Main.cantorPair(boxPos[0], boxPos[1]))) {
                if (isCornerDeadlock(boxPos) || isWallDeadlock(boxPos, boxPositions)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCornerDeadlock(int[] boxPos) {
        int x = boxPos[0], y = boxPos[1];
        return (Main.board[x - 1][y] == 'X' && Main.board[x][y - 1] == 'X') ||
                (Main.board[x - 1][y] == 'X' && Main.board[x][y + 1] == 'X') ||
                (Main.board[x + 1][y] == 'X' && Main.board[x][y - 1] == 'X') ||
                (Main.board[x + 1][y] == 'X' && Main.board[x][y + 1] == 'X');
    }

    private boolean isWallDeadlock(int[] boxPos, HashMap<Integer, int[]> boxPositions) {
        int x = boxPos[0], y = boxPos[1];
        boolean horizontalWall = (Main.board[x - 1][y] == 'X' && Main.board[x + 1][y] == 'X');
        boolean verticalWall = (Main.board[x][y - 1] == 'X' && Main.board[x][y + 1] == 'X');

        if (horizontalWall) {
            return boxPositions.containsKey(Main.cantorPair(x, y - 1)) ||
                    boxPositions.containsKey(Main.cantorPair(x, y + 1));
        }
        if (verticalWall) {
            return boxPositions.containsKey(Main.cantorPair(x - 1, y)) ||
                    boxPositions.containsKey(Main.cantorPair(x + 1, y));
        }
        return false;
    }

    private boolean isCloserToGoal(int[] newPos, int[] oldPos) {
        int oldMinDistance = Integer.MAX_VALUE;
        int newMinDistance = Integer.MAX_VALUE;

        for (int[] goalPos : Main.goalPositions.values()) {
            int oldDistance = Math.abs(oldPos[0] - goalPos[0]) + Math.abs(oldPos[1] - goalPos[1]);
            int newDistance = Math.abs(newPos[0] - goalPos[0]) + Math.abs(newPos[1] - goalPos[1]);

            oldMinDistance = Math.min(oldMinDistance, oldDistance);
            newMinDistance = Math.min(newMinDistance, newDistance);
        }

        return newMinDistance < oldMinDistance;
    }

    private int[] moveSokoban(int[] position, char move) {
        switch (move) {
            case 'U':
                return new int[]{position[0] - 1, position[1]};
            case 'D':
                return new int[]{position[0] + 1, position[1]};
            case 'L':
                return new int[]{position[0], position[1] - 1};
            case 'R':
                return new int[]{position[0], position[1] + 1};
            default:
                return position;
        }
    }

    private boolean isValidMove(int[] newPos, HashMap<Integer, int[]> boxPositions) {
        int x = newPos[0];
        int y = newPos[1];

        if (Main.board[x][y] == 'X') return false; // Wall

        int pair = Main.cantorPair(x, y);
        if (boxPositions.containsKey(pair)) {
            int[] newBoxPos = moveSokoban(newPos, dnaToChar(newPos));
            int newPair = Main.cantorPair(newBoxPos[0], newBoxPos[1]);
            if (Main.board[newBoxPos[0]][newBoxPos[1]] == 'X' || boxPositions.containsKey(newPair)) {
                return false; // Invalid move (into a wall or another box)
            }
        }

        return true;
    }

    private void updateBoxPositions(int[] sokobanPos, HashMap<Integer, int[]> boxPositions, char move) {
        int pair = Main.cantorPair(sokobanPos[0], sokobanPos[1]);
        if (boxPositions.containsKey(pair)) {
            int[] newBoxPos = moveSokoban(sokobanPos, move);
            boxPositions.remove(pair);
            boxPositions.put(Main.cantorPair(newBoxPos[0], newBoxPos[1]), newBoxPos);
        }
    }


    private char dnaToChar(int[] pos) {
        // Translate pos changes back to DNA directions.
        if (pos[0] == -1) return 'U';
        if (pos[0] == 1) return 'D';
        if (pos[1] == -1) return 'L';
        return 'R';
    }

    private int calculateTotalManhattanDistance(HashMap<Integer, int[]> boxPositions) {
        int totalDistance = 0;
        for (int[] boxPos : boxPositions.values()) {
            int minDistance = Integer.MAX_VALUE;
            for (int[] goalPos : Main.goalPositions.values()) {
                int distance = Math.abs(boxPos[0] - goalPos[0]) + Math.abs(boxPos[1] - goalPos[1]);
                minDistance = Math.min(minDistance, distance);
            }
            totalDistance += minDistance;
        }
        return totalDistance;
    }

    private int countPlacedBoxes(HashMap<Integer, int[]> boxPositions) {
        int count = 0;
        for (int[] boxPos : boxPositions.values()) {
            if (Main.goalPositions.containsKey(Main.cantorPair(boxPos[0], boxPos[1]))) {
                count++;
            }
        }
        return count;
    }

    public Element getHighestElement() {
        if (bestElement != null) return bestElement;

        Element highest = population[0];
        for (Element element : population) {
            if (element.fitness > highest.fitness) {
                highest = element;
            }
        }
        return highest;
    }

    public void reproduction() {
        Element[] newPopulation = new Element[population_size];

        // Elitism: Keep the best 10% of the population
        int eliteSize = population_size / 10;
        Arrays.sort(population, (a, b) -> Double.compare(b.fitness, a.fitness));
        System.arraycopy(population, 0, newPopulation, 0, eliteSize);

        // Check for stagnation
        if (population[0].fitness > bestFitness) {
            bestFitness = population[0].fitness;
            stagnationCounter = 0;
        } else {
            stagnationCounter++;
        }

        // If stagnation occurs for too long, introduce more diversity
        if (stagnationCounter > 5) {
            for (int i = eliteSize; i < population_size; i++) {
                newPopulation[i] = Element.getRandom(population[i].dna.length);
            }
            stagnationCounter = 0;
        } else {
            // Fill the rest of the population with mutated offspring
            for (int i = eliteSize; i < population_size; i++) {
                Element parent = tournamentSelection();
                Element offspring = new Element(parent);
                mutate(offspring, i);
                newPopulation[i] = offspring;
            }
        }
        population = newPopulation;
    }

    private void mutate(Element element, int index) {
        char[] dir = {'U', 'D', 'L', 'R'};

        // Adaptive mutation rate
        double adaptiveMutationRate = initial_mutation_rate * (1 + (double) index / population_size);

        for (int i = 0; i < element.dna.length; i++) {
            if (random.nextDouble() < adaptiveMutationRate) {
                // Intelligent mutation: higher chance to mutate to a different direction
                char newDir;
                do {
                    newDir = dir[random.nextInt(4)];
                } while (newDir == element.dna[i] && random.nextDouble() < 0.5);
                element.dna[i] = newDir;
            }
        }
    }

    private Element tournamentSelection() {
        int tournamentSize = 15;
        Element best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Element contestant = population[random.nextInt(population_size)];
            if (best == null || contestant.fitness > best.fitness) {
                best = contestant;
            }
        }
        return best;
    }

}

class Element {
    char[] dna;
    double fitness;

    public Element(Element e) {
        this.dna = new char[e.dna.length];
        System.arraycopy(e.dna, 0, this.dna, 0, dna.length);
        this.fitness = e.fitness;
    }

    public Element(char[] dna) {
        this.dna = dna;
    }

    public static Element getRandom(int dnaLength) {
        char[] dir = {'U', 'D', 'L', 'R'};
        char[] dna = new char[dnaLength];

        for (int i = 0; i < dnaLength; i++) {
            dna[i] = dir[(int) (Math.random() * 4)];
        }
        return new Element(dna);
    }
}

class Main {

    static char[][] board = {
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'},
            {'X', 'S', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', 'X', '.', '.', '.', '#', 'E', 'X'},
            {'X', '.', '.', '.', '.', '.', '.', '.', '#', 'E', 'X'},
            {'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X', 'X'}
    };

    static HashMap<Integer, int[]> goalPositions = getGoalPositions();
    static HashMap<Integer, int[]> initBoxPositions = getInitBoxPositions();
    static int[] sokoban = getSokobanPosition();

    public static void main(String[] args) {
        int population_size = 2000;
        double mutation_rate = 0.1;
        int dnaLength = 65;
        Population population = new Population(population_size, mutation_rate, dnaLength);

        int generations = 200;
        for (int i = 0; i < generations; i++) {
            population.evaluate_fitness();

            Element highest = population.getHighestElement();
            System.out.println(highest.fitness + " Generation: " + i);
            if (highest.fitness == Double.MAX_VALUE) {
                System.out.println(Arrays.toString(highest.dna));
                return;
            }

            population.reproduction();
        }

        // If no solution is found, use A* algorithm
        AStarSolver aStarSolver = new AStarSolver();
        String solution = aStarSolver.solve(Main.sokoban, Main.initBoxPositions);
        if (solution != null) {
            System.out.println("A* Solution found: " + solution);
        } else {
            System.out.println("No solution found.");
        }
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
            for (int j = j = 0; j < board[i].length; j++) {
                if (board[i][j] == '#')
                    boxPositions.put(cantorPair(i, j), new int[]{i, j});
            }
        }
        return boxPositions;
    }

    public static int cantorPair(int x, int y) {
        return (x + y) * (x + y + 1) / 2 + y;
    }
}


class AStarSolver {

    class Node implements Comparable<Node> {
        int[] sokobanPos;
        HashMap<Integer, int[]> boxPositions;
        int gCost; // Cost from start to this node
        int hCost; // Heuristic cost to the goal
        String path; // Path taken as a sequence of moves

        Node(int[] sokobanPos, HashMap<Integer, int[]> boxPositions, int gCost, String path) {
            this.sokobanPos = sokobanPos;
            this.boxPositions = new HashMap<>(boxPositions);
            this.gCost = gCost;
            this.hCost = heuristic(sokobanPos, boxPositions);
            this.path = path;
        }

        int getFCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.getFCost(), other.getFCost());
        }
    }

    public String solve(int[] sokobanStart, HashMap<Integer, int[]> initBoxPositions) {
        PriorityQueue<Node> openList = new PriorityQueue<>();
        HashSet<String> closedList = new HashSet<>();

        Node startNode = new Node(sokobanStart, initBoxPositions, 0, "");
        openList.add(startNode);

        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();

            if (isGoalState(currentNode.boxPositions)) {
                return currentNode.path; // Solution found
            }

            closedList.add(encodeState(currentNode.sokobanPos, currentNode.boxPositions));

            for (char move : new char[]{'U', 'D', 'L', 'R'}) {
                int[] newSokobanPos = moveSokoban(currentNode.sokobanPos, move);
                if (isValidMove(currentNode.sokobanPos, newSokobanPos, currentNode.boxPositions)) {
                    HashMap<Integer, int[]> newBoxPositions = new HashMap<>(currentNode.boxPositions);
                    updateBoxPositions(newSokobanPos, newBoxPositions, move);

                    Node neighborNode = new Node(newSokobanPos, newBoxPositions, currentNode.gCost + 1, currentNode.path + move);

                    if (!closedList.contains(encodeState(neighborNode.sokobanPos, neighborNode.boxPositions))) {
                        openList.add(neighborNode);
                    }
                }
            }
        }
        return null; // No solution found
    }

    private void updateBoxPositions(int[] newSokobanPos, HashMap<Integer, int[]> boxPositions, char move) {
        int pair = Main.cantorPair(newSokobanPos[0], newSokobanPos[1]);

        if (boxPositions.containsKey(pair)) {
            int[] newBoxPos = moveSokoban(newSokobanPos, move);
            int newPair = Main.cantorPair(newBoxPos[0], newBoxPos[1]);

            // Update the box position
            boxPositions.remove(pair);
            boxPositions.put(newPair, newBoxPos);
        }
    }


    private boolean isValidMove(int[] currentSokobanPos, int[] newSokobanPos, HashMap<Integer, int[]> boxPositions) {
        int x = newSokobanPos[0];
        int y = newSokobanPos[1];

        // Check if the new position is a wall
        if (Main.board[x][y] == 'X') return false;

        int pair = Main.cantorPair(x, y);

        // Check if the new position is occupied by a box
        if (boxPositions.containsKey(pair)) {
            int[] newBoxPos = moveSokoban(newSokobanPos, getMoveDirection(currentSokobanPos, newSokobanPos));
            int newPair = Main.cantorPair(newBoxPos[0], newBoxPos[1]);

            // Check if the new box position is a wall or another box
            if (Main.board[newBoxPos[0]][newBoxPos[1]] == 'X' || boxPositions.containsKey(newPair)) {
                return false;
            }
        }

        return true;
    }


    private char getMoveDirection(int[] oldPos, int[] newPos) {
        if (newPos[0] < oldPos[0]) return 'U';
        if (newPos[0] > oldPos[0]) return 'D';
        if (newPos[1] < oldPos[1]) return 'L';
        return 'R';
    }


    private int[] moveSokoban(int[] sokobanPos, char move) {
        int[] newPos = sokobanPos.clone();
        switch (move) {
            case 'U':
                newPos[0]--;
                break; // Move up
            case 'D':
                newPos[0]++;
                break; // Move down
            case 'L':
                newPos[1]--;
                break; // Move left
            case 'R':
                newPos[1]++;
                break; // Move right
        }
        return newPos;
    }


    private int heuristic(int[] sokobanPos, HashMap<Integer, int[]> boxPositions) {
        int totalDistance = 0;
        int minBoxDistance = Integer.MAX_VALUE;

        for (int[] box : boxPositions.values()) {
            int minGoalDistance = Integer.MAX_VALUE;

            for (int[] goal : Main.goalPositions.values()) {
                int distance = Math.abs(box[0] - goal[0]) + Math.abs(box[1] - goal[1]);
                minGoalDistance = Math.min(minGoalDistance, distance);
            }

            totalDistance += minGoalDistance;

            int boxDistance = Math.abs(sokobanPos[0] - box[0]) + Math.abs(sokobanPos[1] - box[1]);
            minBoxDistance = Math.min(minBoxDistance, boxDistance);
        }

        return totalDistance + (minBoxDistance == Integer.MAX_VALUE ? 0 : minBoxDistance);
    }

    private boolean isGoalState(HashMap<Integer, int[]> boxPositions) {
        for (int[] boxPos : boxPositions.values()) {
            if (!Main.goalPositions.containsKey(Main.cantorPair(boxPos[0], boxPos[1]))) {
                return false;
            }
        }
        return true;
    }

    private String encodeState(int[] sokobanPos, HashMap<Integer, int[]> boxPositions) {
        StringBuilder sb = new StringBuilder();
        sb.append(sokobanPos[0]).append(",").append(sokobanPos[1]).append(";");
        for (int[] pos : boxPositions.values()) {
            sb.append(pos[0]).append(",").append(pos[1]).append(";");
        }
        return sb.toString();
    }

}

