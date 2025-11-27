package monolith_ttt_game_server.application;

import common.exagonal.InBoundPort;
import monolith_ttt_game_server.domain.Account;
import monolith_ttt_game_server.domain.Game;
import monolith_ttt_game_server.domain.InvalidJoinException;
import monolith_ttt_game_server.domain.TTTSymbol;
import monolith_ttt_game_server.domain.UserId;

//interfaccia che contiene tutti i metodi che il client può richiamare per interagire con il sistema
@InBoundPort
public interface GameService  {

	//registra un utente al servizio
	Account registerUser(String userName, String password) throws AccountAlreadyPresentException;

	//recupera le informazioni dell'account
	Account getAccountInfo(String userName) throws AccountNotFoundException;

	//recupera le informazioni della partita
	Game getGameInfo(String gameId) throws GameNotFoundException;

	//esegue il login di un utente al servizio
	UserSession login(String userName, String password) throws LoginFailedException;

	//recupera la sessione dell'utente
	UserSession getUserSession(String sessionId);

	//recupera la sessione del giocatore
    PlayerSession getPlayerSession(String sessionId);

	//crea una nuova partita
	void createNewGame(String gameId) throws GameAlreadyPresentException;

	//esegue il join di un utente ad un partita
	PlayerSession joinGame(UserId userId, String gameId, TTTSymbol symbol, PlayerSessionEventObserver observer) throws InvalidJoinException;

	//definisce un repository per gli account
    void bindAccountRepository(AccountRepository repo);

	//definisce un repository per le partite
    void bindGameRepository(GameRepository repo);
    
}
