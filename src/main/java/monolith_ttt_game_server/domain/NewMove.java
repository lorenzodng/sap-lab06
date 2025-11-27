package monolith_ttt_game_server.domain;

//record per la mossa
public record NewMove (String gameId, String symbol, int x, int y) implements GameEvent {}
