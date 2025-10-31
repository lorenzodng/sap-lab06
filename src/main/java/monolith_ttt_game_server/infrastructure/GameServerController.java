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

/**
*
* Game Server controller
* 
* - exposing a REST API
* 
*/
public class GameServerController extends VerticleBase  {

	private int port;
	static Logger logger = Logger.getLogger("[GameServerController]");

	static final String API_VERSION = "v1";
	static final String ACCOUNTS_RESOURCE_PATH = 		"/api/" + API_VERSION + "/accounts";
	static final String ACCOUNT_RESOURCE_PATH = 		"/api/" + API_VERSION + "/accounts/:accountId";
	static final String LOGIN_RESOURCE_PATH = 			"/api/" + API_VERSION + "/accounts/:accountId/login";
	static final String GAMES_RESOURCE_PATH = 			"/api/" + API_VERSION + "/games";
	static final String GAME_RESOURCE_PATH = 			"/api/" + API_VERSION + "/games/:gameId";
	static final String USER_SESSIONS_RESOURCE_PATH = 	"/api/" + API_VERSION + "/user-sessions";
	static final String CREATE_GAME_RESOURCE_PATH = USER_SESSIONS_RESOURCE_PATH + "/:sessionId/create-game";
	static final String JOIN_GAME_RESOURCE_PATH = USER_SESSIONS_RESOURCE_PATH + "/:sessionId/join-game";
	static final String PLAYER_MOVE_RESOURCE_PATH = GAME_RESOURCE_PATH + "/:playerSessionId/move";
	static final String WS_EVENT_CHANNEL_PATH = "/api/" + API_VERSION + "/events";
	
	private GameService gameService;
	
	
	public GameServerController(GameService service, int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
		this.gameService = service;

	}

	public Future<?> start() {
		logger.log(Level.INFO, "TTT Game Service initializing...");
		HttpServer server = vertx.createHttpServer();

		/* REST API routes */
				
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, ACCOUNTS_RESOURCE_PATH).handler(this::createNewAccount);
		router.route(HttpMethod.GET, ACCOUNT_RESOURCE_PATH).handler(this::getAccountInfo);
		router.route(HttpMethod.POST, LOGIN_RESOURCE_PATH).handler(this::login);
		router.route(HttpMethod.POST, CREATE_GAME_RESOURCE_PATH).handler(this::createNewGame);
		router.route(HttpMethod.GET, GAME_RESOURCE_PATH).handler(this::getGameInfo);
		router.route(HttpMethod.POST, JOIN_GAME_RESOURCE_PATH).handler(this::joinGame);
		router.route(HttpMethod.POST, PLAYER_MOVE_RESOURCE_PATH).handler(this::makeAMove);
		handleEventSubscription(server, WS_EVENT_CHANNEL_PATH);

		/* static files */
		
		router.route("/public/*").handler(StaticHandler.create());
		
		/* start the server */
		
		var fut = server
			.requestHandler(router)
			.listen(port);
		
		fut.onSuccess(res -> {
			logger.log(Level.INFO, "TTT Game Service ready - port: " + port);
		});

