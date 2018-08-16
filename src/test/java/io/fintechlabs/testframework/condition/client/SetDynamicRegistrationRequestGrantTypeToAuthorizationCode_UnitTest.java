package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class SetDynamicRegistrationRequestGrantTypeToAuthorizationCode_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject noGrantTypeDynamicRegistrationRequest = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\""
		+ "}").getAsJsonObject();

	private static JsonObject existingGrantTypeDynamicRegistrationRequest = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_types\":[\"not-authorization_code\"]"
		+ "}").getAsJsonObject();

	private static JsonObject goodGrantTypeDynamicRegistrationRequest = new JsonParser().parse("{"
		+ "\"client_name\":\"UNIT-TEST client\","
		+ "\"grant_types\":[\"authorization_code\"]"
		+ "}").getAsJsonObject();


	private SetDynamicRegistrationRequestGrantTypeToAuthorizationCode cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new SetDynamicRegistrationRequestGrantTypeToAuthorizationCode("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test for {@link SetDynamicRegistrationRequestGrantTypeToAuthorizationCode#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noExistingGrantType() {
		testGrantType(noGrantTypeDynamicRegistrationRequest);
	}

	/**
	 * Test for {@link SetDynamicRegistrationRequestGrantTypeToAuthorizationCode#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_withExistingGrantType() {
		testGrantType(existingGrantTypeDynamicRegistrationRequest);
	}

	private void testGrantType(JsonObject dynamicRegistrationRequestObject) {
		env.putObject("dynamic_registration_request", dynamicRegistrationRequestObject);
		cond.evaluate(env);
		assertThat(env.getObject("dynamic_registration_request").equals(goodGrantTypeDynamicRegistrationRequest)).isTrue();
	}



}
