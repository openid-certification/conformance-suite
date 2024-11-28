package net.openid.conformance.condition.as;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddVpTokenToAuthorizationEndpointResponseParams_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddVpTokenToAuthorizationEndpointResponseParams cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new AddVpTokenToAuthorizationEndpointResponseParams();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		String pd = """
			{
			    "id": "4db74328-9e94-49bb-97b7-bbfcb2d11a06",
			    "name": "PID - Name and age verification (vc+sd-jwt)",
			    "purpose": "We need to verify your name and age",
			    "input_descriptors": [
			      {
			        "id": "fdf340c6-05f9-4125-b0fc-86a08d8ad051",
			        "format": {
			          "vc+sd-jwt": {
			            "sd-jwt_alg_values": [
			              "ES256"
			            ],
			            "kb-jwt_alg_values": [
			              "ES256"
			            ]
			          }
			        },
			        "constraints": {
			          "limit_disclosure": "required",
			          "fields": [
			            {
			              "path": [
			                "$.given_name"
			              ]
			            },
			            {
			              "path": [
			                "$.family_name"
			              ]
			            },
			            {
			              "path": [
			                "$.age_equal_or_over.21"
			              ]
			            },
			            {
			              "path": [
			                "$.vct"
			              ],
			              "filter": {
			                "type": "string",
			                "enum": [
			                  "https://example.bmi.bund.de/credential/pid/1.0",
			                  "urn:eu.europa.ec.eudi:pid:1"
			                ]
			              }
			            },
			            {
			              "path": [
			                "$.iss"
			              ],
			              "filter": {
			                "type": "string",
			                "enum": [
			                  "https://demo.pid-issuer.bundesdruckerei.de/c",
			                  "https://demo.pid-issuer.bundesdruckerei.de/c1",
			                  "https://demo.pid-issuer.bundesdruckerei.de/b1"
			                ]
			              }
			            }
			          ]
			        }
			      }
			    ]
			  }""";
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
		env.putObjectFromJsonString("authorization_request_object", "claims.presentation_definition", pd);
		String key = "{\n" +
				"    \"kty\": \"EC\",\n" +
				"    \"d\": \"y2NSNIvlRAEBMFk2bjQcSKbjS1y_NBJQ6jRzIfuIxS0\",\n" +
				"    \"use\": \"sig\",\n" +
				"    \"x5c\": [\n" +
				"        \"MIIB+DCCAZ6gAwIBAgIUSy80Ezru1eOPrGW88uSFC8H8lVYwCgYIKoZIzj0EAwIwITELMAkGA1UEBhMCR0IxEjAQBgNVBAMMCU9JREYgVGVzdDAeFw0yNDExMTkwOTMwMzNaFw0zNDExMTcwOTMwMzNaMCExCzAJBgNVBAYTAkdCMRIwEAYDVQQDDAlPSURGIFRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATT/dLsd51LLBrGV6R23o6vymRxHXeFBoI8yq31y5kFV2VV0gi9x5ZzEFiq8DMiAHucLACFndxLtZorCha9zznQo4GzMIGwMB0GA1UdDgQWBBS5cbdgAeMBi5wxpbpwISGhShAWETAfBgNVHSMEGDAWgBS5cbdgAeMBi5wxpbpwISGhShAWETAPBgNVHRMBAf8EBTADAQH/MF0GA1UdEQRWMFSCEHd3dy5oZWVuYW4ubWUudWuCHWRlbW8uY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0gglsb2NhbGhvc3SCFmxvY2FsaG9zdC5lbW9iaXguY28udWswCgYIKoZIzj0EAwIDSAAwRQIhAPQtPciRiOPkw4ZMfmP1ov3LXlhG8wizrJ9Oyu+QPWAEAiBJn30EEuhhFyS7nqOhZok+M0XNbbxhNB0i7KxKSEsITA==\"\n" +
				"    ],\n" +
				"    \"crv\": \"P-256\",\n" +
				"    \"kid\": \"5H1WLeSx55tMW6JNlvqMfg3O_E0eQPqB8jDSoUn6oiI\",\n" +
				"    \"x\": \"0_3S7HedSywaxlekdt6Or8pkcR13hQaCPMqt9cuZBVc\",\n" +
				"    \"y\": \"ZVXSCL3HlnMQWKrwMyIAe5wsAIWd3Eu1misKFr3POdA\",\n" +
				"    \"alg\": \"ES256\"\n" +
				"}";
		env.putObjectFromJsonString("config", "credential.signing_jwk", key);

		cond.execute(env);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(env.getObject(CreateAuthorizationEndpointResponseParams.ENV_KEY)));
	}

}
