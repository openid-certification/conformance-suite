package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Security;

@RunWith(MockitoJUnitRunner.class)
public class ValidateRequestObjectSignature_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateRequestObjectSignature cond;
	@Before
	public void setUp() throws Exception {
		cond = new ValidateRequestObjectSignature();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void evaluate_validateSignature1() {
		JsonParser jsonParser = new JsonParser();
		JsonObject requestObject = jsonParser.parse("{\n" +
			"  \"value\": \"eyJhbGciOiJQUzI1NiIsInR5cCI6Im9hdXRoLWF1dGh6LXJlcStqd3QiLCJraWQiOiJraWQifQ.eyJzY29wZSI6Im9wZW5pZCBjb25zZW50OnVybjpjb25mb3JtYW5jZS5vaWRmOnNkS09zTnc3SFMgYWNjb3VudHMiLCJyZXNwb25zZV90eXBlIjoiY29kZSBpZF90b2tlbiIsInJlZGlyZWN0X3VyaSI6Imh0dHBzOi8vdHBwLmxvY2FsaG9zdC9jYiIsImNvZGVfY2hhbGxlbmdlIjoiUXhGMHJfd1lZTTBCc1Y1QVlNT3lyWTJFN2VjN01yYTMwZGpGYk1XMVFjOCIsImNvZGVfY2hhbGxlbmdlX21ldGhvZCI6IlMyNTYiLCJyZXNwb25zZV9tb2RlIjoiZm9ybV9wb3N0Iiwic3RhdGUiOiJkZTE3NjRiZGJiNjhjOTBkNjIyYzJiMzE3ZWU3ZGVlMTcxOWVjYTA1YmI2Y2U1MzA0YTJiZDg1ZWJhODIwNDlmIiwibm9uY2UiOiJlZjdjNGQxOGY4ZTk3NmY3YzViZGY2YTI5NTM2YzUyMzgyNTBhNTcwMjZmZWE2MWI4Njc0MzkwZTI0YWZhYjQ4IiwiY2xhaW1zIjp7ImlkX3Rva2VuIjp7ImF1dGhfdGltZSI6eyJlc3NlbnRpYWwiOnRydWV9LCJnaXZlbl9uYW1lIjp7ImVzc2VudGlhbCI6dHJ1ZX0sImFjciI6eyJ2YWx1ZSI6InVybjpicmFzaWw6b3BlbmJhbmtpbmc6bG9hMiIsImVzc2VudGlhbCI6dHJ1ZX19LCJ1c2VyX2luZm8iOnsiYXV0aF90aW1lIjp7ImVzc2VudGlhbCI6dHJ1ZX0sImdpdmVuX25hbWUiOnsiZXNzZW50aWFsIjp0cnVlfSwiYWNyIjp7InZhbHVlIjoidXJuOmJyYXNpbDpvcGVuYmFua2luZzpsb2EyIiwiZXNzZW50aWFsIjp0cnVlfX19LCJtYXhfYWdlIjo5MDAsImlzcyI6InF1YW50b190ZXN0IiwiYXVkIjoiaHR0cHM6Ly93d3cuY2VydGlmaWNhdGlvbi5vcGVuaWQubmV0L3Rlc3QvYS9xdWFudG9fdGVzdF9ub2RlLyIsImNsaWVudF9pZCI6InF1YW50b190ZXN0IiwianRpIjoiRHVRR1duNzJ5YUV0ek4zQ2xmUzJ3UGRLMTJNSEVBVXhqeVZwV1dNa3FhVSIsImlhdCI6MTYzMTU5MDcxNywiZXhwIjoxNjMxNTkxMDE3LCJuYmYiOjE2MzE1OTA3MTd9.KMJrsOVahlo0yihP1swnR2qtWLTkXCerlvkZuRKzueJ8n-sr7JcJ3-IQDGsR9ieTpy9y_PW3xD9UYMZBOV-VqtzbPWwZcj6ks93sBWgeQgSDnw0pflAyEbGMYbaOR3rz9DZNCL9DRQlQuRgv8kKVBBzutLI7Gt8L2heBfYIGVX371VzbStVN2_YU5C9d2mLT-pyxBcdDtOgrrTHEYgTPqPrOkKAshoZ4fJ14zU9znNHAtpoIVoUT8Hovx__XXsIpbcXB8Bgb13OnznZosrnPlICyN0nutbo8Ms-yX4UlxYzKF34arXtXrXJWhck3qLqkV0bRvW71yKNZ3qM9YiD5pg\",\n" +
			"  \"header\": {\n" +
			"    \"kid\": \"kid\",\n" +
			"    \"typ\": \"oauth-authz-req+jwt\",\n" +
			"    \"alg\": \"PS256\"\n" +
			"  },\n" +
			"  \"claims\": {\n" +
			"    \"iss\": \"quanto_test\",\n" +
			"    \"response_type\": \"code id_token\",\n" +
			"    \"code_challenge_method\": \"S256\",\n" +
			"    \"nonce\": \"ef7c4d18f8e976f7c5bdf6a29536c5238250a57026fea61b8674390e24afab48\",\n" +
			"    \"client_id\": \"quanto_test\",\n" +
			"    \"response_mode\": \"form_post\",\n" +
			"    \"max_age\": 900,\n" +
			"    \"aud\": \"https://www.certification.openid.net/test/a/quanto_test_node/\",\n" +
			"    \"nbf\": 1631590717,\n" +
			"    \"scope\": \"openid consent:urn:conformance.oidf:sdKOsNw7HS accounts\",\n" +
			"    \"claims\": {\n" +
			"      \"user_info\": {\n" +
			"        \"acr\": {\n" +
			"          \"value\": \"urn:brasil:openbanking:loa2\",\n" +
			"          \"essential\": true\n" +
			"        },\n" +
			"        \"auth_time\": {\n" +
			"          \"essential\": true\n" +
			"        },\n" +
			"        \"given_name\": {\n" +
			"          \"essential\": true\n" +
			"        }\n" +
			"      },\n" +
			"      \"id_token\": {\n" +
			"        \"acr\": {\n" +
			"          \"value\": \"urn:brasil:openbanking:loa2\",\n" +
			"          \"essential\": true\n" +
			"        },\n" +
			"        \"auth_time\": {\n" +
			"          \"essential\": true\n" +
			"        },\n" +
			"        \"given_name\": {\n" +
			"          \"essential\": true\n" +
			"        }\n" +
			"      }\n" +
			"    },\n" +
			"    \"redirect_uri\": \"https://tpp.localhost/cb\",\n" +
			"    \"state\": \"de1764bdbb68c90d622c2b317ee7dee1719eca05bb6ce5304a2bd85eba82049f\",\n" +
			"    \"exp\": 1631591017,\n" +
			"    \"iat\": 1631590717,\n" +
			"    \"code_challenge\": \"QxF0r_wYYM0BsV5AYMOyrY2E7ec7Mra30djFbMW1Qc8\",\n" +
			"    \"jti\": \"DuQGWn72yaEtzN3ClfS2wPdK12MHEAUxjyVpWWMkqaU\"\n" +
			"  },\n" +
			"  \"jwe_header\": {\n" +
			"    \"kid\": \"kid1\",\n" +
			"    \"cty\": \"oauth-authz-req+jwt\",\n" +
			"    \"enc\": \"A256GCM\",\n" +
			"    \"alg\": \"RSA-OAEP\"\n" +
			"  }\n" +
			"}").getAsJsonObject();
		JsonObject clientPublicJwks = jsonParser.parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"use\": \"sig\",\n" +
			"      \"kid\": \"kid\",\n" +
			"      \"alg\": \"PS256\",\n" +
			"      \"n\": \"hPnwtvrlgvnXLD_8ZvqgAQSjazwytJSo_JoTIR3kNJ-ePFmm_UBPGVdZYrXjOH_ahiQ_wxgLto1GCKaYJOxv8LThF-Fr1JX2FLB59fI_Ol17irGi_WRna0tddYzzPVF5qpQylQSKGQX1Q75zPJFIHNvBZyNMnHHLvx0NoWhq-gGcXJbVaFYDwCkLYmuykAA3z9RCVWHbO-hjZjRUn8IHaOFNiKGLOpD38hZ33sBH_LQm25fnv0qt1djnD53GB2APyGpaZtMiNQ9zMMDdMXvyZAqCJJXMdBlRx_c_ehY61VdN32VTyy3jtcsE7OWKhWkhnL51YxjDcoEDQPFb2BmRYw\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject();
		JsonObject client = new JsonObject();
		client.addProperty("client_id", "quanto_test");
		client.addProperty("scope", "openid accounts");
		client.addProperty("redirect_uri", "https://tpp.localhost/cb");
		client.addProperty("certificate", "-----BEGIN CERTIFICATE-----\n" +
			"MIIEaDCCA1CgAwIBAgIJANYuMO6BSWEfMA0GCSqGSIb3DQEBCwUAMIGJMQswCQYD\n" +
			"VQQGEwJOTDESMBAGA1UEBwwJQUNNRSBDSVRZMRYwFAYDVQQKDA1BQ01FIFdFQlNJ\n" +
			"VEVTMRAwDgYDVQQLDAdBQ01FIElUMRUwEwYDVQQDDAxBQ01FIFJPT1QgQ0ExJTAj\n" +
			"BgkqhkiG9w0BCQEWFmx1Y2FzLmZlbGlwaW5pQHF1YW4udG8wHhcNMjEwMzEwMDUz\n" +
			"NjE4WhcNMjIwNzIzMDUzNjE4WjB+MQswCQYDVQQGEwJOTDESMBAGA1UEBwwJQUNN\n" +
			"RSBDSVRZMQ0wCwYDVQQKDARBQ01FMRYwFAYDVQQLDA1BQ01FIFdFQlNJVEVTMQ0w\n" +
			"CwYDVQQDDARBQ01FMSUwIwYJKoZIhvcNAQkBFhZsdWNhcy5mZWxpcGluaUBxdWFu\n" +
			"LnRvMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvEAvl60WjuoAa+xL\n" +
			"p3v/dPIbsJ7xZp/IZ4YysJFDqg1zdHwA2FojxEB5PgDgj4XzhTo9e1MiaHZd3lSl\n" +
			"MESMGKo3L/8De2cu61E2GrSBO+eCgUUg02Hcfwwqvwofk8C6iHDwge/zk8fxaHpj\n" +
			"T3iTFk6w+m1/k+qFpkiAgVfJMmA4wCyr0ZPsjUexkq2nduhFRhkr/CVymGn76cak\n" +
			"IgL1HTDQzBPqUtC8XV5B5CFkOxktn2x2MASjf7fqYFmtAyTIXDyGPjz1AC5ZzPnh\n" +
			"U2JuSib5xaKC42lx2P6BIpEIjT8nE3XRgFX6qmtUXcU/uK3oDyaWx6eiJAkObRnX\n" +
			"Uui3mQIDAQABo4HcMIHZMIGoBgNVHSMEgaAwgZ2hgY+kgYwwgYkxCzAJBgNVBAYT\n" +
			"Ak5MMRIwEAYDVQQHDAlBQ01FIENJVFkxFjAUBgNVBAoMDUFDTUUgV0VCU0lURVMx\n" +
			"EDAOBgNVBAsMB0FDTUUgSVQxFTATBgNVBAMMDEFDTUUgUk9PVCBDQTElMCMGCSqG\n" +
			"SIb3DQEJARYWbHVjYXMuZmVsaXBpbmlAcXVhbi50b4IJAN0JnxEd8RroMAkGA1Ud\n" +
			"EwQCMAAwCwYDVR0PBAQDAgTwMBQGA1UdEQQNMAuCCWxvY2FsaG9zdDANBgkqhkiG\n" +
			"9w0BAQsFAAOCAQEAj3xYFondsQE3U4xukQsCW0IlfzgWsJ1sfZHsJTShzNaeTrz8\n" +
			"8x6KQlpF1J7q9cwT/x7iDCb7xjPyfix16FXgzGJFt0mJygHrGAaZGQiZYGQTAxAA\n" +
			"fck1bCbZyBaYeVC6g0H1KT7FGU6H4WV3M+8y8xBWiOlfOPbo3uD0Diniyyfa45aE\n" +
			"6DkUmSYa4DXHpVD3H/TISHJvWBYc/V11wSpuZGaVlKnBjyZgXw/yZFURZ7a8RySM\n" +
			"/bS6NImyoDUtOvGAEKK5RYbmlxrgz2bhC1Gj6bQ7U1lBV1aA7yEt8Z/m5vd08+xu\n" +
			"IzfcAugSWarNhDdb5Oh22Jwy4nsO3bH4Q4d52g==\n" +
			"-----END CERTIFICATE-----");
		client.add("jwks", jsonParser.parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"kid\": \"kid\",\n" +
			"      \"n\": \"hPnwtvrlgvnXLD_8ZvqgAQSjazwytJSo_JoTIR3kNJ-ePFmm_UBPGVdZYrXjOH_ahiQ_wxgLto1GCKaYJOxv8LThF-Fr1JX2FLB59fI_Ol17irGi_WRna0tddYzzPVF5qpQylQSKGQX1Q75zPJFIHNvBZyNMnHHLvx0NoWhq-gGcXJbVaFYDwCkLYmuykAA3z9RCVWHbO-hjZjRUn8IHaOFNiKGLOpD38hZ33sBH_LQm25fnv0qt1djnD53GB2APyGpaZtMiNQ9zMMDdMXvyZAqCJJXMdBlRx_c_ehY61VdN32VTyy3jtcsE7OWKhWkhnL51YxjDcoEDQPFb2BmRYw\",\n" +
			"      \"use\": \"sig\",\n" +
			"      \"alg\": \"PS256\"\n" +
			"    }\n" +
			"  ]\n" +
			"}"));
		env.putObject("authorization_request_object", requestObject);
		env.putObject("client_public_jwks", clientPublicJwks);
		env.putObject("client", client);
		cond.evaluate(env);
	}

}
