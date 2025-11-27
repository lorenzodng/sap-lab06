package monolith_ttt_game_server.domain;

import common.ddd.Entity;

//account utente
public class Account implements Entity<String> {
	
	private String userName; /* this is the id */
	private String password;
	private long whenCreated;;
	
	public Account(String userName, String password) {
		this.userName = userName;
		this.password = password;
		this.whenCreated = System.currentTimeMillis();
	}

	public Account(String userName, String password, long whenCreated) {
		this.userName = userName;
		this.password = password;
		this.whenCreated = whenCreated;
	}

	
	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public void updatePassword(String password) {
		this.password = password;
	}
	
	public long getWhenCreated() {
		return this.whenCreated;
	}

	@Override
	public String getId() {
		return userName;
	}
	
}
