package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIEnsureConfiguredCredentialConfigurationIdIsInCredentialOffer_UnitTest {

	private VCIEnsureConfiguredCredentialConfigurationIdIsInCredentialOffer cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureConfiguredCredentialConfigurationIdIsInCredentialOffer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void configuredIdAmongOfferedIdsPasses() {
		putOffer("[\"eu.europa.ec.eudi.pid.1\", \"org.iso.18013.5.1.mDL\"]");
		env.putString("vci_credential_configuration_id", "org.iso.18013.5.1.mDL");

		cond.execute(env);
	}

	@Test
	public void configuredIdNotOfferedFails() {
		putOffer("[\"eu.europa.ec.eudi.pid.1\"]");
		env.putString("vci_credential_configuration_id", "org.iso.18013.5.1.mDL");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void missingOfferedIdsFails() {
		env.putObject("vci", "credential_offer", JsonParser.parseString("{\"credential_issuer\": \"https://issuer.example.com\"}").getAsJsonObject());
		env.putString("vci_credential_configuration_id", "eu.europa.ec.eudi.pid.1");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	private void putOffer(String ids) {
		env.putObject("vci", "credential_offer", JsonParser.parseString(
			"{\"credential_issuer\": \"https://issuer.example.com\", \"credential_configuration_ids\": " + ids + "}").getAsJsonObject());
	}
}
