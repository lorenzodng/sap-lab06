package monolith_ttt_game_server.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;
import monolith_ttt_game_server.application.AccountAlreadyPresentException;
import monolith_ttt_game_server.application.AccountNotFoundException;
import monolith_ttt_game_server.application.GameAlreadyPresentException;
import monolith_ttt_game_server.application.GameNotFoundException;
import monolith_ttt_game_server.application.GameService;
import monolith_ttt_game_server.application.LoginFailedException;
import monolith_ttt_game_server.domain.InvalidJoinException;
import monolith_ttt_game_server.domain.InvalidMoveException;
import monolith_ttt_game_server.domain.TTTSymbol;

/*
controller di backend (intermediario client <-> servizio di gioco):
client -> controller che utilizza un server -> servizio di gioco
 */
public class GameServerController extends VerticleBase  {

	private int port; //porta su cui il server ascolta le richieste http
	static Logger logger = Logger.getLogger("[GameServerController]");
	static final String API_VERSION = "v1"; //versione dell'API utilizzata per definire le rotte
	static final String ACCOUNTS_RESOURCE_PATH = 		"/api/" + API_VERSION + "/accounts"; //rotta per creare un nuovo account
	static final String ACCOUNT_RESOURCE_PATH = 		"/api/" + API_VERSION + "/accounts/:accountId"; //rotta per recuperare le informazioni di un account
	static final String LOGIN_RESOURCE_PATH = 			"/api/" + API_VERSION + "/accounts/:accountId/login"; //rotta per effettuare il login di un account
	static final String GAME_RESOURCE_PATH = 			"/api/" + API_VERSION + "/games/:gameId"; //rotta per recuperare le informazioni di una partita
	static final String USER_SESSIONS_RESOURCE_PATH = 	"/api/" + API_VERSION + "/user-sessions"; //rotta (di base) per le sessioni utente
	static final String CREATE_GAME_RESOURCE_PATH = USER_SESSIONS_RESOURCE_PATH + "/:sessionId/create-game"; //rotta per creare una nuova partita
	static final String JOIN_GAME_RESOURCE_PATH = USER_SESSIONS_RESOURCE_PATH + "/:sessionId/join-game"; //rotta per far entrare un utente in una partita
	static final String PLAYER_MOVE_RESOURCE_PATH = GAME_RESOURCE_PATH + "/:playerSessionId/move"; //rotta per eseguire una mossa
	static final String WS_EVENT_CHANNEL_PATH = "/api/" + API_VERSION + "/events"; //rotta (websocket) per ricevere gli eventi di una partita
	private GameService gameService; //servizio principale

	public GameServerController(GameService service, int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
		this.gameService = service;
	}

    //avvia il server (eseguito automaticamente alla chiamata "vertx.deployVerticle(server)")
	public Future<?> start() {
		logger.log(Level.INFO, "TTT Game Service initializing...");
		HttpServer server = vertx.createHttpServer(); //crea un sever http
				
		Router router = Router.router(vertx); //router per l'instradamento delle richieste http
		router.route(HttpMethod.POST, ACCOUNTS_RESOURCE_PATH).handler(this::createNewAccount); //associa alla rotta per creare un nuovo account il relativo metodo
		router.route(HttpMethod.GET, ACCOUNT_RESOURCE_PATH).handler(this::getAccountInfo);  //associa alla rotta per recuperare le informazioni di un account il relativo metodo
		router.route(HttpMethod.POST, LOGIN_RESOURCE_PATH).handler(this::login); //associa alla rotta per effettuare il login di un account il relativo metodo
        router.route(HttpMethod.GET, GAME_RESOURCE_PATH).handler(this::getGameInfo); //associa alla rotta per recuperare le informazioni di una partita il relativo metodo
        router.route(HttpMethod.POST, CREATE_GAME_RESOURCE_PATH).handler(this::createNewGame); //associa alla rotta per creare una nuova partita il relativo metodo
		router.route(HttpMethod.POST, JOIN_GAME_RESOURCE_PATH).handler(this::joinGame); //associa alla rotta per far entrare un utente in una partita il relativo metodo
		router.route(HttpMethod.POST, PLAYER_MOVE_RESOURCE_PATH).handler(this::makeAMove); //associa alla rotta per eseguire una mossa il relativo metodo
		handleEventSubscription(server); //registra un websocket handler al server per ascoltare le richieste del client

		router.route("/public/*").handler(StaticHandler.create()); //gestisce le richieste del client che iniziano con "public", relative all'aspetto della pagina web

		var fut = server.requestHandler(router).listen(port); //avvia il server sulla porta specificata
		fut.onSuccess(res -> { //in caso di avvio con successo
			logger.log(Level.INFO, "TTT Game Service ready - port: " + port); //stampa un messaggio di log
		});

		return fut; //restituisce la future
	}

