package distributed_ttt.game_service.domain;

import java.util.Optional;

//record per la partita terminata
public record GameEnded (String gameId, Optional<String> winner) implements GameEvent {}
