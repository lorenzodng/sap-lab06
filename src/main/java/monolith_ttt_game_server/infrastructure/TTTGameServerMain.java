package monolith_ttt_game_server.infrastructure;

import io.vertx.core.Vertx;
import monolith_ttt_game_server.application.GameServiceImpl;

/**
 * 
 * TTT Game Server example
 * 
 * - Modular monolith architecture, based on hexagonal architecture
 * - DDD domain model pattern 
 * - REST API
 * 
 */
public class TTTGameServerMain {

	static final int BACKEND_PORT = 8080;

	public static void main(String[] args) {
		
		var service = new GameServiceImpl();		
		service.bindAccountRepository(new InMemoryAccountRepository());
		service.bindGameRepository(new InMemoryGameRepository());
		
		var vertx = Vertx.vertx();
		var server = new GameServerController(service, BACKEND_PORT);
		vertx.deployVerticle(server);	
	}

}

