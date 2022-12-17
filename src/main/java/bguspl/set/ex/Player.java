package bguspl.set.ex;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.logging.Level;
import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    protected int[] tokens = new int[3];
    private Dealer dealer;
    private ArrayBlockingQueue<Integer> actionsQ = new ArrayBlockingQueue<Integer>(3);

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.score = 0;
        this.dealer = dealer;
        for (int i = 0; i < this.tokens.length; i++)
            this.tokens[i] = -1;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            int slot = 0;
            try {
                slot = this.actionsQ.take();
            } catch (InterruptedException ignored) {}
            boolean existToken = false;
            //checks if the slot contains a token, if so removes it
            for (int i = 0; i < this.tokens.length; i++) {
                if (slot == this.tokens[i]) {
                    this.tokens[i] = -1;
                    table.removeToken(this.id, slot);
                    existToken = true;
                    break;
                }
            }
            //if slot doesn't contain a token then place it
            if (!existToken){
                for (int i = 0; i < this.tokens.length; i++) {
                    if (this.tokens[i] == -1) {
                        this.tokens[i] = slot;
                        table.placeToken(this.id, slot);
                        if (i == 2){
                            synchronized (this) {
                                dealer.playersQ.add(this);
                                dealer.dealerThread.interrupt();
                                while (dealer.playersQ.contains(this)) {
                                   try {
                                        wait();
                                   } catch (InterruptedException ignored){}
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {

        // TODO implement
       if(!actionsQ.offer(slot))
           env.logger.info("Only 3 action in a time");

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        //this.score +=1;
        try {
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {}

        long timePoint = env.config.pointFreezeMillis;
        timePoint += System.currentTimeMillis();
        while (timePoint > System.currentTimeMillis()){
            env.ui.setFreeze(this.id, timePoint - System.currentTimeMillis());
            try {
                Thread.sleep(950);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        env.ui.setFreeze(this.id,0);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement

        long timePenalty = env.config.penaltyFreezeMillis;
        timePenalty += System.currentTimeMillis();
        while (timePenalty > System.currentTimeMillis()){
            env.ui.setFreeze(this.id, timePenalty - System.currentTimeMillis());
            try {
                Thread.sleep(950);
            } catch (InterruptedException ignored){}
        }
        env.ui.setFreeze(this.id,0);
    }

    public int getScore() {
        return score;
    }
}
