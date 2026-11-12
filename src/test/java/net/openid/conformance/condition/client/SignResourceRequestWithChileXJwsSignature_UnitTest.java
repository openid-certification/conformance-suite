package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Uses the test key and vectors 1 and 2 of Appendix A.2 of the CMF x-jws-signature profile
 * (CMF-XJWS-A.2). PS256 signatures are randomized, so the produced signature is verified rather than
 * compared with the published one.
 */
public class SignResourceRequestWithChileXJwsSignature_UnitTest {

	private static final String TEST_KEY_PEM = """
		-----BEGIN PRIVATE KEY-----
		MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC+5Ex5Jq2FnzW9
		Xjg25ExJ2YmVCaG3RfDqMFaq5Og7u/Ts9MNW37N4vcOAtRgx2H7DVDezSgmFQZls
		CFZIYhW2zfyTgRD58e4OX5lwzinN/CK/SnpLUXr+TGUW0rDvWlySt+57Z2yB3CkB
		ECV7Hry0wrsgM4bg+U0uONvYQl8+pgNUctdcIe21GKut6n9i6gjRAIEL+a9mHOSV
		RXYbfrrJ2v7Aj3s7NTfYB+tsgb8j8S9WBZWZm7O+DfSXA5y6Z64mI2q+r7q9TEwK
		ACk0vfUD/aRfFeM3vaV0XWvrkLAS5FJzo8+JLa+J2LM3gOHiMBVOBpnxac4rx3tn
		/E4VttazAgMBAAECggEAHc2XAvM+9DW/NimXrTpHjuAf0t0GGo8Y5fdWEKhcebcG
		rlgoh0+1vYwjEd00QdQDNovXhm5wvhVX1r4WeoaVzUyrunvUIrHa8POMCUimzjon
		UqZWIZw/D6Ttw3pxlW0ONvnwiePD1SKCZGAY+ff6jl44yPZCgK1rl2PZLxJtvGH5
		30MPUm4DUT+x1GGKiVsdOcwSN+uGX862b5ExPayox2Ok5QxfRrborNyZ12b+4j6m
		Dj07T1GZSPKifw5Mj3Mt8x5/s4pwFGlTmGmxU+3gX43+wRUe5099ogj1rQo9R+zR
		9ITN4l020R0/r4J+onLI/M7L0ZOFSJy7GJyMcw3EAQKBgQDlnPfHaEcKA1RD20Md
		oYtJgrKLGhUd1fo3lb4EP0tdNr6dY1ful3hl/k5lhYqEu69Sl6NCclOpJXxwGsGg
		9VoKjnEeM5Ft7Bqw/wlMe6zGABuDqlYAiODdg9rH5wB2PIr4XlsVrp7+6/x/kjEK
		y5AnBHr+I+xtgdedJz5fpDKdcwKBgQDU1C4JnEDTTNAjRHVerjbAYkmoLzJ43ESL
		0LOouzo1pGnvDrlXuJloRhPTdnT4H9zGmqyLSJWv3IYFvn1W5a9uMYFj6SNp3l5w
		xC1C3LcBcZwdhtEZaj+8F5N2CCsWwXruXZJSy0SaPa7yRpVXQ9MVunBfJV/7rn18
		cWOIxiCRwQKBgER3d9L4Hya3M2tAeFTflL8Iv+CGmIFbCs+EUTLHBNv4tQINW20D
		sZ0IJaxUE6fcpzzDhqULnjKzwNVUxSQPxRecvdI5okXav2LDyawKvMwVwB6Urixe
		pF6pMJWnJhYbm3zP9e0TMsnk6FfJ6eHELWnVBi+92oQU0YxE5W5zcbxNAoGBAK3L
		WpDUfQdPaYPzKlkXsHQnjvh9Gh0h2gPGeH9n7J/TVAI4JtfgUb2ZxCThTaE0+6de
		tVnD+yAx12unY1jrlPdKC9wXjKiuQraZLFC2Recy7AYcinHTlpo1MmCnSPB7LLSr
		yoo7nRjLGCqVwuwQ4bWTmqHHtnqiwaw2N85/5CbBAoGBAJVfpnGnWfEzyFt7W0AS
		tp3mPRgg9fRVKt4BV/vOk+K8oZzTsIaVhUmBsNQGUeDdURdr01fvSF60gOeKBuGe
		eK9aWZdLpgUEaLxLgmbq+c8rmXrIT+oTqrd7uM+GgcB5pll4+xdEJXzQ6w0Osu5I
		ljPkBdOb/d7KvHMTRazWW+IG
		-----END PRIVATE KEY-----
		""";

	private static final long VECTOR_TS = 1792775926L;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private SignResourceRequestWithChileXJwsSignature cond;

	private RSAKey testKey;

