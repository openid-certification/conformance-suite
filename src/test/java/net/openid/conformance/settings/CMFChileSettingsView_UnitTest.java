package net.openid.conformance.settings;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CMFChileSettingsView_UnitTest {

	private static CMFChileSettings settings;

	@BeforeAll
	public static void createSettings() throws Exception {
		settings = TestPki.validSettings().withAudit(Instant.parse("2026-09-30T12:00:00Z"), "Admin User");
	}

	@Test
	public void containsNoSecretOrPrivateKeyMaterial() throws Exception {
		String json = CMFChileSettingsView.toJson(settings).toString();

		assertThat(json).doesNotContain("the-client-secret");
		assertThat(json).doesNotContain("PRIVATE KEY");
		for (String privateMember : List.of("\"d\"", "\"p\"", "\"q\"", "\"dp\"", "\"dq\"", "\"qi\"", "\"k\"", "\"oth\"")) {
			assertThat(json).doesNotContain(privateMember);
		}
		RSAKey key = (RSAKey) JWKSet.parse(settings.clientJwks()).getKeyByKeyId("sig-1");
		assertThat(json).doesNotContain(key.getPrivateExponent().toString());
		assertThat(json).doesNotContain(key.getModulus().toString());
	}

	@Test
	public void reportsWhichSecretsAreSet() {
		JsonObject view = CMFChileSettingsView.toJson(settings);

		assertThat(OIDFJSON.getBoolean(view.get("clientSecretSet"))).isTrue();
		assertThat(OIDFJSON.getBoolean(view.getAsJsonObject("clientJwks").get("set"))).isTrue();
		JsonObject entry = view.getAsJsonArray("positiveCertificates").get(0).getAsJsonObject();
		assertThat(OIDFJSON.getBoolean(entry.get("privateKeySet"))).isTrue();
	}

	@Test
	public void summarisesTheJwksKeys() {
		JsonObject key = CMFChileSettingsView.toJson(settings).getAsJsonObject("clientJwks")
			.getAsJsonArray("keys").get(0).getAsJsonObject();

		assertThat(OIDFJSON.getString(key.get("kid"))).isEqualTo("sig-1");
		assertThat(OIDFJSON.getString(key.get("kty"))).isEqualTo("RSA");
		assertThat(OIDFJSON.getString(key.get("alg"))).isEqualTo("PS256");
		assertThat(OIDFJSON.getString(key.get("use"))).isEqualTo("sig");
	}

	@Test
	public void returnsPlainFieldsAndCertificateMetadata() {
		JsonObject view = CMFChileSettingsView.toJson(settings);

		assertThat(OIDFJSON.getString(view.get("directoryTokenEndpoint"))).isEqualTo("https://directory.example.cl/token");
		assertThat(OIDFJSON.getString(view.get("softwareStatementEndpoint")))
			.isEqualTo("https://directory.example.cl/software-statement");
		assertThat(OIDFJSON.getString(view.get("clientId"))).isEqualTo("oidf-conformance");
		assertThat(OIDFJSON.getString(view.get("updatedAt"))).isEqualTo("2026-09-30T12:00:00Z");
		assertThat(OIDFJSON.getString(view.get("updatedBy"))).isEqualTo("Admin User");

		JsonObject negative = view.getAsJsonArray("negativeCertificates").get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(negative.get("id"))).isEqualTo("neg-1");
		assertThat(OIDFJSON.getString(negative.get("label"))).isEqualTo("expired");
		assertThat(OIDFJSON.getString(negative.get("certificateChainPem"))).contains("BEGIN CERTIFICATE");
		assertThat(OIDFJSON.getString(negative.get("subject"))).isEqualTo("CN=negative");
		assertThat(OIDFJSON.getString(negative.get("issuer"))).isEqualTo("CN=negative");
		assertThat(Instant.parse(OIDFJSON.getString(negative.get("notAfter")))).isBefore(Instant.now());
	}

	@Test
	public void anEmptySectionHasNothingSet() {
		JsonObject view = CMFChileSettingsView.toJson(CMFChileSettings.empty());

		assertThat(OIDFJSON.getBoolean(view.get("clientSecretSet"))).isFalse();
		assertThat(OIDFJSON.getBoolean(view.getAsJsonObject("clientJwks").get("set"))).isFalse();
		assertThat(view.getAsJsonArray("positiveCertificates")).isEmpty();
		assertThat(view.has("directoryTokenEndpoint")).isFalse();
	}

	@Test
	public void aCertificateWithInvalidBase64IsFlaggedRatherThanFailingTheView() {
		CMFChileSettings corrupt = new CMFChileSettings(null, null, null, null, null, List.of(new CertificateEntry("x", "broken",
			"-----BEGIN CERTIFICATE-----\nnot*base64!!\n-----END CERTIFICATE-----\n", "KEY")), List.of(), null, null);

		JsonObject entry = CMFChileSettingsView.toJson(corrupt).getAsJsonArray("positiveCertificates").get(0).getAsJsonObject();

		assertThat(OIDFJSON.getBoolean(entry.get("unreadable"))).isTrue();
	}

	@Test
	public void anUnreadableCertificateIsFlaggedWithoutMetadata() {
		CMFChileSettings corrupt = new CMFChileSettings(null, null, null, null, null,
			List.of(new CertificateEntry("x", "broken", "garbage", "KEY")), List.of(), null, null);

		JsonObject entry = CMFChileSettingsView.toJson(corrupt).getAsJsonArray("positiveCertificates").get(0).getAsJsonObject();

		assertThat(OIDFJSON.getBoolean(entry.get("unreadable"))).isTrue();
		assertThat(entry.has("subject")).isFalse();
		assertThat(entry.toString()).doesNotContain("KEY\"");
	}
}
