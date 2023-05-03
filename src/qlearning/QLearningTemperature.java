package qlearning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Q-Learning algorithm for learning temperature in a number of intervals in day.
 */
public class QLearningTemperature {
    /**
     * Number of days
     */
    private static final int NUM_DAYS = 1;
    /**
     * Number of intervals in a day
     */
    private static final int NUM_INTERVALS = 24 * 4;
    /**
     * Number of possible states
     */
    private static final int NUM_STATES = NUM_DAYS * NUM_INTERVALS;
    /**
     * Number of possible temperatures/actions.
     */
    private static final int NUM_TEMPERATURES = 21; //15-25 degrees
    /**
     * Learning rate
     */
    private static final float LEARNING_RATE = 0.1f;
    /**
     * Discount factor
     */
    private static final float DISCOUNT_FACTOR = 0.0f;
    /**
     * List of possible temperatures for indexing
     */
    public static final List<Float> temperatures = new ArrayList<>();

    /**
     * Q-Table
     */
    private float[][] qTable;
    /**
     * Random generator for epsilon-greedy
     */
    private Random random;

    /**
     * Constructor filling Q-Table, initializing random generator
     * and filling list of possible temperatures.
     */
    public QLearningTemperature() {
        qTable = new float[NUM_STATES][NUM_TEMPERATURES];
        random = new Random();
        float t = 15.f;
        for (int i = 0; i < NUM_TEMPERATURES; i++) {
            temperatures.add(t);
            t += 0.5f;
        }
    }

    /**
     * This method chooses the best possible action
     * for given interval based on Q-Values.
     * With a chance of 1% chooses random action.
     *
     * @param interval integer representing interval for which
     *                 it chooses the action
     * @return chosen action
     */
    private int chooseAction(int interval) {
        // Choose action using epsilon-greedy policy
        float epsilon = 0.01f;
        if (random.nextFloat() < epsilon) {
            return random.nextInt(NUM_TEMPERATURES);
        } else {
            float[] actionValues = qTable[interval];
            int maxIndex = 0;
            for (int i = 1; i < NUM_TEMPERATURES; i++) {
                if (actionValues[i] > actionValues[maxIndex]) {
                    maxIndex = i;
                }
            }
            return maxIndex;
        }
    }

    /**
     * Method updating Q-Table according to Bellman's equation.
     *
     * @param interval represents the state
     * @param action represents the action
     * @param reward represents the reward
     */
    public void updateQTable(int interval, int action, float reward) {
        int nextInterval = (interval + 1) % NUM_STATES;
        float currentQValue = qTable[interval][action];
        float maxNextQValue = qTable[nextInterval][0];
        for (int i = 1; i < NUM_TEMPERATURES; i++) {
            if (qTable[nextInterval][i] > maxNextQValue) {
                maxNextQValue = qTable[nextInterval][i];
            }
        }
        float newQValue = currentQValue + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQValue - currentQValue);
        qTable[interval][action] = newQValue;
    }

    /**
     * Method returns temperature from action format based on given interval.
     *
     * @param interval state for which the temperature is chosen
     * @return chosen temperature
     */
    public Float chooseTemperature(int interval) {
        int action = chooseAction(interval);
        return temperatures.get(action);
    }

    /**
     * Returns action index based on float temperature.
     *
     * @param temp temperature
     * @return action index
     */
    public int getActionIndex(float temp) {
        return temperatures.indexOf(temp);
    }
}