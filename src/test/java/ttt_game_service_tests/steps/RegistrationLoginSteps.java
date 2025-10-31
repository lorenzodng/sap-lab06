package ttt_game_service_tests.steps;

import io.cucumber.java.en.*;
import monolith_ttt_game_server.application.AccountAlreadyPresentException;
import monolith_ttt_game_server.application.GameService;
import monolith_ttt_game_server.application.GameServiceImpl;
import monolith_ttt_game_server.infrastructure.InMemoryAccountRepository;
import monolith_ttt_game_server.infrastructure.InMemoryGameRepository;

import static org.assertj.core.api.Assertions.*;

public class RegistrationLoginSteps {

    private String currentPage = "";
    private String lastError = "";
    private String lastInfo = "";

	private GameService gs;
	
    
	public RegistrationLoginSteps(){
		gs = new GameServiceImpl(); 
		gs.bindAccountRepository(new InMemoryAccountRepository());
		gs.bindGameRepository(new InMemoryGameRepository());
	}
	
    /* Scenario: Successful registration */
    
    @Given("I am on the registration page")
    public void i_am_on_the_registration_page() {
        currentPage = "registration";
    }

    @Given("I have not registered before")
    public void i_have_not_registered_before() {
    }

    @When("I register with a unique username {string} and a valid password {string}")
    public void i_register_with_unique_username_and_valid_password(String username, String pwd) {
        assertThat(currentPage).isEqualTo("registration");
        try {
        	gs.registerUser(username, pwd);        	
        } catch (AccountAlreadyPresentException ex) {
            lastError = "Username already taken";
            return;
        }
        lastInfo = "Account created";
    }

    @Then("I should see a confirmation that my account was created")
    public void i_should_see_confirmation() {
        assertThat(lastInfo).isEqualTo("Account created");
    }
    
   /* Scenario: Registration fails with invalid data */  
    
    @Given("Someone already registered with a username {string}")
    public void someone_already_registered_with_username(String username) {
    	try {
        	gs.registerUser(username, "any");        	
        } catch (AccountAlreadyPresentException ex) {
            ex.printStackTrace();
            return;
        }    
    }
    
    @When("I register with username {string} and any password")
    public void i_register_with_username_and_any_password(String username) {
        i_register_with_unique_username_and_valid_password(username, "any");
    }

    @Then("I should see an error {string}")
    public void i_should_see_an_error(String message) {
        assertThat(lastError).isEqualTo(message);
    }

    @Then("my account should not be created")
    public void my_account_should_not_be_created() {
        assertThat(lastError).isNotBlank();
    }
    

}
