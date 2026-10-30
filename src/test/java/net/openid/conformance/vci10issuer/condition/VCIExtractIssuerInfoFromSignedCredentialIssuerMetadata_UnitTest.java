package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata_UnitTest {

	private VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void extractsTopLevelIssuerInfo() {
		Environment env = IssuerInfoTestFixtures.envWithSignedMetadata(IssuerInfoTestFixtures.suiteIssuerInfo());
		cond.execute(env);
		assertThat(env.getElementFromObject(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, "issuer_info").isJsonArray()).isTrue();
	}

	@Test
	public void failsWhenAbsent() {
		Environment env = IssuerInfoTestFixtures.envWithSignedMetadata(null);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenOnlyInTheUnsignedMetadata() {
		Environment env = IssuerInfoTestFixtures.envWithSignedMetadata(null);
		env.getElementFromObject("vci", "credential_issuer_metadata").getAsJsonObject()
			.add("issuer_info", IssuerInfoTestFixtures.suiteIssuerInfo());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void failsWhenNotAnArray() {
		Environment env = IssuerInfoTestFixtures.envWithSignedMetadata(new JsonPrimitive("registrar"));
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
