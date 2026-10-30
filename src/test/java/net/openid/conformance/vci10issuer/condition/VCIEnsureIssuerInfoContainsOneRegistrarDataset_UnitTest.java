package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIEnsureIssuerInfoContainsOneRegistrarDataset_UnitTest {

	private VCIEnsureIssuerInfoContainsOneRegistrarDataset cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureIssuerInfoContainsOneRegistrarDataset();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void extractsTheSuitesRegistrarDataset() {
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(IssuerInfoTestFixtures.suiteIssuerInfo());
		cond.execute(env);
		assertThat(env.getObject(VCIEnsureIssuerInfoContainsOneRegistrarDataset.ENV_KEY).has("providesAttestations")).isTrue();
	}

	@Test
	public void acceptsADatasetWithoutARegistrationCertificate() {
		cond.execute(env("""
			[ { "format": "registrar_dataset", "data": { "registryURI": "https://registry.example.com" } } ]
			"""));
	}

	@Test
	public void rejectsARegistrationCertificateWithoutADataset() {
		// as an issuer at the EUDI Wallet Launchpad sent it
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "registration_cert", "data": "eyJhbGciOiJFUzI1NiJ9.e30.c2ln" } ]
			""")));
	}

	@Test
	public void rejectsTwoDatasets() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[
			  { "format": "registrar_dataset", "data": { "registryURI": "https://a.example.com" } },
			  { "format": "registrar_dataset", "data": { "registryURI": "https://b.example.com" } }
			]
			""")));
	}

	@Test
	public void rejectsTwoRegistrationCertificates() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[
			  { "format": "registrar_dataset", "data": { "registryURI": "https://a.example.com" } },
			  { "format": "registration_cert", "data": "a.b.c" },
			  { "format": "registration_cert", "data": "d.e.f" }
			]
			""")));
	}

	@Test
	public void rejectsAnEmptyDataset() {
		assertThrows(ConditionError.class, () -> cond.execute(env("""
			[ { "format": "registrar_dataset", "data": {} } ]
			""")));
	}

	private static Environment env(String issuerInfo) {
		return IssuerInfoTestFixtures.envWithIssuerInfo(JsonParser.parseString(issuerInfo).getAsJsonArray());
	}
}
