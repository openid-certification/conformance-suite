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
public class VCIEnsureRequestedScopeIncludesOfferedCredentialConfiguration_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private VCIEnsureRequestedScopeIncludesOfferedCredentialConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureRequestedScopeIncludesOfferedCredentialConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
		env.putObject("credential_configuration_id_scope_map", JsonParser.parseString("""
			{
			  "eudi.pid.1": ["eu.europa.ec.eudi.pid.1"],
			  "eudi.pid.mdoc.1": ["eu.europa.ec.eudi.pid.mdoc.1"],
			  "openid.example.cert.1": ["net.openid.examples.certification.1.sdjwtvc", "net.openid.examples.certification.1.mdoc"]
			}
			""").getAsJsonObject());
	}

	private void putOffer(String... credentialConfigurationIds) {
		StringBuilder ids = new StringBuilder();
		for (String id : credentialConfigurationIds) {
			if (ids.length() > 0) {
				ids.append(", ");
			}
			ids.append('"').append(id).append('"');
		}
		env.putObject("vci", "credential_offer", JsonParser.parseString(
			"{\"credential_configuration_ids\": [" + ids + "]}").getAsJsonObject());
	}

	@Test
	public void scopeForOfferedConfiguration_passes() {
		putOffer("eu.europa.ec.eudi.pid.1");
		env.putString("scope", "eudi.pid.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void sharedScopeCoversOfferedConfiguration_passes() {
		putOffer("net.openid.examples.certification.1.mdoc");
		env.putString("scope", "openid.example.cert.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void oneOfSeveralOfferedConfigurations_passes() {
		putOffer("eu.europa.ec.eudi.pid.mdoc.1", "eu.europa.ec.eudi.pid.1");
		env.putString("scope", "eudi.pid.1");
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void scopeForOtherConfiguration_fails() {
		putOffer("eu.europa.ec.eudi.pid.mdoc.1");
		env.putString("scope", "eudi.pid.1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void noScopeMatches_fails() {
		putOffer("eu.europa.ec.eudi.pid.1");
		env.putString("scope", "openid");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void offerWithoutCredentialConfigurationIds_fails() {
		env.putObject("vci", "credential_offer", JsonParser.parseString(
			"{\"credential_issuer\": \"https://issuer.example.com\"}").getAsJsonObject());
		env.putString("scope", "eudi.pid.1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void credentialConfigurationIdsNotAnArray_fails() {
		env.putObject("vci", "credential_offer", JsonParser.parseString(
			"{\"credential_configuration_ids\": \"eu.europa.ec.eudi.pid.1\"}").getAsJsonObject());
		env.putString("scope", "eudi.pid.1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void offeredConfigurationWithoutScope_fails() {
		putOffer("no.scope.configuration");
		env.putString("scope", "eudi.pid.1");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
