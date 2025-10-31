package monolith_ttt_game_server.application;

import java.util.HashMap;
import java.util.logging.Logger;

import common.ddd.Aggregate;
import common.ddd.Repository;
import monolith_ttt_game_server.domain.Account;
import monolith_ttt_game_server.domain.UserId;

/**
 * 
 * User accounts.
 * 
 */
public class UserSessions implements Repository {
	static Logger logger = Logger.getLogger("[SessionRepo]");

	private HashMap<String, UserSession> userSessions;
	
	public UserSessions() {
		userSessions = new HashMap<>();
	}
	
	public void addSession(UserSession us) {
		userSessions.put(us.getSessionId(), us);
	}

	public UserSession getSession(String sessionId) {
		return userSessions.get(sessionId);
	}
	
}
