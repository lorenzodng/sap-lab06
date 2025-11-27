package monolith_ttt_game_server.application;

import java.util.logging.Level;
import java.util.logging.Logger;
import monolith_ttt_game_server.domain.Account;
import monolith_ttt_game_server.domain.Game;
import monolith_ttt_game_server.domain.InvalidJoinException;
import monolith_ttt_game_server.domain.TTTSymbol;
import monolith_ttt_game_server.domain.UserId;

//servizio applicativo principale (gestisce il flusso di gioco)
public class GameServiceImpl implements GameService {

	static Logger logger = Logger.getLogger("[Game Service]");
	private AccountRepository accountRepository; //repository degli account
	private GameRepository gameRepository; //repository delle partite in corso
	private UserSessions userSessionRepository; //repository delle sessioni degli utenti
	private PlayerSessions playerSessionRepository; //repository delle sessioni dei giocatori
	private int sessionCount; //numero di sessioni utente
	private int playerSessionCount; //numero di sessioni giocatore
    
    public GameServiceImpl(){
    	userSessionRepository = new UserSessions();
    	playerSessionRepository = new PlayerSessions();
    	sessionCount = 0;
    	playerSessionCount = 0;
    }

	//registra un utente al servizio
	public Account registerUser(String userName, String password) throws AccountAlreadyPresentException {
		logger.log(Level.INFO, "Register User: " + userName + " " + password);		
		if (accountRepository.isPresent(userName)) { //se l'utente esiste già
			throw new AccountAlreadyPresentException(); //lancia un'eccezione
		}
		var account = new Account(userName, password); //crea l'account
		accountRepository.addAccount(account); //lo aggiunge
		return account; //restituisce l'account creato
	}

	//esegue il login di un utente al servizio
	public UserSession login(String userName, String password) throws LoginFailedException {
		logger.log(Level.INFO, "Login: " + userName + " " + password);
		if (!accountRepository.isValid(userName, password)) { //se i dati non sono corretti
			throw new LoginFailedException(); //lancia un'eccezione
		}
		var id = new UserId(userName); //crea l'utente
		sessionCount++; //incrementa il numero di sessioni utente
		var sessionId = "user-session-" + sessionCount; //crea un id per la sessione
		var us = new UserSession(sessionId, id, this); //crea la sessione
		userSessionRepository.addSession(us); //aggiunge la sessione
		return us; //restituisce la sessione creata
	}

	//crea una nuova partita
	public void createNewGame(String gameId) throws GameAlreadyPresentException {
		logger.log(Level.INFO, "create New Game " + gameId);
		var game = new Game(gameId);
		if (gameRepository.isPresent(gameId)) { //se la partita esiste già (è già avviata)
			throw new GameAlreadyPresentException(); //lancia un'eccezione
		}
		gameRepository.addGame(game); //avvia la partita
	}

	//fa entrare un utente in una partita
	public PlayerSession joinGame(UserId userId, String gameId, TTTSymbol symbol, PlayerSessionEventObserver notifier) throws InvalidJoinException  {
		logger.log(Level.INFO, "JoinGame - user: " + userId + " game: " + gameId + " symbol " + symbol);
		var game = gameRepository.getGame(gameId); //recupera la partita
		game.joinGame(userId, symbol); //fa entrare l'utente nella partita indicata
		playerSessionCount++; //incrementa il numero di sessioni giocatore
		var playerSessionId = "player-session-" + playerSessionCount; //crea un id per la sessione
		var ps = new PlayerSession(playerSessionId, userId, game, symbol);  //crea la sessione
		ps.bindPlayerSessionEventNotifier(notifier); //definisce un observer per la sessione
		playerSessionRepository.addSession(ps); //aggiunge la sessione
		game.addGameObserver(ps); //aggiunge l'observer
		if (game.isReadyToStart()) { //se la partita può iniziare
			game.startGame(); //avvia la partita
		}
		return ps; //restituisce la sessione giocatore
	}

	//recupera un account
	public Account getAccountInfo(String userName) throws AccountNotFoundException {
		logger.log(Level.INFO, "Get account info: " + userName);
		if (!accountRepository.isPresent(userName)) { //se l'account non esiste
			throw new AccountNotFoundException(); //lancia un'eccezione
		}
		return accountRepository.getAccount(userName); //restituisce l'account
	}

	//recupera una partita
	public Game getGameInfo(String gameId) throws GameNotFoundException {
		logger.log(Level.INFO, "create New Game " + gameId);
		if (!gameRepository.isPresent(gameId)) { //se la partita non esiste
			throw new GameNotFoundException(); //lancia un'eccezione
		}
		return gameRepository.getGame(gameId); //restituisce la partita
	}

	//recupera una sessione utente
	public UserSession getUserSession(String sessionId) {
		return userSessionRepository.getSession(sessionId);
	}

	//recupera una sessione giocatore
	public PlayerSession getPlayerSession(String sessionId) {
		return playerSessionRepository.getSession(sessionId);
	}

	//definisce un repository per gli account
    public void bindAccountRepository(AccountRepository repo) {
    	this.accountRepository = repo;
    }

	//definisce un repository per le partite
    public void bindGameRepository(GameRepository repo) {
    	this.gameRepository = repo;
    }
}
