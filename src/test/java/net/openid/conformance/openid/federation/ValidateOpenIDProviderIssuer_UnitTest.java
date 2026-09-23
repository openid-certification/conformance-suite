package net.openid.conformance.openid.federation;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateOpenIDProviderIssuer_UnitTest {

	private Environment env;
	private ValidateOpenIDProviderIssuer condition;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putObject("primary_entity_statement_jwt", JsonParser.parseString("""
			{
				"claims": {
					"iss": "https://op.example:8443/test/federation",
					"metadata": { "openid_provider": {} }
				}
			}
			""").getAsJsonObject());
		condition = new ValidateOpenIDProviderIssuer();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://op.example:8443/test/federation", "https://op.example:8443/test/federation/"})
	public void acceptsExactMatch(String entityIdentifier) {
		env.putString("primary_entity_statement_jwt", "claims.iss", entityIdentifier);
		metadata().addProperty("issuer", entityIdentifier);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	public void rejectsMissingIssuer() {
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {"null", "123", "true", "[]", "{}"})
	public void rejectsNonStringIssuer(String issuerJson) {
		metadata().add("issuer", JsonParser.parseString(issuerJson));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "https://other.example", "https://op.example:8443/test/federation/"})
	public void rejectsIssuerMismatch(String issuer) {
		metadata().addProperty("issuer", issuer);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	private JsonObject metadata() {
		return env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata.openid_provider").getAsJsonObject();
	}

}
