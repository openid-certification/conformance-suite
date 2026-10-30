package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VCIExtractIssuerInfoRegistrationCertificate_UnitTest {

	private VCIExtractIssuerInfoRegistrationCertificate cond;

	@BeforeEach
	public void setUp() {
		cond = new VCIExtractIssuerInfoRegistrationCertificate();
		cond.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO);
	}

	@Test
	public void extractsACompactJws() {
		// as the suite and the EUDI Wallet Launchpad issuers send it
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(IssuerInfoTestFixtures.suiteIssuerInfo());
		cond.execute(env);
		assertThat(env.getString(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "encoding")).isEqualTo("compact JWS");
		assertThat(env.getString(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "header.typ")).isEqualTo("rc-wrp+jwt");
		assertThat(env.getElementFromObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "payload.provides_attestations")).isNotNull();
	}

	@Test
	public void extractsABase64urlEncodedCertificate() {
		// as TS 119 472-2 encodes it in verifier_info
		JsonArray issuerInfo = IssuerInfoTestFixtures.suiteIssuerInfo();
		JsonObject certificate = registrationCert(issuerInfo);
		String jws = OIDFJSON.getString(certificate.get("data"));
		certificate.addProperty("data", Base64.getUrlEncoder().withoutPadding().encodeToString(jws.getBytes(StandardCharsets.US_ASCII)));

		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(issuerInfo);
		cond.execute(env);
		assertThat(env.getString(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "encoding")).isEqualTo("base64url encoding of the compact JWS");
		assertThat(env.getString(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY, "value")).isEqualTo(jws);
	}

	@Test
	public void storesNothingWithoutARegistrationCertificate() {
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(JsonParser.parseString("""
			[ { "format": "registrar_dataset", "data": { "registryURI": "https://registry.example.com" } } ]
			""").getAsJsonArray());
		cond.execute(env);
		assertThat(env.containsObject(VCIExtractIssuerInfoRegistrationCertificate.ENV_KEY)).isFalse();
	}

	@Test
	public void rejectsDataThatIsNoJwt() {
		Environment env = IssuerInfoTestFixtures.envWithIssuerInfo(JsonParser.parseString("""
			[ { "format": "registration_cert", "data": "not a certificate" } ]
			""").getAsJsonArray());
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private static JsonObject registrationCert(JsonArray issuerInfo) {
		for (JsonElement element : issuerInfo) {
			if ("registration_cert".equals(OIDFJSON.getString(element.getAsJsonObject().get("format")))) {
				return element.getAsJsonObject();
			}
		}
		throw new AssertionError("no registration_cert");
	}
}
