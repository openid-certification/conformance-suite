package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FAPIBrazilValidateLoggedUserNameConfiguration_UnitTest {

	private Environment env;
	private JsonObject resource;
	private FAPIBrazilValidateLoggedUserNameConfiguration condition;

	@BeforeEach
	void setUp() {
		env = new Environment();
		resource = new JsonObject();
		resource.addProperty("brazilCpf", "11111111111");
		resource.addProperty("brazilCnpj", "11111111111111");
		JsonObject config = new JsonObject();
		config.add("resource", resource);
		env.putObject("config", config);
		condition = new FAPIBrazilValidateLoggedUserNameConfiguration();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.FAILURE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Joaquim Silva", "João da Silva", " Joaquim Silva ", "名😀" })
	void preservesConfiguredName(String name) {
		resource.addProperty("brazilLoggedUserName", name);
		condition.execute(env);
		assertEquals(name, env.getString("brazil_logged_user_name"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	void clearsPreviousNameWhenOmitted(String name) {
		env.putString("brazil_logged_user_name", "Previous name");
		if (name != null) {
			resource.addProperty("brazilLoggedUserName", name);
		}
		condition.execute(env);
		assertNull(env.getString("brazil_logged_user_name"));
	}

	@Test
	void treatsJsonNullAsOmitted() {
		resource.add("brazilLoggedUserName", JsonParser.parseString("null"));
		condition.execute(env);
		assertNull(env.getString("brazil_logged_user_name"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "42", "true", "{}", "[]" })
	void rejectsNonStringName(String json) {
		resource.add("brazilLoggedUserName", JsonParser.parseString(json));
		assertConfigurationError();
	}

	@Test
	void acceptsFiftyUnicodeCharacters() {
		String name = "😀".repeat(50);
		resource.addProperty("brazilLoggedUserName", name);
		condition.execute(env);
		assertEquals(name, env.getString("brazil_logged_user_name"));
	}

	@Test
	void rejectsMoreThanFiftyCharacters() {
		resource.addProperty("brazilLoggedUserName", "a".repeat(51));
		assertConfigurationError();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " " })
	void rejectsNameWithoutCpf(String cpf) {
		resource.addProperty("brazilLoggedUserName", "Joaquim Silva");
		if (cpf == null) {
			resource.remove("brazilCpf");
		} else {
			resource.addProperty("brazilCpf", cpf);
		}
		assertConfigurationError();
	}

	private void assertConfigurationError() {
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("Logged-in user name"));
		assertTrue(error.getMessage().contains("in the test configuration"));
	}
}
