package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureSdJwtVcVctMatchesCredentialConfiguration_UnitTest {

	private EnsureSdJwtVcVctMatchesCredentialConfiguration cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new EnsureSdJwtVcVctMatchesCredentialConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	private void putSdJwt(String vct) {
		JsonObject claims = new JsonObject();
		if (vct != null) {
			claims.addProperty("vct", vct);
		}
		JsonObject credential = new JsonObject();
		credential.add("claims", claims);
		JsonObject sdjwt = new JsonObject();
		sdjwt.add("credential", credential);
		env.putObject("sdjwt", sdjwt);
	}

	private void putCredentialConfiguration(String vct) {
		JsonObject config = new JsonObject();
		config.addProperty("format", "dc+sd-jwt");
		if (vct != null) {
			config.addProperty("vct", vct);
		}
		env.putObject("vci_credential_configuration", config);
	}

	@Test
	public void testEvaluate_passesWhenVctMatches() {
		putSdJwt("urn:eudi:pid:1");
		putCredentialConfiguration("urn:eudi:pid:1");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_failsWhenVctDiffers() {
		putSdJwt("urn:eudi:pid:1");
		putCredentialConfiguration("https://credentials.example.com/identity_credential");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not match"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenConfigurationHasNoVct() {
		putSdJwt("urn:eudi:pid:1");
		putCredentialConfiguration(null);

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("vct"), e.getMessage());
	}

	@Test
	public void testEvaluate_failsWhenCredentialHasNoVct() {
		putSdJwt(null);
		putCredentialConfiguration("urn:eudi:pid:1");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("vct"), e.getMessage());
	}
}