	//crea un account utente
	protected void createNewAccount(RoutingContext context) { //context è l'oggetto che rappresenta la richiesta http
		logger.log(Level.INFO, "create a new account");
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = userInfo.getString("userName"); //estrae il valore del campo "userName"
			var password = userInfo.getString("password"); //estrae il valore del campo "password"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				gameService.registerUser(userName, password); //crea un nuovo account utente
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
                var loginPath = LOGIN_RESOURCE_PATH.replace(":accountId", userName); //costruisce il link per il login
                var accountPath = ACCOUNT_RESOURCE_PATH.replace(":accountId", userName); //costruisce il link per le informazioni dell'account
				reply.put("loginLink", loginPath); //popola l'oggetto con il link per il login
				reply.put("accountLink", accountPath); //popola l'oggetto con il link alle informazioni dell'account
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (AccountAlreadyPresentException ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}
		});
	}

	//recupera le informazioni di un account
    protected void getAccountInfo(RoutingContext context) {
        logger.log(Level.INFO, "get account info");
        var userName = context.pathParam("accountId"); //estrae (dall'url) il valore del campo "userName"
        var reply = new JsonObject(); //crea un oggetto json di risposta al client
        try {
            var acc = gameService.getAccountInfo(userName); //recupera l'account associato a "userName"
            reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
            var accJson = new JsonObject(); //crea un oggetto json account per memorizzare le informazioni dell'account
            accJson.put("userName", acc.getUserName()); //popola l'oggetto account con il relativo username
            accJson.put("password", acc.getPassword()); //popola l'oggetto account con la relativa password
            accJson.put("whenCreated", acc.getWhenCreated()); //popola l'oggetto account con la relativa data di creazione
            reply.put("accountInfo", accJson); //popola l'oggetto di risposta con l'oggetto account
            sendReply(context.response(), reply); //invia la risposta al client
        } catch (AccountNotFoundException ex) {
            reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
            reply.put("error", "account-not-present"); //popola l'oggetto con la specifica dell'errore
            sendReply(context.response(), reply); //invia la risposta al client
        } catch (Exception ex1) {
            sendError(context.response()); //invia un errore al client
        }
    }
	
	//logga un utente
	protected void login(RoutingContext context) {
		logger.log(Level.INFO, "Login request");
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = context.pathParam("accountId"); //estrae (dall'url) il valore del campo "userName"
			var password = userInfo.getString("password"); //estrae il valore del campo "password"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.login(userName, password); //esegue il login dell'utente
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				var createPath = CREATE_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId()); //costruisce il link per la creazione della partita
				var joinPath = JOIN_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId()); //costruisce il link per entrare nella partita
				reply.put("createGameLink", createPath); //popola l'oggetto con il link per la creazione della partita
				reply.put("joinGameLink", joinPath); //popola l'oggetto con il link per il login
				reply.put("sessionId", session.getSessionId()); //popola l'oggetto con l'id della sessione
				reply.put("sessionLink", USER_SESSIONS_RESOURCE_PATH + "/" + session.getSessionId());//popola l'oggetto con il link della sessione
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (LoginFailedException ex) {
				reply.put("result", "login-failed"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}
		});
	}
	
    //crea una nuova partita
	protected void createNewGame(RoutingContext context) {
		logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject userInfo = buf.toJsonObject(); //converte il body in un oggetto json
			var sessionId = context.pathParam("sessionId"); //estrae (dall'url) il valore del campo "sessionId"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.getUserSession(sessionId); //recupera la sessione dell'utente
				var gameId = userInfo.getString("gameId"); //estrae il valore del campo "gameId"
				session.createNewGame(gameId); //crea una partita
                var gamePath = GAME_RESOURCE_PATH.replace(":gameId", gameId); //costruisce il link per recuperare le informazioni della partita creata
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
                reply.put("gameLink", gamePath); //popola l'oggetto con il link alla partita creata
				var joinPath = JOIN_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId()); //costruisce il link per entrare nella partita
				reply.put("joinGameLink", joinPath); //popola l'oggetto con il link per entrare nella partita
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (GameAlreadyPresentException ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", "game-already-present"); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}			
		});		
	}

	//recupera le informazioni di una partita
    protected void getGameInfo(RoutingContext context) {
        logger.log(Level.INFO, "get game info");
        var gameId = context.pathParam("gameId"); //estrae (dall'url) il valore del campo "gameId"
        var reply = new JsonObject(); //crea un oggetto json di risposta al client
        try {
            var game = gameService.getGameInfo(gameId); //recupera la partita con id "gameId"
            reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
            var gameJson = new JsonObject(); //crea un oggetto json partita per memorizzare le informazioni della partita
            gameJson.put("gameId", game.getId()); //popola l'oggetto partita con il relativo id
            gameJson.put("gameState", game.getGameState()); //popola l'oggetto partita con il relativo stato
            if (game.isStarted() || game.isFinished()) { //se la partita è iniziata o terminata
                var bs = game.getBoardState(); //recupera lo stato della griglia
                JsonArray array = new JsonArray(); //crea un arrray per lo stato della griglia
                for (var el : bs) { //per ogni elemento della griglia
                    array.add(el); //lo aggiunge all'array
                }
                gameJson.put("boardState", array); //popola l'oggetto partita con l'array
            }
            if (game.isStarted()) { //se la partita è iniziata
                gameJson.put("turn", game.getCurrentTurn()); //popola l'oggetto partita con il turno corrente
            }
            reply.put("gameInfo", gameJson); //popola l'oggetto di risposta con l'oggetto partita
            sendReply(context.response(), reply); //invia la risposta al client
        } catch (GameNotFoundException ex) {
            reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
            reply.put("error", "game-not-present"); //popola l'oggetto con la specifica dell'errore
            sendReply(context.response(), reply); //invia la risposta al client
        } catch (Exception ex1) {
            sendError(context.response()); //invia un errore al client
        }
    }

    //consente a un utente di unirsi a una partita
	protected void joinGame(RoutingContext context) {
		logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			JsonObject joinInfo = buf.toJsonObject(); //converte il body in un oggetto json
			logger.log(Level.INFO, "Join info: " + joinInfo);
			String sessionId = context.pathParam("sessionId"); //estrae (dall'url) il valore del campo "sessionId"
			String gameId = joinInfo.getString("gameId"); //estrae il valore del campo "gameId"
			String symbol = joinInfo.getString("symbol"); //estrae il valore del campo "symbol"
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				var session = gameService.getUserSession(sessionId); //recupera la sessione dell'utente
				var playerSession = session.joinGame(gameId, symbol.equals("X") ? TTTSymbol.X : TTTSymbol.O, new VertxPlayerSessionEventObserver(vertx.eventBus())); //esegue il join dell'utente nella partita
				var movePath = PLAYER_MOVE_RESOURCE_PATH.replace(":gameId", gameId).replace(":playerSessionId",playerSession.getId()); //costruisce il link per eseguire una mossa nella partita
                var gamePath = GAME_RESOURCE_PATH.replace(":gameId", gameId); //costruisce il link per recuperare le informazioni della partita creata
                reply.put("playerSessionId", playerSession.getId()); //popola l'oggetto con l'id della sessione giocatore
				reply.put("moveLink", movePath); //popola l'oggetto con il link per eseguire la mossa
                reply.put("gameLink", gamePath); //popola l'oggetto con il link alla partita
                reply.put("playerSessionLink", gamePath + "/player-" + symbol); //popola l'oggetto con il link alla partita dal punto di vista del giocatore
                reply.put("eventChannelLink", WS_EVENT_CHANNEL_PATH); //popola l'oggetto con il link che il client deve usare per ricevere gli eventi di una partita
				reply.put("result", "ok"); //popola l'oggetto con un'informazione di successo
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (InvalidJoinException  ex) {
				reply.put("result", "error"); //popola l'oggetto con un'informazione di errore
				reply.put("error", ex.getMessage()); //popola l'oggetto con la specifica dell'errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				sendError(context.response()); //invia un errore al client
			}			
		});
	}

    //esegue una mossa
	protected void makeAMove(RoutingContext context) {
		logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
		context.request().handler(buf -> { //prende il body del messaggio http inviato dal client
			var reply = new JsonObject(); //crea un oggetto json di risposta al client
			try {
				JsonObject moveInfo = buf.toJsonObject(); //converte il body in un oggetto json
				logger.log(Level.INFO, "move info: " + moveInfo);
                String playerSessionId = moveInfo.getString("playerSessionId"); //estrae il valore del campo "playerSessionId"
				int x = Integer.parseInt(moveInfo.getString("x")); //estrae il valore del campo "x" e lo converte in un intero
				int y = Integer.parseInt(moveInfo.getString("y")); //estrae il valore del campo "y" e lo converte in un intero
				var ps = gameService.getPlayerSession(playerSessionId); //recupera la sessione del giocatore
                ps.makeMove(x, y); //fa eseguire al giocatore una mossa
				reply.put("result", "accepted"); //popola l'oggetto con un'informazione di successo
				var gameId = moveInfo.getString("gameId"); //estrae il valore del campo "gameId"
                var movePath = PLAYER_MOVE_RESOURCE_PATH.replace(":gameId",gameId).replace(":playerSessionId",ps.getId()); //costruisce il link per eseguire una mossa nella partita
                var gamePath = GAME_RESOURCE_PATH.replace(":gameId", gameId); //costruisce il link per recuperare le informazioni della partita creata
                reply.put("moveLink", movePath); //popola l'oggetto con il link per eseguire la mossa
                reply.put("gameLink", gamePath); //popola l'oggetto con il link alla partita
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (InvalidMoveException ex) {
				reply.put("result", "invalid-move"); //popola l'oggetto con un'informazione di errore
				sendReply(context.response(), reply); //invia la risposta al client
			} catch (Exception ex1) {
				reply.put("result", ex1.getMessage()); //popola l'oggetto con la specifica dell'errore
				try {
					sendReply(context.response(), reply); //invia la risposta al client
				} catch (Exception ex2) {
					sendError(context.response()); //invia un errore al client
				}				
			}
		});
	}

    //registra un websocket handler al server
    protected void handleEventSubscription(HttpServer server) {
        server.webSocketHandler(webSocket -> { //registra un handlder per websocket
            logger.log(Level.INFO, "New TTT subscription accepted.");
            webSocket.textMessageHandler(openMsg -> { //imposta un handler per i messaggi ricevuti dal client
                logger.log(Level.INFO, "For game: " + openMsg);
                JsonObject obj = new JsonObject(openMsg); //converte il messaggio ricevuto dal client in un oggetto json
                String playerSessionId = obj.getString("playerSessionId"); //estrae il valore del campo "playerSessionId"

                EventBus eb = vertx.eventBus(); //recupera l'event bus di vertx
                eb.consumer(playerSessionId, msg -> { //iscrive l'event bus all'indirizzo creato e, ogni volta che arriva un messaggio all'event bus...
                    JsonObject ev = (JsonObject) msg.body(); //...lo converte in json
                    logger.log(Level.INFO, "Event: " + ev.encodePrettily());
                    webSocket.writeTextMessage(ev.encodePrettily()); //lo invia al client tramite websocket
                });

                var ps = gameService.getPlayerSession(playerSessionId); //recupera la sessione del giocatore corrispondente al servizio principale (ovvero l'utente in prima persona)
                var en = ps.getPlayerSessionEventNotifier(); //recupera il notificatore di eventi della sessione del giocatore
                en.enableEventNotification(playerSessionId); //abilita la notifica degli eventi per questa sessione sul bus con l'indirizzo "playerSessionId"
            });
        });
    }

    //invia la risposta al client
    private void sendReply(HttpServerResponse response, JsonObject reply) {
        response.putHeader("content-type", "application/json"); //imposta l’header del messaggio http come json
        response.end(reply.toString()); //converte l’oggetto json in stringa, lo invia al client e chiude la risposta
    }

    //invia una risposta di errore al client
    private void sendError(HttpServerResponse response) {
        response.setStatusCode(500);  //imposta lo stato della risposta a 500 (errore)
        response.putHeader("content-type", "application/json"); //imposta l’header del messaggio http come json
        response.end(); //chiude la risposta
    }
}
