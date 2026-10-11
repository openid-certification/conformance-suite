package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.SignedJWT;
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

@ExtendWith(MockitoExtension.class)
public class CreateSdJwtKbCredentialWithWrongVct_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateSdJwtKbCredentialWithWrongVct cond;

	@BeforeEach
	public void setUp() throws Exception {
		// PreGeneratedJwks requires owner_sub / owner_iss.
		env.putString("owner_sub", "unit-test-sub");
		env.putString("owner_iss", "unit-test-iss");
		cond = new CreateSdJwtKbCredentialWithWrongVct();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_vctIsNotTheRequestedType() throws Exception {
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
		String vct = SignedJWT.parse(sdJwt.getCredentialJwt()).getJWTClaimsSet().getStringClaim("vct");
		assertEquals(CreateSdJwtKbCredentialWithWrongVct.WRONG_VCT, vct);

		// Only the type is wrong: the requested claims are still disclosed.
		Set<String> disclosedClaimNames = sdJwt.getDisclosures().stream()
			.map(Disclosure::getClaimName)
			.collect(Collectors.toSet());
		assertEquals(Set.of("given_name", "family_name"), disclosedClaimNames);
	}

}
