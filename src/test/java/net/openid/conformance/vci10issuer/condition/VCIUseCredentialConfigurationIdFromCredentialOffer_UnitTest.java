package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIUseCredentialConfigurationIdFromCredentialOffer_UnitTest {

	private VCIUseCredentialConfigurationIdFromCredentialOffer cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIUseCredentialConfigurationIdFromCredentialOffer();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
	}

	@Test
	public void singleOfferedIdIsUsed() {
		putOffer("[\"eu.europa.ec.eudi.pid.1\"]");

		cond.execute(env);

		assertThat(env.getString("vci_credential_configuration_id")).isEqualTo("eu.europa.ec.eudi.pid.1");
	}

	@Test
	public void severalOfferedIdsFail() {
		putOffer("[\"eu.europa.ec.eudi.pid.1\", \"org.iso.18013.5.1.mDL\"]");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void emptyOfferedIdsFail() {
		putOffer("[]");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}

	private void putOffer(String ids) {
		env.putObject("vci", "credential_offer", JsonParser.parseString(
			"{\"credential_issuer\": \"https://issuer.example.com\", \"credential_configuration_ids\": " + ids + "}").getAsJsonObject());
	}
}
