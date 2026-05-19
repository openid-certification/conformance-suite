package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class CreateSdJwtKbCredential_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateSdJwtKbCredential cond;

	@BeforeEach
	public void setUp() throws Exception {
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

}
