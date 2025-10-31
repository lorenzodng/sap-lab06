package monolith_ttt_game_server.domain;

/**
 * 
 * Domain event: game started
 * 
 */
public record GameStarted (String gameId) implements GameEvent {}
