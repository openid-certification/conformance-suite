package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureSecurityEventTokenTxnClaimIsString_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureSecurityEventTokenTxnClaimIsString condition;

	@BeforeEach
	public void setUp() {
		condition = new OIDSSFEnsureSecurityEventTokenTxnClaimIsString();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private void setUpSetToken(JsonElement txn) {
		// CAEP 1.0 Figure 9, txn replaced per test
		JsonObject claims = JsonParser.parseString("""
			{
				"iss": "https://idp.example.com/3456789/",
				"jti": "07efd930f0977e4fcc1149a733ce7f78",
				"iat": 1615305159,
				"aud": "https://sp.example2.net/caep",
				"sub_id": {
					"format": "iss_sub",
					"iss": "https://idp.example.com/3456789/",
					"sub": "jane.smith@example.com"
				},
				"events": {
					"https://schemas.openid.net/secevent/caep/event-type/credential-change": {
						"credential_type": "fido2-roaming",
						"change_type": "create"
					}
				}
			}""").getAsJsonObject();
		if (txn != null) {
			claims.add("txn", txn);
		}
		JsonObject token = new JsonObject();
		token.add("claims", claims);
		env.putObject("set_token", token);
	}

	@Test
	void shouldPassWithSpecExampleTxn() {
		setUpSetToken(JsonParser.parseString("\"8675309\""));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldPassWhenTxnIsMissing() {
		setUpSetToken(null);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@Test
	void shouldFailWhenTxnIsEmptyString() {
		setUpSetToken(JsonParser.parseString("\"\""));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenTxnIsNumber() {
		setUpSetToken(JsonParser.parseString("8675309"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	void shouldFailWhenTxnIsObject() {
		setUpSetToken(JsonParser.parseString("{\"id\":\"8675309\"}"));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
