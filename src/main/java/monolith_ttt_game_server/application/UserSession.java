package monolith_ttt_game_server.application;

import monolith_ttt_game_server.domain.InvalidJoinException;
import monolith_ttt_game_server.domain.TTTSymbol;
import monolith_ttt_game_server.domain.UserId;

/**
 * 
 * Representing a user session.
 * 
 * - Created when a user logs in
 * - It includes the operations that a user can do -  create a game, join a game
 * 
 */
public class UserSession {

	private String sessionId;
	private UserId userId;
	private GameService gameService;
	
	public UserSession(String sessionId, UserId userId, GameServiceImpl gameService) {
		this.userId = userId;
		this.gameService = gameService;
		this.sessionId = sessionId;
	}
		
	public void createNewGame(String gameId) throws GameAlreadyPresentException {
		gameService.createNewGame(gameId);		
	}

	public PlayerSession joinGame(String gameId, TTTSymbol symbol, PlayerSessionEventObserver notifier) throws InvalidJoinException {
		return gameService.joinGame(userId, gameId, symbol, notifier);
	}
	
	public String getSessionId() {
		return sessionId;
	}
	
	public UserId getUserId() {
		return userId;
	}

}
