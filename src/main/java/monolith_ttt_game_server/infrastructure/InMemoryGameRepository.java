package monolith_ttt_game_server.infrastructure;

import java.util.HashMap;

import common.ddd.Aggregate;
import common.ddd.Repository;
import common.exagonal.Adapter;
import common.exagonal.OutBoundPort;
import monolith_ttt_game_server.application.GameRepository;
import monolith_ttt_game_server.domain.Game;

/**
 * 
 * Games Repository
 * 
 */
@Adapter
public class InMemoryGameRepository implements GameRepository {

	private HashMap<String, Game> games;

	public InMemoryGameRepository() {
		games = new HashMap<>();
	}
	
	public void addGame(Game game) {
		games.put(game.getId(), game);
		
	}
	
	public boolean isPresent(String gameId) {
		return games.containsKey(gameId);
	}
	
	public Game getGame(String gameId) {
		return games.get(gameId);
	}


}
