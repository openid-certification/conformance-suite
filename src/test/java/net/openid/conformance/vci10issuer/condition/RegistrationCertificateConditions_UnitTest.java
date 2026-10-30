package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.lote.TrustPurpose;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.SuiteWrprc;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The registration certificate checks of issuer_info, each run on the suite's own certificate, which
 * passes all of them, and on a certificate with the one defect the check is about.
 */
public class RegistrationCertificateConditions_UnitTest {

	@Test
	public void theSuitesCertificatePassesEveryCheck() {
		Environment env = extracted(IssuerInfoTestFixtures.suiteIssuerInfo());
		TrustPurpose.REGISTRATION_CERTIFICATE.register(env, TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getCertPem());
		for (AbstractCondition cond : new AbstractCondition[] {
			new VCIValidateIssuerInfoRegistrationCertificateHeader(),
			new VCIValidateIssuerInfoRegistrationCertificateSignature(),
			new VCIValidateIssuerInfoRegistrationCertificateTimestamps(),
			new VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement(),
			new VCIEnsureIssuerInfoRegistrationCertificateSubMatchesAccessCertificate(),
			new VCICheckIssuerInfoRegistrationCertificateRegistryFields(),
			new VCICheckIssuerInfoRegistrationCertificateProvidesAttestations(),
		}) {
			run(cond, env);
		}
	}

	@Test
	public void signatureCheckRejectsACertificateFromAnotherCa() {
		Environment env = extracted(IssuerInfoTestFixtures.suiteIssuerInfo());
		TrustPurpose.REGISTRATION_CERTIFICATE.register(env, TestKeysAndCerts.SuiteCa.ACCESS.getCertPem());
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateSignature(), env);
	}

	@Test
	public void headerCheckRejectsAnotherTyp() {
		Environment env = extracted(IssuerInfoTestFixtures.suiteIssuerInfo());
		env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "header").getAsJsonObject().addProperty("typ", "JWT");
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateHeader(), env);
	}

	@Test
	public void headerCheckRejectsAMissingX5c() {
		Environment env = extracted(IssuerInfoTestFixtures.suiteIssuerInfo());
		env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "header").getAsJsonObject().remove("x5c");
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateHeader(), env);
	}

	@Test
	public void timestampsCheckAcceptsAMissingExp() {
		JsonObject payload = suitePayload();
		payload.remove("exp");
		run(new VCIValidateIssuerInfoRegistrationCertificateTimestamps(), withPayload(payload));
	}

	@Test
	public void timestampsCheckRejectsAnExpMoreThanTwelveMonthsAfterIat() {
		JsonObject payload = suitePayload();
		long iat = OIDFJSON.getLong(payload.get("iat"));
		payload.addProperty("exp", Instant.ofEpochSecond(iat).plus(400, ChronoUnit.DAYS).getEpochSecond());
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateTimestamps(), withPayload(payload));
	}

	@Test
	public void timestampsCheckRejectsAnExpiredCertificate() {
		JsonObject payload = suitePayload();
		payload.addProperty("iat", Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());
		payload.addProperty("exp", Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateTimestamps(), withPayload(payload));
	}

	@Test
	public void timestampsCheckRejectsAMissingIat() {
		JsonObject payload = suitePayload();
		payload.remove("iat");
		assertFails(new VCIValidateIssuerInfoRegistrationCertificateTimestamps(), withPayload(payload));
	}

	@Test
	public void entitlementCheckRejectsAServiceProviderOnly() {
		JsonObject payload = suitePayload();
		payload.add("entitlements", JsonParser.parseString("[\"https://uri.etsi.org/19475/Entitlement/Service_Provider\"]"));
		assertFails(new VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement(), withPayload(payload));
	}

	@Test
	public void entitlementCheckRejectsUnknownEntitlements() {
		JsonObject payload = suitePayload();
		payload.add("entitlements", JsonParser.parseString("[\"PID_Provider\"]"));
		assertFails(new VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement(), withPayload(payload));
	}

	@Test
	public void entitlementCheckAcceptsSeveralProviderRoles() {
		// as the EUDI Wallet Launchpad PID issuer registers
		JsonObject payload = suitePayload();
		payload.add("entitlements", JsonParser.parseString("""
			[
			  "https://uri.etsi.org/19475/Entitlement/PID_Provider",
			  "https://uri.etsi.org/19475/Entitlement/QEAA_Provider",
			  "https://uri.etsi.org/19475/Entitlement/Service_Provider"
			]
			"""));
		run(new VCIEnsureIssuerInfoRegistrationCertificateHasAttestationProviderEntitlement(), withPayload(payload));
	}

	@Test
	public void subCheckRejectsAnotherOrganization() {
		JsonObject payload = suitePayload();
		payload.addProperty("sub", "LEIXG-529900T8BM49AURSDO55");
		assertFails(new VCIEnsureIssuerInfoRegistrationCertificateSubMatchesAccessCertificate(), withPayload(payload));
	}

	@Test
	public void registryFieldsCheckRejectsAMissingField() {
		JsonObject payload = suitePayload();
		payload.remove("supervisory_authority");
		assertFails(new VCICheckIssuerInfoRegistrationCertificateRegistryFields(), withPayload(payload));
	}

	@Test
	public void registryFieldsCheckAcceptsANaturalPerson() {
		JsonObject payload = suitePayload();
		payload.remove("sub_ln");
		payload.addProperty("sub_gn", "Erika");
		payload.addProperty("sub_fn", "Mustermann");
		run(new VCICheckIssuerInfoRegistrationCertificateRegistryFields(), withPayload(payload));
	}

	@Test
	public void providesAttestationsCheckRejectsAMissingList() {
		JsonObject payload = suitePayload();
		payload.remove("provides_attestations");
		assertFails(new VCICheckIssuerInfoRegistrationCertificateProvidesAttestations(), withPayload(payload));
	}

	private static JsonObject suitePayload() {
		return SuiteWrprc.attestationProviderPayload(Instant.now(), IssuerInfoTestFixtures.providesAttestations());
	}

	private static Environment withPayload(JsonObject payload) {
		return extracted(IssuerInfoTestFixtures.issuerInfoWithRegistrationCertificatePayload(payload));
	}

	private static Environment extracted(JsonArray issuerInfo) {
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(issuerInfo);
		env.putObject("config", new JsonObject());
		run(new VCIExtractIssuerInfoRegistrationCertificate(), env);
		return env;
	}

	private static void run(AbstractCondition cond, Environment env) {
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		cond.execute(env);
	}

	private static void assertFails(AbstractCondition cond, Environment env) {
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
