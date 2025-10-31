package monolith_ttt_game_server.domain;

import java.util.Optional;

/**
 * 
 * Domain event: game ended
 * 
 */
public record GameEnded (String gameId, Optional<String> winner) implements GameEvent {}
