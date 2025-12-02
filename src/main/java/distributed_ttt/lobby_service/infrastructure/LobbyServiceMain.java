package distributed_ttt.lobby_service.infrastructure;

import distributed_ttt.lobby_service.application.*;
import io.vertx.core.Vertx;

public class LobbyServiceMain {

	static final int LOBBY_SERVICE_PORT = 9001; //porta sul quale il server ascolta le richiesta http
	static final String ACCOUNT_SERVICE_URI = "http://localhost:9000"; //link del servizio degli account
	static final String GAME_SERVICE_URI = "http://localhost:9002"; //link del servizio di gioco

	public static void main(String[] args) {
		
		var lobby = new LobbyServiceImpl(); //crea un'istanza del servizio di lobby
		AccountService accountService =  new AccountServiceProxy(ACCOUNT_SERVICE_URI); //crea un'istanza del proxy degli account
		lobby.bindAccountService(accountService); //associa l'istanza al servizio di lobby
		GameService gameService = new GameServiceProxy(GAME_SERVICE_URI); //crea un'istanza del proxy di gioco
		lobby.bindGameService(gameService); //associa l'istanza al servizio di lobby
        var vertx = Vertx.vertx(); //crea un'istanza vertx per gestire le richieste http
        var server = new LobbyServiceController(lobby, LOBBY_SERVICE_PORT); //crea un'istanza del controller
		vertx.deployVerticle(server); //avvia il server sulla porta specificata (esegue il metodo "start" del controller)
	}
}

