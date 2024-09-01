package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckIfClientIdInX509CertSanDns_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckIfClientIdInX509CertSanDns cond;

	@BeforeEach
	public void setUp() throws Exception {

		String jwksWithX5cForDemo = """
			{
			            "keys": [
			                {
			                    "kty": "EC",
			                    "d": "y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0",
			                    "use": "sig",
			                    "x5c": [
			                        "MIIB1DCCAXugAwIBAgIUaAIyt8MjaeEkFr7JbNm3SQRGRLEwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDAyMTMxOTEzMzJaFw0zNDAyMTAxOTEzMzJaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GQMIGNMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MDoGA1UdEQQzMDGCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0MAoGCCqGSM49BAMCA0cAMEQCIFjg5xf9B+2ruZlWQO74tAm7LTXBh1CuiXHY3BNcZSQHAiB7CqCQCrBdB1CufiuvzvNrh3iEZJvLTmvFM+jyRaACxA=="
			                    ],
			                    "crv": "P-256",
			                    "kid": "5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI",
			                    "x": "0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc",
			                    "y": "ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA",
			                    "alg": "ES256"
			                },
			                {
			                    "kty": "EC",
			                    "d": "7N8jd8HvUp3vHC7a-xitehRnYuyZLy3kqkxG7KmpfMY",
			                    "use": "enc",
			                    "crv": "P-256",
			                    "kid": "A541J5yUqazgE8WBFkIyeh2OtK-udqUR_OC0kB7l3oU",
			                    "x": "cwYyuS94hcOtcPlrMMtGtflCfbZUwz5Mf1Gfa2m0AM8",
			                    "y": "KB7sJkFQyB8jZHO9vmWS5LNECL4id3OJO9HX9ChNonA",
			                    "alg": "ECDH-ES"
			                }
			            ]
			        }
			""";
		env.putObjectFromJsonString("client_jwks",
			jwksWithX5cForDemo);

//		Security.addProvider(new BouncyCastleProvider());

		cond = new CheckIfClientIdInX509CertSanDns();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_clientIdWrong() {
		assertThrows(ConditionError.class, () -> {
			env.putString("config", "client.client_id", "www.certification.openid.net");

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_success() {

		env.putString("config", "client.client_id", "demo.certification.openid.net");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {
			env.putString("config", "client.client_name", "foo");

			cond.execute(env);
		});
	}

}
