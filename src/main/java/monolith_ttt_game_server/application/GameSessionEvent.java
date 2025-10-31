package monolith_ttt_game_server.application;

import monolith_ttt_game_server.domain.GameEvent;

public record GameSessionEvent(String sessionId, GameEvent ev) {

}
