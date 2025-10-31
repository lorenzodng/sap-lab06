package distributed_ttt.lobby_service.infrastructure;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;

import common.exagonal.Adapter;
import distributed_ttt.lobby_service.application.AccountService;
import distributed_ttt.lobby_service.application.ServiceNotAvailableException;
import distributed_ttt.lobby_service.application.UserNotFoundException;
import distributed_ttt.lobby_service.domain.User;

import java.net.http.HttpResponse;

import io.vertx.core.json.JsonObject;

@Adapter
public class AccountServiceProxy implements AccountService {

	private String serviceURI;
	
	public AccountServiceProxy(String serviceAPIEndpoint)  {
		this.serviceURI = serviceAPIEndpoint;		
	}
	
	@Override
	public boolean isValidPassword(String userName, String password) throws UserNotFoundException, ServiceNotAvailableException {
			try {
	            HttpClient client = HttpClient.newHttpClient();

	            JsonObject body = new JsonObject();
	            body.put("password", password);
	            		
	            String isValidReq = serviceURI + "/api/v1/accounts/" + userName + "/check-pwd";
	            HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(isValidReq))
	                    .header("Accept", "application/json")
	                    .POST(BodyPublishers.ofString(body.toString()))
	                    .build();

	            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
	            System.out.println("Response Code: " + response.statusCode());

	            if (response.statusCode() == 200) {
	                JsonObject json = new JsonObject(response.body());	                
	               
	                var res = json.getString("result");
	                if (res.equals("valid-password")) {
	                	return true;
	                } else {
	                	return false;
	                }
	            } else {
	                System.out.println("POST request failed: " + response.body());
	                return false;
	            }

		} catch (Exception ex) {
			throw new ServiceNotAvailableException();
		}
	}


}
