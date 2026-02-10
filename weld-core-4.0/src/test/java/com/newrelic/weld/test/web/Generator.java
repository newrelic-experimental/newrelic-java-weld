package com.newrelic.weld.test.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.io.Serializable;
import java.util.Random;

/**
 * CDI bean that generates a random number for the game.
 */
@ApplicationScoped
public class Generator implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Random random = new Random();

    private final int maxNumber = 100;

    /**
     * Produces a random number for injection.
     */
    int next() {
        return random.nextInt(maxNumber);
    }

    /**
     * Produces the maximum number for injection.
     */
    @Produces
    @MaxNumber
    public int getMaxNumber() {
        return maxNumber;
    }
}
