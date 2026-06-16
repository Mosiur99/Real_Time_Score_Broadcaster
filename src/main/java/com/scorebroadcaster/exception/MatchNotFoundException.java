package com.scorebroadcaster.exception;

/**
 * Thrown when a requested match does not exist.
 */
public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(Long matchId) {
        super("Match not found with id: " + matchId);
    }

    public MatchNotFoundException(String message) {
        super(message);
    }
}
