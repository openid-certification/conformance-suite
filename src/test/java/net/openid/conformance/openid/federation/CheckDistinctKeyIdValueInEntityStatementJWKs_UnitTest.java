package net.openid.conformance.openid.federation;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckDistinctKeyIdValueInEntityStatementJWKs_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckDistinctKeyIdValueInEntityStatementJWKs condition;

	@BeforeEach
	public void setUp() {
		condition = new CheckDistinctKeyIdValueInEntityStatementJWKs();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsDistinctKids() {
		env.putObject("ec_jwks", JsonParser.parseString("""
			{
			  "keys": [
			    { "kid": "kid-1" },
			    { "kid": "kid-2" }
			  ]
			}
			""").getAsJsonObject());

		condition.execute(env);
	}

	@Test
	public void rejectsDuplicateKids() {
		env.putObject("ec_jwks", JsonParser.parseString("""
			{
			  "keys": [
			    { "kid": "same-kid" },
			    { "kid": "same-kid" }
			  ]
			}
			""").getAsJsonObject());

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void acceptsKeysWithoutKid() {
		// Keys without kid are silently skipped by the uniqueness check
		env.putObject("ec_jwks", JsonParser.parseString("""
			{
			  "keys": [
			    { "kty": "EC" },
			    { "kty": "EC" }
			  ]
			}
			""").getAsJsonObject());

		condition.execute(env);
	}
}
