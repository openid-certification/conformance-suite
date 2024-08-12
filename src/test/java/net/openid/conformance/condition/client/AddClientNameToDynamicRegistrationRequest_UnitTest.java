package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AddClientNameToDynamicRegistrationRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;


	private AddClientNameToDynamicRegistrationRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddClientNameToDynamicRegistrationRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		JsonObject dynamicRegistrationRequest = new JsonObject();

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);
	}

	@Test
	public void testEvaluate_noClientName(){
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("OIDF Conformance Test UNIT-TEST");
	}

	@Test
	public void testEvaluate_withClientName(){
		env.putString("client_name","my-client-name");
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request")).isNotNull();
		assertThat(env.getString("dynamic_registration_request","client_name")).isEqualTo("my-client-name UNIT-TEST");
	}

}
