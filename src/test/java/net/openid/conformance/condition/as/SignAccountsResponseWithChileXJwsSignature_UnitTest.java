package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses the test key and vectors 1 and 3 of Appendix A.2 of the CMF x-jws-signature profile
 * (CMF-XJWS-A.2): vector 3 is the signed response to the request of vector 1.
 */
public class SignAccountsResponseWithChileXJwsSignature_UnitTest {

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

	private static final String VECTOR_1_INTERACTION_ID = "5f0c2d1e-8a3b-4c6d-9e7f-102938475600";

	private static final String VECTOR_1_SIGNATURE = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJvaWRmLXRlc3Qta2V5LTEiLCJvZmMubWV0aG9kIjoiR0VUIiwib2ZjLnVyaSI6Ii9hY2NvdW50cy92MS9hY2NvdW50cy9DTC0wMDAxLUNUQS1PRlRFU1QtMTIzNDU2NzgvdHJhbnNhY3Rpb25zIiwib2ZjLmludGVyYWN0aW9uIjoiNWYwYzJkMWUtOGEzYi00YzZkLTllN2YtMTAyOTM4NDc1NjAwIiwib2ZjLnRzIjoxNzkyNzc1OTI2LCJvZmMuZGlnZXN0IjoiUzI1Njo0N0RFUXBqOEhCU2EtX1RJbVctNUpDZXVRZVJrbTVOTXBKV1pHM2hTdUZVIiwib2ZjLnF1ZXJ5IjoiUzI1NjppMjR4VnVVVTRNZlNuVDRPWFZSRG5Ha1B3WVFxMzlmeUpyS0ZvYmpXRWlBIn0..CVoFNLn4zl2ywyvD9uVwwLTVZWKIwffk80g0UJGOBsnVyJ6gROh-Nz8audIMurNmJOatTU87MXokw8efeK_eTxE92cRV4dQLvV18DRFTJGtO7gyZWhlHnHb7peLD4QPaSoHujdmzuvr5jhkLA0CiU3_0U9RoZThxX-mujiWo-vcZQCCOqEuGSidZMmxA3dMIWxa0dUojdibYXndZ17__KFlKHdW5awx_W8sW__ASE_FWw8v_D07V2hg7qcyEPijNrhAy7rqSRhbohBCtzGaHWWUQTUmuquLkBEQoblSaZdJ2W344nS5ZAGgjO302T8DAvzTxW-MAnnJEuR59rXPz_g";

	private static final String VECTOR_3_BODY = "{\"data\":{\"accountId\":\"CL-0001-CTA-OFTEST-12345678\",\"currency\":\"CLP\"},\"links\":{\"self\":\"/accounts/v1/accounts/CL-0001-CTA-OFTEST-12345678\"},\"meta\":{}}";

	private static final String VECTOR_3_SIGNATURE = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpPU0UiLCJraWQiOiJvaWRmLXRlc3Qta2V5LTEiLCJvZmMuc3RhdHVzIjoyMDAsIm9mYy5pbnRlcmFjdGlvbiI6IjVmMGMyZDFlLThhM2ItNGM2ZC05ZTdmLTEwMjkzODQ3NTYwMCIsIm9mYy5yZXEiOiJTMjU2OklKZmh6eFlxNWI5T2YwTzRwazkxRFZSTEJjTks5THIxTm54c2dqZFlKVVkiLCJvZmMudHMiOjE3OTI3NzU5MjZ9..YdbSRQvpWsvsSm0pSSZ4exfJQygxdVdPX6sRzgELm1TU0XkZKIDSPBbiC9dGRRtiCSdvXQh_cMkYUcJVB1Imxetc5LDZGEAm8-1u10HJBUunACXCahvRyb3P1KfIYusioJI7cWhbSq2wKq6KYVq4ceB9Z5QOF-kVly63VM11DEE8Jtjn-FxvLeoklN5zS1kxcN_CUSVOefRYD1Q-tv7k18WCvzUWFddLawZZhsK0GVr7vVomxnaFpP4CAdfnQQaufPu8YiHlIyeZsT2_uLXMONe8BmttL1Du9s6Jyb0QDkNqZ_sJ1aF-Atg0Dx5o0HJSF6xkpntDCoeCescizoyd5Q";

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	private SignAccountsResponseWithChileXJwsSignature cond;

