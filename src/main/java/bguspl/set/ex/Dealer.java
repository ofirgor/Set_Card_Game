package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    protected Thread dealerThread;
    protected final LinkedBlockingQueue<Player> playersQ = new LinkedBlockingQueue<>();

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        for (Player player:players) {
            Thread playerThread = new Thread(player,""+ player.id);
            playerThread.start();
        }
        dealerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            reshuffleTime= System.currentTimeMillis() + 60000;
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }


    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            table.hints();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
        if(!playersQ.isEmpty()) {
            Player player = playersQ.remove();
            int[] currPlayerTokens = new int[3];
            for (int i = 0; i < currPlayerTokens.length; i++) {
                currPlayerTokens[i] = player.tokens[i];
            }
            synchronized (player) {
                int[] cards = new int[3];
                for (int i = 0; i < currPlayerTokens.length; i++)
                    cards[i] = table.slotToCard[currPlayerTokens[i]];
                List<Player> playersToRemoveFromQ = new ArrayList<>();
                if (env.util.testSet(cards)) {
                    player.point();
                    //removes all tokens from the cards
                    for (Player p:players) {
                        for (int i = 0; i < p.tokens.length; i++) {
                            for (int j = 0; j < currPlayerTokens.length; j++) {
                                if (p.tokens[i] == currPlayerTokens[j]) {
                                    playersToRemoveFromQ.add(p);
                                    table.removeToken(p.id,p.tokens[i]);
                                    p.tokens[i] = -1;
                                }

                            }
                        }
                    }
                    //remove the cards from the table
                    for (int i = 0; i < cards.length; i++) {
                        table.cardToSlot[cards[i]] = null;
                    }
                    for (int i = 0; i < currPlayerTokens.length; i++) {
                        table.slotToCard[currPlayerTokens[i]] = null;
                    }
                    reshuffleTime= System.currentTimeMillis() + 60000;
                    updateTimerDisplay(false);
                } else
                    player.penalty();
                //remove players from the waiting queue
                for (Player p: playersQ)
                    if (playersToRemoveFromQ.contains(p)) {
                        playersQ.remove(p);
                        playersToRemoveFromQ.remove(p);
                    }
                player.notifyAll();
            }
        }

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        Collections.shuffle(deck);
        int card = 0;
        for (int i = 0; i < 12; i++) {
           if (table.slotToCard[i] == null) {
               card = deck.remove(0);
               table.placeCard(card, i);
           }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored){}
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(),false);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
       for (Player p:players) {
            for (int i = 0; i < p.tokens.length; i++) {
                if(p.tokens[i] != -1) {
                    table.removeToken(p.id, p.tokens[i]);
                    p.tokens[i] = -1;
                }
            }
      }
        for (int i = 0; i < 12; i++) {
            if(table.slotToCard[i] != null) {
                deck.add(0, table.slotToCard[i]);
                table.removeCard(i);
            }
        }
        Collections.shuffle(deck);
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }


}
