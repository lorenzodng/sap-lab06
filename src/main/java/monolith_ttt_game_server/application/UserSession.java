package monolith_ttt_game_server.application;

import monolith_ttt_game_server.domain.InvalidJoinException;
import monolith_ttt_game_server.domain.TTTSymbol;
import monolith_ttt_game_server.domain.UserId;

//sessione dell'utente (ancora non giocatore)
public class UserSession {

	private String sessionId; //id della sessione
	private UserId userId; //id dell'utente
	private GameService gameService; //servizio principale dell'app
	
	public UserSession(String sessionId, UserId userId, GameServiceImpl gameService) {
		this.userId = userId;
		this.gameService = gameService;
		this.sessionId = sessionId;
	}

	//crea una nuova partita
	public void createNewGame(String gameId) throws GameAlreadyPresentException {
		gameService.createNewGame(gameId);		
	}

	//fa entrare un giocaotre in una partita
	public PlayerSession joinGame(String gameId, TTTSymbol symbol, PlayerSessionEventObserver notifier) throws InvalidJoinException {
		return gameService.joinGame(userId, gameId, symbol, notifier);
	}

	//recupera l'id della sessione
	public String getSessionId() {
		return sessionId;
	}

	//recupera l'id dell'utente
	public UserId getUserId() {
		return userId;
	}

}
