package monolith_ttt_game_server.application;

import common.ddd.Repository;
import common.exagonal.OutBoundPort;
import monolith_ttt_game_server.domain.Account;

/**
 * 
 * Interface of account repository
 * 
 */
@OutBoundPort
public interface AccountRepository extends Repository {

	void addAccount(Account account);
	
	boolean isPresent(String userName);

	Account getAccount(String userName) throws AccountNotFoundException;
	
	boolean isValid(String userName, String password);
}
