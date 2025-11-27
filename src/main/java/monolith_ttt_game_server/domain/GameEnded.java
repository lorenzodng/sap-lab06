package monolith_ttt_game_server.domain;

import java.util.Optional;

//record per la partita terminata
public record GameEnded (String gameId, Optional<String> winner) implements GameEvent {}
