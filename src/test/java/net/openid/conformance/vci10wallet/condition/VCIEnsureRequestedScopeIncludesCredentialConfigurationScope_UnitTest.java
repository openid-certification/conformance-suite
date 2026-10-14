package net.openid.conformance.vci10wallet.condition;

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
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VCIEnsureRequestedScopeIncludesCredentialConfigurationScope_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureRequestedScopeIncludesCredentialConfigurationScope cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureRequestedScopeIncludesCredentialConfigurationScope();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putObject("credential_configuration_id_scope_map", JsonParser.parseString("""
			{
			  "eudi.pid.1": ["eu.europa.ec.eudi.pid.1"],
			  "eudi.pid.mdoc.1": ["eu.europa.ec.eudi.pid.mdoc.1"],
			  "openid.example.cert.1": ["net.openid.examples.certification.1.sdjwtvc", "net.openid.examples.certification.1.mdoc"]
			}
			""").getAsJsonObject());
	}

	@Test
	public void singleCredentialScope_passes() {
		env.putString("scope", "eudi.pid.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void credentialScopePlusOtherValue_passes() {
		env.putString("scope", "offline_access eudi.pid.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void noCredentialScope_fails() {
		env.putString("scope", "accounts offline_access");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void duplicateValues_doNotCrash() {
		env.putString("scope", "eudi.pid.1 eudi.pid.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void emptyScopeMap_fails() {
		env.putObject("credential_configuration_id_scope_map", new JsonObject());
		env.putString("scope", "eudi.pid.1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