		return fut;
	}


	/* List of handlers mapping the API */
	
	/**
	 * 
	 * Register a new user
	 * 
	 * @param context
	 */
	protected void createNewAccount(RoutingContext context) {
		logger.log(Level.INFO, "create a new account");
		context.request().handler(buf -> {
			JsonObject userInfo = buf.toJsonObject();
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = userInfo.getString("userName");
			var password = userInfo.getString("password");
			var reply = new JsonObject();
			try {
				gameService.registerUser(userName, password);
				reply.put("result", "ok");
				var accountPath = ACCOUNT_RESOURCE_PATH.replace(":accountId", userName);
				var loginPath = LOGIN_RESOURCE_PATH.replace(":accountId", userName);
				reply.put("loginLink", loginPath);
				reply.put("accountLink", accountPath);				
				sendReply(context.response(), reply);
			} catch (AccountAlreadyPresentException ex) {
				reply.put("result", "error");
				reply.put("error", ex.getMessage());
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}
		});
	}

	/**
	 * 
	 * Get account info
	 * 
	 * @param context
	 */
	protected void getAccountInfo(RoutingContext context) {
		logger.log(Level.INFO, "get account info");
		// context.request().handler(buf -> {
			var userName = context.pathParam("accountId");
			var reply = new JsonObject();
			try {
				var acc = gameService.getAccountInfo(userName);
				reply.put("result", "ok");
				var accJson = new JsonObject();
				accJson.put("userName", acc.getUserName());
				accJson.put("password", acc.getPassword());
				accJson.put("whenCreated", acc.getWhenCreated());
				reply.put("accountInfo", accJson);			
				sendReply(context.response(), reply);
			} catch (AccountNotFoundException ex) {
				reply.put("result", "error");
				reply.put("error", "account-not-present");
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}
		// });
	}
	
	/**
	 * 
	 * Login a user
	 * 
	 * It creates a User Session
	 * 
	 * @param context
	 */
	protected void login(RoutingContext context) {
		logger.log(Level.INFO, "Login request");
		context.request().handler(buf -> {
			JsonObject userInfo = buf.toJsonObject();
			logger.log(Level.INFO, "Payload: " + userInfo);
			var userName = context.pathParam("accountId");
			var password = userInfo.getString("password");
			var reply = new JsonObject();
			try {
				var session = gameService.login(userName, password);
				reply.put("result", "ok");
				var createPath = CREATE_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId());
				var joinPath = JOIN_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId());
				reply.put("createGameLink", createPath);
				reply.put("joinGameLink", joinPath);
				reply.put("sessionId", session.getSessionId());
				reply.put("sessionLink", USER_SESSIONS_RESOURCE_PATH + "/" + session.getSessionId());				
				sendReply(context.response(), reply);
			} catch (LoginFailedException ex) {
				reply.put("result", "login-failed");
				reply.put("error", ex.getMessage());
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}			
		});
	}
	
	
	/**
	 * 
	 * Create a New Game - by users logged in (with a UserSession)
	 * 
	 * @param context
	 */
	protected void createNewGame(RoutingContext context) {
		logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			JsonObject userInfo = buf.toJsonObject();
			var sessionId = context.pathParam("sessionId");
			var reply = new JsonObject();
			try {
				var session = gameService.getUserSession(sessionId);
				var gameId = userInfo.getString("gameId");
				session.createNewGame(gameId);
				reply.put("result", "ok");
				reply.put("gameLink", GAMES_RESOURCE_PATH + "/" + gameId);
				var joinPath = JOIN_GAME_RESOURCE_PATH.replace(":sessionId", session.getSessionId());
				reply.put("joinGameLink", joinPath);
				sendReply(context.response(), reply);
			} catch (GameAlreadyPresentException ex) {
				reply.put("result", "error");
				reply.put("error", "game-already-present");
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}			
		});		
	}

	/**
	 * 
	 * Get game info
	 * 
	 * @param context
	 */
	protected void getGameInfo(RoutingContext context) {
		logger.log(Level.INFO, "get game info");
		// context.request().handler(buf -> {
			var gameId = context.pathParam("gameId");
			var reply = new JsonObject();
			try {
				var game = gameService.getGameInfo(gameId);
				reply.put("result", "ok");
				var gameJson = new JsonObject();
				gameJson.put("gameId", game.getId());
				gameJson.put("gameState", game.getGameState());
				if (game.isStarted() || game.isFinished()) {
					var bs = game.getBoardState();
					JsonArray array = new JsonArray();
					for (var el: bs) {
						array.add(el);
					}
					gameJson.put("boardState", array);
				}
				if (game.isStarted()) {
					gameJson.put("turn", game.getCurrentTurn());
				}
				reply.put("gameInfo", gameJson);			
				sendReply(context.response(), reply);
			} catch (GameNotFoundException ex) {
				reply.put("result", "error");
				reply.put("error", "game-not-present");
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}
		// });
	}
	
	/**
	 * 
	 * Join a Game - by user logged in (with a UserSession)
	 * 
	 * It creates a PlayerSession
	 * 
	 * @param context
	 */
	protected void joinGame(RoutingContext context) {
		logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			JsonObject joinInfo = buf.toJsonObject();
			logger.log(Level.INFO, "Join info: " + joinInfo);
			
			String sessionId = context.pathParam("sessionId");
			String gameId = joinInfo.getString("gameId");
			String symbol = joinInfo.getString("symbol");

			var reply = new JsonObject();
			try {
				var session = gameService.getUserSession(sessionId);
				var playerSession = session.joinGame(gameId, symbol.equals("X") ? TTTSymbol.X : TTTSymbol.O, new VertxPlayerSessionEventObserver(vertx.eventBus()));
				var movePath = PLAYER_MOVE_RESOURCE_PATH
									.replace(":gameId", gameId)
									.replace(":playerSessionId",playerSession.getId());
				reply.put("playerSessionId", playerSession.getId());
				reply.put("moveLink", movePath);
				reply.put("gameLink", GAMES_RESOURCE_PATH + "/" + gameId);
				reply.put("playerSessionLink", GAMES_RESOURCE_PATH + "/" + gameId + "/player-" + symbol);
				reply.put("eventChannelLink", WS_EVENT_CHANNEL_PATH);
				reply.put("result", "ok");
				sendReply(context.response(), reply);
			} catch (InvalidJoinException  ex) {
				reply.put("result", "error");
				reply.put("error", ex.getMessage());
				sendReply(context.response(), reply);
			} catch (Exception ex1) {
				sendError(context.response());
			}			
		});
	}
	
	/**
	 * 
	 * Make a move in a game - by players playing a game (with a PlayerSession)
	 * 
	 * @param context
	 */
	protected void makeAMove(RoutingContext context) {
		logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			var  reply = new JsonObject();
			try {
				JsonObject moveInfo = buf.toJsonObject();
				logger.log(Level.INFO, "move info: " + moveInfo);				
				String playerSessionId = context.pathParam("playerSessionId");
				int x = Integer.parseInt(moveInfo.getString("x"));
				int y = Integer.parseInt(moveInfo.getString("y"));
				var ps = gameService.getPlayerSession(playerSessionId);
				ps.makeMove(x, y);				
				reply.put("result", "accepted");
				var gameId = context.pathParam("gameId");
				var movePath = PLAYER_MOVE_RESOURCE_PATH
						.replace(":gameId",gameId)
						.replace(":playerSessionId",ps.getId());
				reply.put("moveLink", movePath);
				reply.put("gameLink", GAMES_RESOURCE_PATH + "/" + gameId);
				sendReply(context.response(), reply);
			} catch (InvalidMoveException ex) {
				reply.put("result", "invalid-move");
				sendReply(context.response(), reply);				
			} catch (Exception ex1) {
				reply.put("result", ex1.getMessage());
				try {
					sendReply(context.response(), reply);
				} catch (Exception ex2) {
					sendError(context.response());
				}				
			}
		});
	}


	/* Handling subscribers using web sockets */
	
	protected void handleEventSubscription(HttpServer server, String path) {
		server.webSocketHandler(webSocket -> {
			logger.log(Level.INFO, "New TTT subscription accepted.");

			/* 
			 * 
			 * Receiving a first message including the id of the game
			 * to observe 
			 * 
			 */
			webSocket.textMessageHandler(openMsg -> {
				logger.log(Level.INFO, "For game: " + openMsg);
				JsonObject obj = new JsonObject(openMsg);
				String playerSessionId = obj.getString("playerSessionId");
				
				
				/* 
				 * Subscribing events on the event bus to receive
				 * events concerning the game, to be notified 
				 * to the frontend using the websocket
				 * 
				 */
				EventBus eb = vertx.eventBus();
				
				eb.consumer(playerSessionId, msg -> {
					JsonObject ev = (JsonObject) msg.body();
					logger.log(Level.INFO, "Event: " + ev.encodePrettily());
					webSocket.writeTextMessage(ev.encodePrettily());
				});
				
				var ps = gameService.getPlayerSession(playerSessionId);
				var en = ps.getPlayerSessionEventNotifier();
				en.enableEventNotification(playerSessionId);
								
			});
		});
	}
	
	/* Aux methods */
	

	private void sendReply(HttpServerResponse response, JsonObject reply) {
		response.putHeader("content-type", "application/json");
		response.end(reply.toString());
	}
	
	private void sendError(HttpServerResponse response) {
		response.setStatusCode(500);
		response.putHeader("content-type", "application/json");
		response.end();
	}


}
