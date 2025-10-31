package monolith_ttt_game_server.application;

import java.util.logging.Logger;

import monolith_ttt_game_server.domain.Game;
import monolith_ttt_game_server.domain.GameEnded;
import monolith_ttt_game_server.domain.GameEvent;
import monolith_ttt_game_server.domain.GameObserver;
import monolith_ttt_game_server.domain.GameStarted;
import monolith_ttt_game_server.domain.InvalidMoveException;
import monolith_ttt_game_server.domain.NewMove;
import monolith_ttt_game_server.domain.TTTSymbol;
import monolith_ttt_game_server.domain.UserId;

/**
 * 
 * Representing a player session.
 * 
 * - Created when a logged user joins a game.
 * - It includes the operations that a player can do.
 * - It acts as observer of events generated in the game. 
 * 
 */
public class PlayerSession implements GameObserver {

	static Logger logger = Logger.getLogger("[Player Session]");
	private UserId userId;
	private Game game;
	private TTTSymbol symbol;
	private String playerSessionId;
	private PlayerSessionEventObserver playerSessionEventNotifier;
	
	public PlayerSession(String playerSessionId, UserId userId, Game game, TTTSymbol symbol) {
		this.userId = userId;
		this.game = game;
		this.symbol = symbol;
		this.playerSessionId = playerSessionId;
	}
		
	public void makeMove(int x, int y) throws InvalidMoveException {
		game.makeAmove(userId, x, y);
	}
	
	public TTTSymbol getSymbol() {
		return symbol;
	}
	
	public String getId() {
		return playerSessionId;
	}

	public void notifyGameEvent(GameEvent ev) {
		if (ev instanceof GameStarted) {
			playerSessionEventNotifier.gameStarted(playerSessionId);
		} else if (ev instanceof GameEnded) {
			var e = (GameEnded) ev;
			playerSessionEventNotifier.gameEnded(playerSessionId, e.winner());			
		} else if (ev instanceof NewMove) {
			var e = (NewMove) ev;
			log("new move: " + e.symbol() + " in (" + e.x() + ", " + e.y() + ")");
			playerSessionEventNotifier.newMove(playerSessionId, e.symbol(), e.x(), e.y());

		}
	}
		
	public void bindPlayerSessionEventNotifier(PlayerSessionEventObserver playerSessionEventNotifier) {
		this.playerSessionEventNotifier = playerSessionEventNotifier;
	}
	
	public PlayerSessionEventObserver getPlayerSessionEventNotifier() {
		return playerSessionEventNotifier;
	}
	
	private void log(String msg) {
		System.out.println("[ player " + userId.id() + " in game " + game.getId() + " ] " + msg);
	}
}
