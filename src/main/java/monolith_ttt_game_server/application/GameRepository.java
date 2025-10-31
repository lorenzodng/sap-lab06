package monolith_ttt_game_server.application;

import java.util.HashMap;

import common.ddd.Aggregate;
import common.ddd.Repository;
import common.exagonal.OutBoundPort;
import monolith_ttt_game_server.domain.Game;

/**
 * 
 * Games Repository
 * 
 */
@OutBoundPort
public interface GameRepository extends Repository {

	void addGame(Game game);
	
	boolean isPresent(String gameId);
	
	Game getGame(String gameId);

}
