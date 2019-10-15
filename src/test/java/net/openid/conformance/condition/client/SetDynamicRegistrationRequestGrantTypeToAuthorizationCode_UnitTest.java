package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
		cond = new SetDynamicRegistrationRequestGrantTypeToAuthorizationCode();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
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
		cond.execute(env);
		assertThat(env.getObject("dynamic_registration_request").equals(goodGrantTypeDynamicRegistrationRequest)).isTrue();
	}



}