	private RSAKey testKey;

	@BeforeEach
	public void setUp() throws Exception {
		RSAKey parsed = JWK.parseFromPEMEncodedObjects(TEST_KEY_PEM).toRSAKey();
		testKey = new RSAKey.Builder(parsed).keyID("oidf-test-key-1").build();

		env = new Environment();
		env.putObject(ExtractChileResponseSigningJwksFromConfiguration.RESPONSE_SIGNING_JWKS,
			JsonParser.parseString(new JWKSet(testKey).toString(false)).getAsJsonObject());
		env.putObject("accounts_endpoint_response_headers", new JsonObject());
		env.putString(SerializeAccountsEndpointResponseBody.ACCOUNTS_RESPONSE_BODY, VECTOR_3_BODY);

		cond = new SignAccountsResponseWithChileXJwsSignature() {
			@Override
			protected long nowEpochSeconds() {
				return VECTOR_TS;
			}
		};
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);
	}

	private void incomingRequest(String interactionId, String signature) {
		JsonObject headers = new JsonObject();
		if (interactionId != null) {
			headers.addProperty("x-fapi-interaction-id", interactionId);
		}
		if (signature != null) {
			headers.addProperty("x-jws-signature", signature);
		}
		JsonObject request = new JsonObject();
		request.add("headers", headers);
		env.putObject("incoming_request", request);
	}

	private JWSObject sentSignature() throws Exception {
		String value = OIDFJSON.getString(env.getObject("accounts_endpoint_response_headers").get("x-jws-signature"));
		String[] parts = value.split("\\.", -1);
		assertThat(parts).hasSize(3);
		assertThat(parts[1]).isEmpty();
		JWSObject jws = JWSObject.parse(value, new Payload(VECTOR_3_BODY.getBytes(StandardCharsets.UTF_8)));
		assertThat(jws.verify(new RSASSAVerifier(testKey.toRSAPublicKey()))).isTrue();
		return jws;
	}

	@Test
	public void publishedVector3VerifiesOverTheBodyWithTheTestKey() throws Exception {
		JWSObject jws = JWSObject.parse(VECTOR_3_SIGNATURE, new Payload(VECTOR_3_BODY.getBytes(StandardCharsets.UTF_8)));

		assertThat(jws.verify(new RSASSAVerifier(testKey.toRSAPublicKey()))).isTrue();
	}

	@Test
	public void vector3ResponseToVector1() throws Exception {
		incomingRequest(VECTOR_1_INTERACTION_ID, VECTOR_1_SIGNATURE);

		cond.execute(env);

		Map<String, Object> header = sentSignature().getHeader().toJSONObject();
		assertThat(header).containsEntry("alg", "PS256")
			.containsEntry("typ", "JOSE")
			.containsEntry("kid", "oidf-test-key-1")
			.containsEntry("ofc.status", 200L)
			.containsEntry("ofc.ts", VECTOR_TS)
			.containsEntry("ofc.interaction", VECTOR_1_INTERACTION_ID)
			.containsEntry("ofc.req", "S256:IJfhzxYq5b9Of0O4pk91DVRLBcNK9Lr1NnxsgjdYJUY");
		assertThat(OIDFJSON.getString(env.getObject("accounts_endpoint_response_headers").get("x-fapi-interaction-id")))
			.isEqualTo(VECTOR_1_INTERACTION_ID);
	}

	@Test
	public void noInteractionOrReqBindingWhenTheRequestLacksThem() throws Exception {
		incomingRequest(null, null);

		cond.execute(env);

		assertThat(sentSignature().getHeader().toJSONObject())
			.containsEntry("ofc.status", 200L)
			.doesNotContainKey("ofc.interaction")
			.doesNotContainKey("ofc.req");
		assertThat(env.getObject("accounts_endpoint_response_headers").has("x-fapi-interaction-id")).isFalse();
	}

}
