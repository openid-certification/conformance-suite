package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FAPIBrazilValidateCibaConsentLoggedUserName_UnitTest {

	private Environment env;
	private JsonObject request;
	private FAPIBrazilValidateCibaConsentLoggedUserName condition;

	@BeforeEach
	void setUp() {
		env = new Environment();
		request = JsonParser.parseString("""
			{"data": {
				"loggedUser": {"document": {"identification": "11111111111", "rel": "CPF"}},
				"businessEntity": {"document": {"identification": "11111111111111", "rel": "CNPJ"}}
			}}
			""").getAsJsonObject();
		env.putObject("new_consent_request", request);
		condition = new FAPIBrazilValidateCibaConsentLoggedUserName();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.FAILURE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Joaquim Silva", "João da Silva", " Joaquim Silva ", "名😀" })
	void acceptsCorporateNameWithoutChangingRequest(String name) {
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", name);
		JsonObject original = request.deepCopy();
		condition.execute(env);
		assertEquals(original, request);
	}

	@Test
	void rejectsCorporateRequestWithoutName() {
		assertInvalidName();
	}

	@Test
	void rejectsCorporateRequestWithoutLoggedUser() {
		request.getAsJsonObject("data").remove("loggedUser");
		assertInvalidName();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\t\n" })
	void rejectsBlankCorporateName(String name) {
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", name);
		assertInvalidName();
	}

	@ParameterizedTest
	@ValueSource(strings = { "null", "42", "true", "{}", "[]" })
	void rejectsNonStringName(String name) {
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").add("name", JsonParser.parseString(name));
		assertInvalidName();
		request.getAsJsonObject("data").remove("businessEntity");
		assertInvalidName();
	}

	@Test
	void acceptsFiftyUnicodeCharacters() {
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", "😀".repeat(50));
		condition.execute(env);
	}

	@Test
	void rejectsNameLongerThanFiftyCharacters() {
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", "a".repeat(51));
		assertInvalidName();
		request.getAsJsonObject("data").remove("businessEntity");
		assertInvalidName();
	}

	@Test
	void acceptsPersonalRequestWithoutNameDespitePreviousCorporateCnpj() {
		env.putString("consent_request_cnpj", "11111111111111");
		request.getAsJsonObject("data").remove("businessEntity");
		condition.execute(env);
	}

	@Test
	void acceptsPersonalName() {
		request.getAsJsonObject("data").remove("businessEntity");
		request.getAsJsonObject("data").getAsJsonObject("loggedUser").addProperty("name", "Joaquim Silva");
		condition.execute(env);
	}

	private void assertInvalidName() {
		ConditionError error = assertThrows(ConditionError.class, () -> condition.execute(env));
		assertTrue(error.getMessage().contains("data.loggedUser.name"));
	}
}
