package distributed_ttt.lobby_service.application;

import common.exagonal.OutBoundPort;
import distributed_ttt.lobby_service.domain.User;

@OutBoundPort

public interface AccountService  {
	
	/**
	 * 
	 * Check password validity
	 * 
	 * @param userName
	 * @param password
	 * @return
	 * @throws UserNotFoundException
	 */
	boolean isValidPassword(String userName, String password) 
			throws UserNotFoundException, ServiceNotAvailableException;;

    
}
