package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateSdJwtKbCredential_UnitTest {

	private static final String SIGNING_JWK = """
		{
		    "kty": "EC",
		    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
		    "use": "sig",
		    "crv": "P-256",
		    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
		    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
		    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
		    "alg": "ES256"
		}""";

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateSdJwtKbCredential cond;

	@BeforeEach
	public void setUp() throws Exception {
		// PreGeneratedJwks requires owner_sub / owner_iss.
		env.putString("owner_sub", "unit-test-sub");
		env.putString("owner_iss", "unit-test-iss");
		cond = new CreateSdJwtKbCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		String key = """
			{
			    "kty": "EC",
			    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
			    "use": "sig",
			    "x5c": [
			        "MIIB+DCCAZ6gAwIBAgIUSy80Ezru1eOPrGW88uSFC8H8lVYwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDExMTkwOTMwMzNaFw0zNDExMTcwOTMwMzNaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GzMIGwMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MF0GA1UdEQRWMFSCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0gglsb2NhbGhvc3SCFmxvY2FsaG9zdC5lbW9iaXguY28udWswCgYIKoZIzj0EAwIDSAAwRQIhAPQtPciRiOPkw4ZMfmP1ov3LXlhG8wizrJ9Oyu+QPWAEAiBJn30EEuhhFyS7nqOhZok+M0XNbbxhNB0i7KxKSEsITA=="
			    ],
			    "crv": "P-256",
			    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
			    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
			    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
			    "alg": "ES256"
			}""";
		env.putObjectFromJsonString("config", "credential.signing_jwk", key);

		cond.execute(env);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY)));
	}

	@Test
	public void testEvaluate_noClaimsInDcqlQueryOmitsDisclosures() {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", """
			{
			    "kty": "EC",
			    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
			    "use": "sig",
			    "x5c": [
			        "MIIB+DCCAZ6gAwIBAgIUSy80Ezru1eOPrGW88uSFC8H8lVYwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDExMTkwOTMwMzNaFw0zNDExMTcwOTMwMzNaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GzMIGwMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MF0GA1UdEQRWMFSCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0gglsb2NhbGhvc3SCFmxvY2FsaG9zdC5lbW9iaXguY28udWswCgYIKoZIzj0EAwIDSAAwRQIhAPQtPciRiOPkw4ZMfmP1ov3LXlhG8wizrJ9Oyu+QPWAEAiBJn30EEuhhFyS7nqOhZok+M0XNbbxhNB0i7KxKSEsITA=="
			    ],
			    "crv": "P-256",
			    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
			    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
			    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
			    "alg": "ES256"
			}""");
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "urn:eudi:pid:1"
			        ]
			      }
			    }
			  ]
			}
			""");

		cond.execute(env);

		SDJWT sdJwt = SDJWT.parse(env.getString("credential"));
		assertTrue(sdJwt.getDisclosures().isEmpty());
	}

	@Test
	public void testEvaluate_subsetOfClaimsKeepsOnlyRequestedDisclosures() {
		// DCQL requests only given_name and family_name; the issuer normally also discloses
		// birthdate, nationalities (with a nested array element disclosure), and place_of_birth.
		// The filter should keep only the two requested object-property disclosures.
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", """
			{
			    "kty": "EC",
			    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
			    "use": "sig",
			    "x5c": [
			        "MIIB+DCCAZ6gAwIBAgIUSy80Ezru1eOPrGW88uSFC8H8lVYwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDExMTkwOTMwMzNaFw0zNDExMTcwOTMwMzNaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GzMIGwMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MF0GA1UdEQRWMFSCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0gglsb2NhbGhvc3SCFmxvY2FsaG9zdC5lbW9iaXguY28udWswCgYIKoZIzj0EAwIDSAAwRQIhAPQtPciRiOPkw4ZMfmP1ov3LXlhG8wizrJ9Oyu+QPWAEAiBJn30EEuhhFyS7nqOhZok+M0XNbbxhNB0i7KxKSEsITA=="
			    ],
			    "crv": "P-256",
			    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
			    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
			    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
			    "alg": "ES256"
			}""");
		env.putObjectFromJsonString(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY, """
			{
			  "credentials": [
			    {
			      "id": "my_credential",
			      "format": "dc+sd-jwt",
			      "meta": {
			        "vct_values": [
			          "urn:eudi:pid:1"
			        ]
			      },
			      "claims": [
			        {"path": ["given_name"]},
			        {"path": ["family_name"]}
			      ]
			    }
			  ]
			}
			""");

		cond.execute(env);

		SDJWT sdJwt = SDJWT.parse(env.getString("credential"));
		Set<String> disclosedClaimNames = sdJwt.getDisclosures().stream()
			.map(Disclosure::getClaimName)
			.collect(Collectors.toSet());
		// Only the two requested object-property disclosures should remain. The orphan array
		// element disclosure (the "FR" inside nationalities) and any other top-level disclosure
		// should have been dropped because no kept disclosure references their digest.
		assertEquals(Set.of("given_name", "family_name"), disclosedClaimNames);
	}

	@Test
	public void testEvaluate_noStatusClaimWithoutAStatusListReference() throws Exception {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", SIGNING_JWK);

		cond.execute(env);

		assertNull(issuerJwtClaims(env.getString("credential")).getClaim("status"));
	}

	@Test
	public void testEvaluate_referencesTheStatusListWhenOneWasAllocated() throws Exception {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", SIGNING_JWK);
		env.putObjectFromJsonString(AbstractCreateStatusListReference.ENV_KEY, """
			{"uri": "https://example.com/test/a/alias/statuslists/1", "idx": 41}""");

		cond.execute(env);

		// draft-ietf-oauth-status-list section 6.2
		Map<String, Object> status = issuerJwtClaims(env.getString("credential"))
			.getJSONObjectClaim("status");
		@SuppressWarnings("unchecked")
		Map<String, Object> statusList = (Map<String, Object>) status.get("status_list");
		assertEquals(41, ((Number) statusList.get("idx")).intValue());
		assertEquals("https://example.com/test/a/alias/statuslists/1", statusList.get("uri"));
	}

	private JWTClaimsSet issuerJwtClaims(String credential) throws Exception {
		return SignedJWT.parse(SDJWT.parse(credential).getCredentialJwt()).getJWTClaimsSet();
	}

	@Test
	public void testEvaluate_missingSigningJwkErrorReferencesConfigField() {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObject("config", new JsonObject());

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK' field is missing from the 'Credential Issuer' section in the test configuration"),
			"expected message to reference the missing 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void testEvaluate_unparseableSigningJwkErrorReferencesConfigField() {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", """
			{"kty": "not-a-real-kty"}""");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("Failed to parse the 'Signing JWK' field in the 'Credential Issuer' section of the test configuration"),
			"expected message to reference the unparseable 'Signing JWK' config field but was: " + err.getMessage());
	}

	@Test
	public void testEvaluate_signingJwkWithoutAlgErrorReferencesConfigField() {
		// OKP key with no 'alg' claim: no default algorithm is defined (unlike EC, which
		// falls back to ES256), so the condition must fail pointing at the config field.
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("config", "credential.signing_jwk", """
			{"kty": "OKP", "crv": "Ed25519", "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"}""");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("'Signing JWK'") && err.getMessage().contains("'alg'"),
			"expected message to reference the 'Signing JWK' config field and the 'alg' claim but was: " + err.getMessage());
	}

}
