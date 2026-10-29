package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VCIEnsureCredentialOfferCredentialIssuerMatchesConfiguration_UnitTest {

	private VCIEnsureCredentialOfferCredentialIssuerMatchesConfiguration cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureCredentialOfferCredentialIssuerMatchesConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		JsonObject config = JsonParser.parseString("""
			{ "vci": { "credential_issuer_url": "https://itb.frontend.dev.issuer.eudiw.dev" } }
			""").getAsJsonObject();
		env.putObject("config", config);
	}

	private void putOfferIssuer(String issuer) {
		JsonObject offer = new JsonObject();
		offer.addProperty("credential_issuer", issuer);
		env.putObject("vci", "credential_offer", offer);
	}

	@Test
	public void theConfiguredIssuerPasses() {
		putOfferIssuer("https://itb.frontend.dev.issuer.eudiw.dev");

		assertThatCode(() -> cond.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void anOfferFromAnotherIssuerFailsNamingBoth() {
		putOfferIssuer("https://issuer.eudiw.setcce.com/pid-issuer");

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Credential Issuer URL' in the test configuration");
	}

	@Test
	public void aTrailingSlashDifferenceFails() {
		putOfferIssuer("https://itb.frontend.dev.issuer.eudiw.dev/");

		assertThatThrownBy(() -> cond.execute(env)).isInstanceOf(ConditionError.class);
	}
}
