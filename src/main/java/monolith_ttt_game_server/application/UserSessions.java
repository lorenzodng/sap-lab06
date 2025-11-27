package monolith_ttt_game_server.application;

import java.util.HashMap;
import common.ddd.Repository;

//tiene traccia delle sessioni di tutti gli utenti
public class UserSessions implements Repository {

	private HashMap<String, UserSession> userSessions; //hashmap che associa l'utente alla sessione
	
	public UserSessions() {
		userSessions = new HashMap<>();
	}

	//aggiunge una sessione
	public void addSession(UserSession us) {
		userSessions.put(us.getSessionId(), us);
	}

	//recupera la sessione dell'utente
	public UserSession getSession(String sessionId) {
		return userSessions.get(sessionId);
	}
	
}