	@BeforeEach
	public void setUp() throws Exception {
		RSAKey parsed = JWK.parseFromPEMEncodedObjects(TEST_KEY_PEM).toRSAKey();
		testKey = new RSAKey.Builder(parsed).keyID("oidf-test-key-1").build();

		env = new Environment();
		env.putObject("client_jwks", jwks(testKey));

		cond = new SignResourceRequestWithChileXJwsSignature() {
			@Override
			protected long nowEpochSeconds() {
				return VECTOR_TS;
			}
		};
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private static JsonObject jwks(JWK key) {
		return JsonParser.parseString(new JWKSet(key).toString(false)).getAsJsonObject();
	}

	private void request(String url, String interactionId) {
		env.putString("protected_resource_url", url);
		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", interactionId);
		env.putObject("resource_endpoint_request_headers", headers);
	}

	private JWSObject sentSignature() throws Exception {
		String value = env.getString("resource_endpoint_request_headers", "x-jws-signature");
		assertThat(value).isEqualTo(env.getString(SignResourceRequestWithChileXJwsSignature.SENT_X_JWS_SIGNATURE));
		String[] parts = value.split("\\.", -1);
		assertThat(parts).hasSize(3);
		assertThat(parts[1]).isEmpty();
		JWSObject jws = JWSObject.parse(value, new Payload(""));
		assertThat(jws.verify(new RSASSAVerifier(testKey.toRSAPublicKey()))).isTrue();
		return jws;
	}

	@Test
	public void vector1GetWithQueryAndEmptyBody() throws Exception {
		request("https://bank.example/accounts/v1/accounts/CL-0001-CTA-OFTEST-12345678/transactions"
				+ "?page=1&fromBookingDate=2026-10-01&toBookingDate=2026-10-23",
			"5f0c2d1e-8a3b-4c6d-9e7f-102938475600");

		cond.execute(env);

		Map<String, Object> header = sentSignature().getHeader().toJSONObject();
		assertThat(header).containsEntry("alg", JWSAlgorithm.PS256.getName())
			.containsEntry("typ", "JOSE")
			.containsEntry("kid", "oidf-test-key-1")
			.containsEntry("ofc.method", "GET")
			.containsEntry("ofc.uri", "/accounts/v1/accounts/CL-0001-CTA-OFTEST-12345678/transactions")
			.containsEntry("ofc.interaction", "5f0c2d1e-8a3b-4c6d-9e7f-102938475600")
			.containsEntry("ofc.ts", VECTOR_TS)
			.containsEntry("ofc.digest", "S256:47DEQpj8HBSa-_TImW-5JCeuQeRkm5NMpJWZG3hSuFU")
			.containsEntry("ofc.query", "S256:i24xVuUU4MfSnT4OXVRDnGkPwYQq39fyJrKFobjWEiA");
	}

	@Test
	public void vector2PostWithJsonBodyAndNoQuery() throws Exception {
		request("https://bank.example/single-payments/v1/pn/single-payments", "9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
		JsonObject resource = new JsonObject();
		resource.addProperty("resourceMethod", "POST");
		env.putObject("resource", resource);
		env.putString("resource_request_entity", "{\"data\":{\"instruction\":{\"amount\":{\"currency\":\"CLP\",\"value\":1000}}}}");

		cond.execute(env);

		Map<String, Object> header = sentSignature().getHeader().toJSONObject();
		assertThat(header).containsEntry("ofc.method", "POST")
			.containsEntry("ofc.uri", "/single-payments/v1/pn/single-payments")
			.containsEntry("ofc.interaction", "9b8a7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
			.containsEntry("ofc.digest", "S256:0lAUAB5ZXtHHsKy19J8VqGySN2Nvo5uwWujHm0zcsac")
			.doesNotContainKey("ofc.query");
	}

	@Test
	public void pathIsSignedInTheEncodedFormSent() throws Exception {
		request("https://bank.example/accounts/v1/accounts/CL 0001", "iid");

		cond.execute(env);

		assertThat(sentSignature().getHeader().toJSONObject()).containsEntry("ofc.uri", "/accounts/v1/accounts/CL%200001");
	}

	@Test
	public void emptyPathIsSignedAsSlash() throws Exception {
		request("https://bank.example", "iid");

		cond.execute(env);

		assertThat(sentSignature().getHeader().toJSONObject()).containsEntry("ofc.uri", "/");
	}

	@Test
	public void canonicalQueryDecodesFormEncodingAndKeepsBlankValues() {
		assertThat(cond.canonicalQuery("b=2&a=x%20y&a=&c")).isEqualTo("a=&a=x y&b=2&c=");
		assertThat(cond.canonicalQuery("q=a+b")).isEqualTo("q=a b");
		assertThat(cond.canonicalQuery("")).isNull();
		assertThat(cond.canonicalQuery(null)).isNull();
	}

	@Test
	public void canonicalQuerySortsByCodePointNotUtf16Unit() {
		// U+1F600 (surrogate pair D83D DE00) sorts after U+FFFD by code point, but before it by UTF-16 unit
		assertThat(cond.canonicalQuery("k=%EF%BF%BD&k=%F0%9F%98%80")).isEqualTo("k=�&k=😀");
	}

	@Test
	public void missingInteractionIdFails() {
		env.putString("protected_resource_url", "https://bank.example/accounts/v1/accounts");
		env.putObject("resource_endpoint_request_headers", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void publicOnlyKeyFails() {
		env.putObject("client_jwks", jwks(testKey.toPublicJWK()));
		request("https://bank.example/accounts/v1/accounts", "iid");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonRsaKeyFails() throws Exception {
		env.putObject("client_jwks", jwks(new ECKeyGenerator(Curve.P_256).keyID("ec").generate()));
		request("https://bank.example/accounts/v1/accounts", "iid");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void storedSignatureMatchesHeader() throws Exception {
		request("https://bank.example/accounts/v1/accounts", "iid");

		cond.execute(env);

		assertThat(OIDFJSON.getString(env.getObject("resource_endpoint_request_headers").get("x-jws-signature")))
			.isEqualTo(env.getString(SignResourceRequestWithChileXJwsSignature.SENT_X_JWS_SIGNATURE));
	}
}
