package com.newrelic.weld.test.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;

/**
 * CDI bean simulating a REST resource for the game.
 * Used for testing ProxyCall instrumentation.
 */
@ApplicationScoped
public class GameResource implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private Game game;

    /**
     * Get game status.
     * This method should be traced when in ProxyCall whitelist.
     */
    public String getStatus() {
        return game.getGameStatus();
    }

    /**
     * Make a guess.
     * This method should be traced when in ProxyCall whitelist.
     */
    public String makeGuess(int guess) {
        game.setGuess(guess);
        boolean won = game.check();

        if (won) {
            return "Correct! You won!";
        } else if (game.isGameLost()) {
            return "Sorry, you lost. The number was " + game.getNumber();
        } else {
            return "Wrong. Try again. " + game.getRemainingGuesses() + " guesses left.";
        }
    }

    /**
     * Reset the game.
     * This method should be traced when in ProxyCall whitelist.
     */
    public String resetGame() {
        game.reset();
        return "Game reset. " + game.getGameStatus();
    }

    /**
     * Get remaining guesses.
     */
    public int getRemainingGuesses() {
        return game.getRemainingGuesses();
    }
}
