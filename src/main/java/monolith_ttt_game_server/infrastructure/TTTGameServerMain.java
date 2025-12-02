package monolith_ttt_game_server.infrastructure;

import io.vertx.core.Vertx;
import monolith_ttt_game_server.application.GameServiceImpl;

//avvia il backend
public class TTTGameServerMain {

	static final int BACKEND_PORT = 8080; //porta sul quale il server ascolta le richiesta http

	public static void main(String[] args) {

		var service = new GameServiceImpl(); //crea un'istanza del servizio principale
		service.bindAccountRepository(new InMemoryAccountRepository()); //crea un repository degli account utente e lo collega al servizio principale
		service.bindGameRepository(new InMemoryGameRepository()); //crea un repository delle partite e lo collega al servizio principale
		var vertx = Vertx.vertx(); //crea un'istanza vertx per gestire le richieste http
		var server = new GameServerController(service, BACKEND_PORT); //crea un'istanza del controller
		vertx.deployVerticle(server); //avvia il server sulla porta specificata (esegue il metodo "start" del controller)
	}
}

