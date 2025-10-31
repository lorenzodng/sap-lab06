package distributed_ttt.lobby_service.infrastructure;

import distributed_ttt.lobby_service.application.*;
import io.vertx.core.Vertx;

public class LobbyServiceMain {

	static final int LOBBY_SERVICE_PORT = 9001;
	static final String ACCOUNT_SERVICE_URI = "http://localhost:9000";
	static final String GAME_SERVICE_URI = "http://localhost:9002";

	public static void main(String[] args) {
		
		var lobby = new LobbyServiceImpl();
		
		AccountService accountService =  new AccountServiceProxy(ACCOUNT_SERVICE_URI);
		lobby.bindAccountService(accountService);

		GameService gameService =  new GameServiceProxy(GAME_SERVICE_URI);
		lobby.bindGameService(gameService);
		
		var vertx = Vertx.vertx();
		var server = new LobbyServiceController(lobby, LOBBY_SERVICE_PORT);
		vertx.deployVerticle(server);	
	}

}

