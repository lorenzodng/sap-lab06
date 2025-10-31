package distributed_ttt.game_service.infrastructure;

import java.util.HashMap;

import common.exagonal.Adapter;
import distributed_ttt.game_service.application.GameRepository;
import distributed_ttt.game_service.domain.Game;

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
