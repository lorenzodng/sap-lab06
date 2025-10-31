package monolith_ttt_game_server.domain;

/**
 * 
 * Domain event: new move
 * 
 */

public record NewMove (String gameId, String symbol, int x, int y) implements GameEvent {}
