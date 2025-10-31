package distributed_ttt.lobby_service.application;

import common.exagonal.OutBoundPort;
import distributed_ttt.lobby_service.domain.*;

@OutBoundPort
public interface GameService  {

	void createNewGame(String gameId) throws GameAlreadyPresentException, CreateGameFailedException, ServiceNotAvailableException;
	
	String joinGame(UserId userId, String gameId, TTTSymbol symbol) throws InvalidJoinGameException, JoinGameFailedException, ServiceNotAvailableException;
	    
}
