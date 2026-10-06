package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FAPIBrazilAddLoggedUserNameToConsentRequest_UnitTest {

	private Environment env;
	private JsonObject resource;
	private FAPIBrazilAddLoggedUserNameToConsentRequest condition;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		resource = new JsonObject();
		resource.addProperty("brazilCpf", "11111111111");
		resource.addProperty("brazilCnpj", "11111111111111");
		JsonObject config = new JsonObject();
		config.add("resource", resource);
		env.putObject("config", config);
		var create = new FAPIBrazilOpenBankingCreateConsentRequest();
		create.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
		create.execute(env);
		condition = new FAPIBrazilAddLoggedUserNameToConsentRequest();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Joaquim Silva", "João da Silva", " Joaquim Silva ", "名😀" })
	public void addsNameWithoutChangingOtherFields(String name) {
		JsonObject expected = env.getObject("consent_endpoint_request").deepCopy();
		JsonObject permissions = env.getObject("brazil_consent").deepCopy();
		expected.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", name);
		resource.addProperty("brazilLoggedUserName", name);

		validateConfiguration();
		condition.execute(env);

		assertEquals(expected, env.getObject("consent_endpoint_request"));
		assertEquals(permissions, env.getObject("brazil_consent"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	public void leavesRequestUnchangedWhenNameIsNotProvided(String name) {
		JsonObject expected = env.getObject("consent_endpoint_request").deepCopy();
		if (name != null) {
			resource.addProperty("brazilLoggedUserName", name);
		}

		validateConfiguration();
		condition.execute(env);

		assertEquals(expected, env.getObject("consent_endpoint_request"));
	}

	@Test
	public void leavesRequestUnchangedWhenNameIsJsonNull() {
		JsonObject expected = env.getObject("consent_endpoint_request").deepCopy();
		resource.add("brazilLoggedUserName", JsonParser.parseString("null"));
		validateConfiguration();
		condition.execute(env);
		assertEquals(expected, env.getObject("consent_endpoint_request"));
	}

	@Test
	public void acceptsFiftyCharacters() {
		String name = "😀".repeat(50);
		resource.addProperty("brazilLoggedUserName", name);
		validateConfiguration();
		condition.execute(env);
		assertEquals(name, env.getString("consent_endpoint_request", "data.loggedUser.name"));
	}

	private void validateConfiguration() {
		var validation = new FAPIBrazilValidateLoggedUserNameConfiguration();
		validation.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.FAILURE);
		validation.execute(env);
	}
}
