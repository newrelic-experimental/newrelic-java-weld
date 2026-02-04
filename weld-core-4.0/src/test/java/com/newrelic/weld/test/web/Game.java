package com.newrelic.weld.test.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.Serializable;

/**
 * CDI bean for a number guessing game.
 * Used for testing BeanInstance instrumentation.
 */
@ApplicationScoped
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    private int number;
    private int guess;
    private int smallest;
    private int biggest;
    private int remainingGuesses;
    private boolean gameWon;
    private boolean gameLost;

    @Inject
    private Generator generator;

    @Inject
    @MaxNumber
    private int maxNumber;

    public Game() {
    }

    public int getNumber() {
        return number;
    }

    public int getGuess() {
        return guess;
    }

    public void setGuess(int guess) {
        this.guess = guess;
    }

    public int getSmallest() {
        return smallest;
    }

    public int getBiggest() {
        return biggest;
    }

    public int getRemainingGuesses() {
        return remainingGuesses;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public boolean isGameLost() {
        return gameLost;
    }

    /**
     * Check if the guess matches the number.
     * This method should be traced when in BeanInstance whitelist.
     * Now also intercepted by @Logged to test AroundInvokeInvocationContext.
     */
    @Logged
    public boolean check() {
        if (guess == number) {
            gameWon = true;
        } else if (guess < number) {
            smallest = guess;
        } else if (guess > number) {
            biggest = guess;
        }
        remainingGuesses--;

        if (remainingGuesses == 0 && !gameWon) {
            gameLost = true;
        }

        return gameWon;
    }

    /**
     * Reset the game.
     * This method should be traced when in BeanInstance whitelist.
     * Now also intercepted by @Logged to test AroundInvokeInvocationContext.
     */
    @Logged
    public void reset() {
        this.smallest = 0;
        this.biggest = maxNumber;
        this.remainingGuesses = 10;
        this.guess = 0;
        this.number = generator.next();
        this.gameWon = false;
        this.gameLost = false;
    }

    /**
     * Get game status message.
     * This method should be traced when matching regex pattern (get.*).
     */
    public String getGameStatus() {
        if (gameWon) {
            return "You won! The number was " + number;
        } else if (gameLost) {
            return "You lost! The number was " + number;
        } else {
            return "Guess a number between " + smallest + " and " + biggest +
                   ". You have " + remainingGuesses + " guesses left.";
        }
    }

    @PostConstruct
    public void init() {
        reset();
    }
}
