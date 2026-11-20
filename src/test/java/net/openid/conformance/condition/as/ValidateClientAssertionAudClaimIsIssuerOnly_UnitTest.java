package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidateClientAssertionAudClaimIsIssuerOnly_UnitTest {

	private static final String ISSUER = "https://as.example.com/";

	private final Environment env = new Environment();
	private final JsonObject server = new JsonObject();
	private final JsonObject claims = new JsonObject();
	private final ValidateClientAssertionAudClaimIsIssuerOnly cond = new ValidateClientAssertionAudClaimIsIssuerOnly();

	@BeforeEach
	public void setUp() {
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO, new String[0]);
		server.addProperty("issuer", ISSUER);
		server.addProperty("token_endpoint", "https://as.example.com/token");
		server.addProperty("backchannel_authentication_endpoint", "https://as.example.com/backchannel");
		env.putObject("server", server);
		JsonObject assertion = new JsonObject();
		assertion.add("claims", claims);
		env.putObject("client_assertion", assertion);
	}

	private void setAud(String json) {
		JsonElement aud = JsonParser.parseString(json);
		claims.add("aud", aud);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"\"https://as.example.com/\"",
		"[\"https://as.example.com/\"]"
	})
	public void acceptsIssuerAsSoleAudience(String aud) {
		setAud(aud);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		// the endpoint URLs that older specifications allowed
		"\"https://as.example.com/token\"",
		"\"https://as.example.com/backchannel\"",
		"[\"https://as.example.com/token\"]",
		// the issuer is present but is not the only value
		"[\"https://as.example.com/\", \"https://as.example.com/token\"]",
		"[\"https://as.example.com/\", \"https://other.example.com/\"]",
		"[\"https://as.example.com/\", \"https://as.example.com/\"]",
		"[\"https://as.example.com/\", 123]",
		// values that only resemble the issuer
		"\"https://as.example.com\"",
		"\"https://as.example.com//\"",
		"\"HTTPS://AS.EXAMPLE.COM/\"",
		"\"https://as.example.com/ \"",
		"\"\"",
		"[\"https://as.example.com\"]",
		// not a string or an array of strings
		"[]",
		"[null]",
		"[123]",
		"[[\"https://as.example.com/\"]]",
		"null",
		"123",
		"true",
		"{}"
	})
	public void rejectsAnythingElse(String aud) {
		setAud(aud);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void rejectsMissingAud() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void rejectsMissingIssuerInServerConfiguration(String issuer) {
		server.addProperty("issuer", issuer);
		claims.addProperty("aud", ISSUER);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void errorNamesTheIssuerWhenItIsNotTheOnlyValue() {
		setAud("[\"https://as.example.com/\", \"https://as.example.com/token\"]");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("other values"), e.getMessage());
	}
}
