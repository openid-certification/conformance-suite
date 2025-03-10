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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateSdJwtCredential_UnitTest {

	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateSdJwtCredential cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CreateSdJwtCredential();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject(CreateAuthorizationEndpointResponseParams.ENV_KEY, new JsonObject());
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
