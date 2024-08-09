package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidateIdTokenSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateIdTokenSignature cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateIdTokenSignature();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject goodIdToken = JsonParser.parseString("{"
			+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL2p3dC1ycC5leGFtcGxlLm5ldCIsZXhwOjAsbmJmOjAsaWF0OjB9."
			+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwks = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
			+ "},"
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdToken);
		env.putObject("server_jwks", goodServerJwks);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badToken() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badIdToken = JsonParser.parseString("{"
				+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL290aGVyLmV4YW1wbGUubmV0IixleHA6MCxuYmY6MCxpYXQ6MH0."
				+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
				+ "}").getAsJsonObject();

			JsonObject goodServerJwks = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
				+ "},"
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("id_token", badIdToken);
			env.putObject("server_jwks", goodServerJwks);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("id_token", "value");
			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingToken() {
		assertThrows(ConditionError.class, () -> {

			JsonObject goodServerJwks = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
				+ "},"
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("server_jwks", goodServerJwks);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("id_token", "value");
			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_wrongKeys() {
		assertThrows(ConditionError.class, () -> {

			JsonObject goodIdToken = JsonParser.parseString("{"
				+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL2p3dC1ycC5leGFtcGxlLm5ldCIsZXhwOjAsbmJmOjAsaWF0OjB9."
				+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
				+ "}").getAsJsonObject();

			JsonObject wrongServerJwks = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("id_token", goodIdToken);
			env.putObject("server_jwks", wrongServerJwks);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("id_token", "value");
			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badKeys() {
		assertThrows(ConditionError.class, () -> {

			JsonObject goodIdToken = JsonParser.parseString("{"
				+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL2p3dC1ycC5leGFtcGxlLm5ldCIsZXhwOjAsbmJmOjAsaWF0OjB9."
				+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
				+ "}").getAsJsonObject();

			env.putObject("id_token", goodIdToken);
			env.putString("server_jwks", "this is not a key set");

			cond.execute(env);

			verify(env, atLeastOnce()).getString("id_token", "value");
			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingKeys() {
		assertThrows(ConditionError.class, () -> {

			JsonObject goodIdToken = JsonParser.parseString("{"
				+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
				+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL2p3dC1ycC5leGFtcGxlLm5ldCIsZXhwOjAsbmJmOjAsaWF0OjB9."
				+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
				+ "}").getAsJsonObject();

			env.putObject("id_token", goodIdToken);

			cond.execute(env);

			verify(env, atLeastOnce()).getString("id_token", "value");
			verify(env, atLeastOnce()).getObject("server_jwks");

		});

	}

	@Test
	public void testEvaluate_noErrorWithKid1() {

		// { "kid": "authlete-fapidev-api-20180524", "alg": "PS256" }
		JsonObject goodIdTokenWithKid1 = JsonParser.parseString("{"
			+ "\"value\":\"eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0."
			+ "eyJzdWIiOiIxMDAxIiwiYXVkIjpbIjMwNDI1NTc0NzM5Il0sImFjciI6InVybjptYWNlOmluY29tbW9uOmlhcDpzaWx2ZXIiLCJjX2hhc2giOiJpekRMM0hBLTRRWHI1Y2U3cURPa2hBIiwic19oYXNoIjoiV2UxSlRaTkVxRld1WDM4d2pSdUpoUSIsImF1dGhfdGltZSI6MTU2NTA1OTQzNCwiaXNzIjoiaHR0cHM6Ly9mYXBpZGV2LWFzLmF1dGhsZXRlLm5ldC8iLCJleHAiOjE1NjUwNTk3MzQsImlhdCI6MTU2NTA1OTQzNCwibm9uY2UiOiJsU05tRExGeUtYIn0."
			+ "d-IBbhQYUYixD9qT9bXG_0WWbUXp7T1O80_EtU12f1eGB_uD6sS0GuxEVrBK30hMuGHXRJ9wiPJfjiKirmdvj93x_IRJotS7Tf0YszM02Si307faVqtYiFwbC2oSRZDoRDyKYzLIFrxK7Ux2uDU7tIDF2d7vsbGobcQ0AqXJb8CTZzaL_sNi5w1tHISr5qjKyesl7piM12JmjyEsKe71oeJR-8H2VPrciIM1OtJqTM36cvea7NXt2Js_2F-PZtXq2ll2xRXv7VUNDfxJON-wmgsjE7GZi2CDdrsKdCRgBU0ignOLX1RhVCuJT_JmKcypC8q6ceUjSkEJMQdF0m153g\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid1 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"authlete-fapidev-api-20180524\","
			+ "\"alg\":\"PS256\","
			+ "\"n\":\"nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid1);

		env.putObject("server_jwks", goodServerJwksWithKid1);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorWithKid2() {

		// header: { "kid": "wU3ifIIaLOUAReRB/FG6eM1P1QM=", "typ": "JWT", "alg": "PS256" }
		JsonObject goodIdTokenWithKid2 = JsonParser.parseString("{"
			+ "\"value\":\"eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUFMyNTYifQ."
			+ "eyJzdWIiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsImF1ZGl0VHJhY2tpbmdJZCI6IjlhYmVjNzY0LTc0YmUtNDBiMy1hYWQ4LTY2NDFlZWIwZmMxMy0yMjkxNDEiLCJpc3MiOiJodHRwczovL29iLnVhdC5iZG4ucHVibGljLnNhaW5zYnVyeXNiYW5rLmNsb3VkOjQ0My9zc28vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9nZW5lcmFsIiwidG9rZW5OYW1lIjoiaWRfdG9rZW4iLCJub25jZSI6IlZiUVcwMkxEUmUiLCJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpjYSIsImF1ZCI6IjhkNWNhNDY1LTEwZjEtNDk0OS05MGJjLTdmMTc1MWFlOGYxYSIsImNfaGFzaCI6Ii1TcW9nNnFzY0JIMURXa1B4Q1JmNGciLCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiJUN2ROalhDV1BIYmFDZk5odUYyOS1ENHI4MnMiLCJzX2hhc2giOiJPRUM2MldSQjBIZzlVTXZVUm1mODNRIiwiYXpwIjoiOGQ1Y2E0NjUtMTBmMS00OTQ5LTkwYmMtN2YxNzUxYWU4ZjFhIiwiYXV0aF90aW1lIjoxNTY0NTI0Nzg3LCJyZWFsbSI6Ii9nZW5lcmFsIiwiZXhwIjoxNTY0NTI4NDAzLCJ0b2tlblR5cGUiOiJKV1RUb2tlbiIsImlhdCI6MTU2NDUyNDgwM30."
			+ "orxSbO_yU8BsdIkLFsNoV7lJU403DIkSM8gh1EhXG_z4gm5CtnHVs3nYSOtRt21SrY6UepulH2O-kYQ8vgHG9-qOPxlJuW1CWd7I7sQIt5gBCC8-26Uv6QNbPB-qywgMQK1aYpRRNfPd6PCoK0RqzooQ5fJ_Sli5525vw_o-4-w7YmDgrYnp3201rjH6KE3X-wbaj9MhwXDEHKiLgMU36s0SiXGPIWUvfBZQ8bMWiAY7q5zbmlpFNHL9Q7kdPei_Paf1Z0MK__vJffnHFZoEnZmRGWgSjuCFU56QfcMu_ECeGUmn_9pthQRPoonQhVZJigKrydc58ub-43XfFcBfgA\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid2 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"EC\","
			+ "\"kid\":\"Fol7IpdKeLZmzKtCEgi1LDhSIzM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"MUOPc5byMEN9q_9gqArkd1EDajg\","
			+ "\"x5c\":[\"MIIBwjCCAWkCCQCw3GyPBTSiGzAJBgcqhkjOPQQBMGoxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxDzANBgNVBAsTBk9wZW5BTTESMBAGA1UEAxMJZXMyNTZ0ZXN0MB4XDTE3MDIwMzA5MzQ0NloXDTIwMTAzMDA5MzQ0NlowajELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0JyaXN0b2wxEDAOBgNVBAcTB0JyaXN0b2wxEjAQBgNVBAoTCUZvcmdlUm9jazEPMA0GA1UECxMGT3BlbkFNMRIwEAYDVQQDEwllczI1NnRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQ3sy05tV/3YUlPBi9jZm9NVPeuBmntrtcO3NP/1HDsgLsTZsqKHD6KWIeJNRQnONcriWVaIcZYTKNykyCVUz93MAkGByqGSM49BAEDSAAwRQIgZhTox7WpCb9krZMyHfgCzHwfu0FVqaJsO2Nl2ArhCX0CIQC5GgWD5jjCRlIWSEFSDo4DZgoQFXaQkJUSUbJZYpi9dA==\"],"
			+ "\"x\":\"N7MtObVf92FJTwYvY2ZvTVT3rgZp7a7XDtzT_9Rw7IA\","
			+ "\"y\":\"uxNmyoocPopYh4k1FCc41yuJZVohxlhMo3KTIJVTP3c\","
			+ "\"crv\":\"P-256\","
			+ "\"alg\":\"ES256\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"DkKMPE7hFVEn77WWhVuzaoFp4O8=\","
			+ "\"use\":\"enc\","
			+ "\"x5t\":\"JRxY4hJRL3sI_dAUWUEosCEQJ3A\","
			+ "\"x5c\":[\"MIIDYTCCAkmgAwIBAgIEFt4OQjANBgkqhkiG9w0BAQsFADBhMQswCQYDVQQGEwJVSzEQMA4GA1UECBMHQnJpc3RvbDEQMA4GA1UEBxMHQnJpc3RvbDESMBAGA1UEChMJRm9yZ2VSb2NrMQswCQYDVQQLEwJBTTENMAsGA1UEAxMEdGVzdDAeFw0xODA0MDMxNDIwNThaFw0yODAzMzExNDIwNThaMGExCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxCzAJBgNVBAsTAkFNMQ0wCwYDVQQDEwR0ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi7t6m4d/02dZ8dOe+DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn/ScFHpq8Tx4BzhcDb6P0+PHCo+bkQedxwhbMD412KSM2UAVQaZ+TW+ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s/BS1obWUOC+0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ/0ts3NsL83413NdbWYh+ChtP696mZbJozflF8jR9pewTbQIDAQABoyEwHzAdBgNVHQ4EFgQUDAvAglxsoXuEwI2NT1hFtVww2SUwDQYJKoZIhvcNAQELBQADggEBADiHqUwRlq1xdHP7S387vMLOr+/OUgNvDUogeyrpdj5vFve/CBxSFlcoY215eE0xzj2+bQoe5To3s8CWkP9hqB3EdhaRBfCrd8Vpvu8xBZcxQzmqwNjmeDrxNpKes717t05fDGgygUM8xIBs29JwRzHzf7e0ByJjn9fvlUjDAGZ7emCTN382F2iOeLC2ibVl7dpmsWZTINhQRbmq5L4ztOcjITk5WZnBF439oRRn68fWZVkOv2UqaKbkuMjgotNuot+ebHtOchEiwKz8VAK7O3/IgD6rfNBfz+c/WeoPcrfQBR4zfizw/ioR115RSywifzlwq5yziqyU04eP4wLr3cM=\"],"
			+ "\"n\":\"i7t6m4d_02dZ8dOe-DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn_ScFHpq8Tx4BzhcDb6P0-PHCo-bkQedxwhbMD412KSM2UAVQaZ-TW-ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s_BS1obWUOC-0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ_0ts3NsL83413NdbWYh-ChtP696mZbJozflF8jR9pewTbQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RSA-OAEP\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"DkKMPE7hFVEn77WWhVuzaoFp4O8=\","
			+ "\"use\":\"enc\","
			+ "\"x5t\":\"JRxY4hJRL3sI_dAUWUEosCEQJ3A\","
			+ "\"x5c\":[\"MIIDYTCCAkmgAwIBAgIEFt4OQjANBgkqhkiG9w0BAQsFADBhMQswCQYDVQQGEwJVSzEQMA4GA1UECBMHQnJpc3RvbDEQMA4GA1UEBxMHQnJpc3RvbDESMBAGA1UEChMJRm9yZ2VSb2NrMQswCQYDVQQLEwJBTTENMAsGA1UEAxMEdGVzdDAeFw0xODA0MDMxNDIwNThaFw0yODAzMzExNDIwNThaMGExCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxCzAJBgNVBAsTAkFNMQ0wCwYDVQQDEwR0ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi7t6m4d/02dZ8dOe+DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn/ScFHpq8Tx4BzhcDb6P0+PHCo+bkQedxwhbMD412KSM2UAVQaZ+TW+ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s/BS1obWUOC+0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ/0ts3NsL83413NdbWYh+ChtP696mZbJozflF8jR9pewTbQIDAQABoyEwHzAdBgNVHQ4EFgQUDAvAglxsoXuEwI2NT1hFtVww2SUwDQYJKoZIhvcNAQELBQADggEBADiHqUwRlq1xdHP7S387vMLOr+/OUgNvDUogeyrpdj5vFve/CBxSFlcoY215eE0xzj2+bQoe5To3s8CWkP9hqB3EdhaRBfCrd8Vpvu8xBZcxQzmqwNjmeDrxNpKes717t05fDGgygUM8xIBs29JwRzHzf7e0ByJjn9fvlUjDAGZ7emCTN382F2iOeLC2ibVl7dpmsWZTINhQRbmq5L4ztOcjITk5WZnBF439oRRn68fWZVkOv2UqaKbkuMjgotNuot+ebHtOchEiwKz8VAK7O3/IgD6rfNBfz+c/WeoPcrfQBR4zfizw/ioR115RSywifzlwq5yziqyU04eP4wLr3cM=\"],"
			+ "\"n\":\"i7t6m4d_02dZ8dOe-DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn_ScFHpq8Tx4BzhcDb6P0-PHCo-bkQedxwhbMD412KSM2UAVQaZ-TW-ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s_BS1obWUOC-0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ_0ts3NsL83413NdbWYh-ChtP696mZbJozflF8jR9pewTbQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RSA-OAEP-256\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RS512\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"DkKMPE7hFVEn77WWhVuzaoFp4O8=\","
			+ "\"use\":\"enc\","
			+ "\"x5t\":\"JRxY4hJRL3sI_dAUWUEosCEQJ3A\","
			+ "\"x5c\":[\"MIIDYTCCAkmgAwIBAgIEFt4OQjANBgkqhkiG9w0BAQsFADBhMQswCQYDVQQGEwJVSzEQMA4GA1UECBMHQnJpc3RvbDEQMA4GA1UEBxMHQnJpc3RvbDESMBAGA1UEChMJRm9yZ2VSb2NrMQswCQYDVQQLEwJBTTENMAsGA1UEAxMEdGVzdDAeFw0xODA0MDMxNDIwNThaFw0yODAzMzExNDIwNThaMGExCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxCzAJBgNVBAsTAkFNMQ0wCwYDVQQDEwR0ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi7t6m4d/02dZ8dOe+DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn/ScFHpq8Tx4BzhcDb6P0+PHCo+bkQedxwhbMD412KSM2UAVQaZ+TW+ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s/BS1obWUOC+0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ/0ts3NsL83413NdbWYh+ChtP696mZbJozflF8jR9pewTbQIDAQABoyEwHzAdBgNVHQ4EFgQUDAvAglxsoXuEwI2NT1hFtVww2SUwDQYJKoZIhvcNAQELBQADggEBADiHqUwRlq1xdHP7S387vMLOr+/OUgNvDUogeyrpdj5vFve/CBxSFlcoY215eE0xzj2+bQoe5To3s8CWkP9hqB3EdhaRBfCrd8Vpvu8xBZcxQzmqwNjmeDrxNpKes717t05fDGgygUM8xIBs29JwRzHzf7e0ByJjn9fvlUjDAGZ7emCTN382F2iOeLC2ibVl7dpmsWZTINhQRbmq5L4ztOcjITk5WZnBF439oRRn68fWZVkOv2UqaKbkuMjgotNuot+ebHtOchEiwKz8VAK7O3/IgD6rfNBfz+c/WeoPcrfQBR4zfizw/ioR115RSywifzlwq5yziqyU04eP4wLr3cM=\"],"
			+ "\"n\":\"i7t6m4d_02dZ8dOe-DFcuUYiOWueHlNkFwdUfOs06eUETOV6Y9WCXu3D71dbF0Fhou69ez5c3HAZrSVS2qC1Htw9NkVlLDeED7qwQQMmSr7RFYNQ6BYekAtn_ScFHpq8Tx4BzhcDb6P0-PHCo-bkQedxwhbMD412KSM2UAVQaZ-TW-ngdaaVEs1Cgl4b8xxZ9ZuApXZfpddNdgvjBeeYQbZnaqU3b0P5YE0s0YvIQqYmTjxh4RyLfkt6s_BS1obWUOC-0ChRWlpWE7QTEVEWJP5yt8hgZ5MecTmBi3yZ_0ts3NsL83413NdbWYh-ChtP696mZbJozflF8jR9pewTbQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RSA1_5\""
			+ "},"

			+ "{"
			+ "\"kty\":\"EC\","
			+ "\"kid\":\"I4x/IijvdDsUZMghwNq2gC/7pYQ=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"GxQ9K-sxpsH487eSkJ7lE_SQodk\","
			+ "\"x5c\":[\"MIIB/zCCAYYCCQDS7UWmBdQtETAJBgcqhkjOPQQBMGoxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxDzANBgNVBAsTBk9wZW5BTTESMBAGA1UEAxMJZXMzODR0ZXN0MB4XDTE3MDIwMzA5MzgzNFoXDTIwMTAzMDA5MzgzNFowajELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0JyaXN0b2wxEDAOBgNVBAcTB0JyaXN0b2wxEjAQBgNVBAoTCUZvcmdlUm9jazEPMA0GA1UECxMGT3BlbkFNMRIwEAYDVQQDEwllczM4NHRlc3QwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASTnBK9b/omE64KP7090NZ0QDigf3RFKYLYZOWKJQcCFePquZN0TZL7/MaYwXG5+5Vd9tH61GhVxkwKhSuQq25lQjJ8KADLxvQVac9mp6/Cl2hPMZAE5dI2Lq4i03+ji0cwCQYHKoZIzj0EAQNoADBlAjAsZyt7iNOBtYCtdEro1QJ2RQ8LOARQ4MBu+8LJNICdi+slnmdh75ulI9UcEhgqiFsCMQD32u2nm26LXbuwKC9DP8BjV+CdFPOZdv/UUfiQBNXz0cJE+uhEfnMJtgvwcovqntI=\"],"
			+ "\"x\":\"k5wSvW_6JhOuCj-9PdDWdEA4oH90RSmC2GTliiUHAhXj6rmTdE2S-_zGmMFxufuV\","
			+ "\"y\":\"XfbR-tRoVcZMCoUrkKtuZUIyfCgAy8b0FWnPZqevwpdoTzGQBOXSNi6uItN_o4tH\","
			+ "\"crv\":\"P-384\","
			+ "\"alg\":\"ES384\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RS256\""
			+ "},"

			+ "{"
			+ "\"kty\":\"EC\","
			+ "\"kid\":\"pZSfpEq8tQPeiIe3fnnaWnnr/Zc=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"6syJZMj8X0Adm-XNzWHHIl_3kG4\","
			+ "\"x5c\":[\"MIICSTCCAawCCQD+h7BW+8vxbTAJBgcqhkjOPQQBMGoxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxDzANBgNVBAsTBk9wZW5BTTESMBAGA1UEAxMJZXM1MTJ0ZXN0MB4XDTE3MDIwMzA5NDA0OVoXDTIwMTAzMDA5NDA0OVowajELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0JyaXN0b2wxEDAOBgNVBAcTB0JyaXN0b2wxEjAQBgNVBAoTCUZvcmdlUm9jazEPMA0GA1UECxMGT3BlbkFNMRIwEAYDVQQDEwllczUxMnRlc3QwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABAB3VSmzQx8pvjIlIenGmqHf5LafD1zeoNcyCi85WgkjmT/NiimkLH8JbQCpzK8NdvZ1cftpLfMdSdaadQA3vR7V7QFKoUSnGLwOpRJSN1K36r6boVbMhBQUOHDPxPb+Fhp0XP6a4ok1Wv1Au2HwrUCU/RfDnNtb/4ue0qdzKv78ObnkXTAJBgcqhkjOPQQBA4GLADCBhwJCAd0cIC8QSVn2bp3DGYXxkz5vPNmR7Mv22E2WaWtHlsYcBIY8E7Kd4wxVD+otogDFf4fcFmA34tk5n4PLa67wS26CAkExH1YP2rFbF3LQZVEjTHOwTh+K5S0cIxmzTGx7nnH9+dnxSpCaxKjQ/L//pH/siWe6h/dmUkTY3Y9t939ypY1Blw==\"],"
			+ "\"x\":\"AHdVKbNDHym-MiUh6caaod_ktp8PXN6g1zIKLzlaCSOZP82KKaQsfwltAKnMrw129nVx-2kt8x1J1pp1ADe9HtXt\","
			+ "\"y\":\"AUqhRKcYvA6lElI3UrfqvpuhVsyEFBQ4cM_E9v4WGnRc_priiTVa_UC7YfCtQJT9F8Oc21v_i57Sp3Mq_vw5ueRd\","
			+ "\"crv\":\"P-521\","
			+ "\"alg\":\"ES512\""
			+ "},"

			// selected key by 'kid' and 'alg' and 'use:sig'
			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"PS256\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"RS384\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"PS512\""
			+ "},"

			+ "{"
			+ "\"kty\":\"RSA\","
			+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
			+ "\"use\":\"sig\","
			+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
			+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
			+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
			+ "\"e\":\"AQAB\","
			+ "\"alg\":\"PS384\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid2);

		env.putObject("server_jwks", goodServerJwksWithKid2);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorWithKid3() {

		// header: { "kid": "lqyzSEQSSpNQMNv6POwYgOtgo2Q", "typ": "JWT", "alg": "PS256" }
		JsonObject goodIdTokenWithKid3 = JsonParser.parseString("{"
			+ "\"value\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJQUzI1NiIsImtpZCI6ImxxeXpTRVFTU3BOUU1OdjZQT3dZZ090Z28yUSJ9."
			+ "ew0KICAic3ViIjogInZBLWE2eVZZT1k5OWs0Z3JJTDJhSkNDX3Z2akZJaVd2dWp6aU9KbWlzbE0iLA0KICAiYXVkIjogImEyZjczMjg4LWRjNjUtNGRmYi1hN2NlLWIxNWE2OTRlNGM1ZSIsDQogICJjX2hhc2giOiAiZzRiT3VQNW9KeGJjazhYVWxuZnZMZyIsDQogICJzX2hhc2giOiAiVHVDaHQ3cld3WGJ5S0gyYVAzVjNhdyIsDQogICJhY3IiOiAidXJuOm9wZW5iYW5raW5nOnBzZDI6c2NhIiwNCiAgImF6cCI6ICJhMmY3MzI4OC1kYzY1LTRkZmItYTdjZS1iMTVhNjk0ZTRjNWUiLA0KICAiYXV0aF90aW1lIjogMTU2NDY1Mzc3OCwNCiAgImlzcyI6ICJodHRwczovL2FwaXMudHNiLmNvLnVrL2FwaXMvb3Blbi1iYW5raW5nL3YzLjEvIiwNCiAgImV4cCI6IDE1NjQ3NDAxNzgsDQogICJpYXQiOiAxNTY0NjUzNzc4LA0KICAibm9uY2UiOiAibEc5WG5tV2h4ZSIsDQogICJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiAiYTA4YTdiZWItNGViYy00NjAyLWIwNTMtODY4NGZlMjllMjFmIg0KfQ."
			+ "SdtaH4g1aQyTevJqoMLDcwqEq4uDvanmCesNLMIpQyaJ4pHAbAv6W5bmXD2j8UFVdql6RZzPPZc4D0fDTnxxHxmJy6Cg11jJ4SWRfMeNDhIqEDzq8nc7oUdpqzvrivOmadZjbVnX968o_pSAj1xLFFRAMa57yxQuyRMz4gL0LiALv_O06r6R3petYVLqfMGomBsZYbyNc3GpWfV3er5WlsDvITmf6jdcjchJMX3UhXa9_zb7TRwfaUlkmRbRA_oAYKi28-7JOUCmsINH5Kwzo1YkGJdWIUyI3PI6N7vKc27UZcKHbnq7MHCJdKgTPFn8h-tcBLp7DKSYB7SEFPzudw\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid3 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"eFwUfAJ0dQCC5fz79n-jn6VYZYY\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"9wx1Mc0J-bbeGoOL-OSstQ9b-dh8baD8bZv6V5a-gwnyVg0jdhAp8WpOI_DwJ4ugIpC8fiCaJEbecYju3aLP6Q__ydagbnwKYqvB8h-IFdraRMh1U3ldqiZ_mh5_8Qcjm8BnGtmDzdiqc2bnEfvyFNVQcOhZsatEtq7a3bZMKFrBey87r6I_ZFUDuykOtz7MD8N8SVezy5mRGyzAYYWqFLX-APg31NT4D3edhwk5HGG8HOPL8mDSjrPPyUCSAT1MEMQYNWAfV7VdFFstvqJhGWKDbg3jm7LlQh5VBAs61EKxXiUaNgztvyMp5-3o2xmgq-Z6Eg7lVZyK8wqhZ3WP9Q\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFpjCCBI6gAwIBAgIEWf9keTANBgkqhkiG9w0BAQsFADBEMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxHzAdBgNVBAMTFk9wZW5CYW5raW5nIElzc3VpbmcgQ0EwHhcNMTkwNTEwMTIyODEyWhcNMjEwNTEwMTI1ODEyWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRVozaEFBSDEfMB0GA1UEAxMWNFNvVXMzM1ZYTXZIUlNuSURoc1ZwUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAPcMdTHNCfm23hqDi/jkrLUPW/nYfG2g/G2b+leWvoMJ8lYNI3YQKfFqTiPw8CeLoCKQvH4gmiRG3nGI7t2iz+kP/8nWoG58CmKrwfIfiBXa2kTIdVN5Xaomf5oef/EHI5vAZxrZg83YqnNm5xH78hTVUHDoWbGrRLau2t22TChawXsvO6+iP2RVA7spDrc+zA/DfElXs8uZkRsswGGFqhS1/gD4N9TU+A93nYcJORxhvBzjy/Jg0o6zz8lAkgE9TBDEGDVgH1e1XRRbLb6iYRlig24N45uy5UIeVQQLOtRCsV4lGjYM7b8jKeft6NsZoKvmehIO5VWcivMKoWd1j/UCAwEAAaOCAoEwggJ9MA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwggFSBgNVHSAEggFJMIIBRTCCAUEGCysGAQQBqHWBBgEBMIIBMDA1BggrBgEFBQcCARYpaHR0cDovL29iLnRydXN0aXMuY29tL3Byb2R1Y3Rpb24vcG9saWNpZXMwgfYGCCsGAQUFBwICMIHpDIHmVGhpcyBDZXJ0aWZpY2F0ZSBpcyBzb2xlbHkgZm9yIHVzZSB3aXRoIE9wZW4gQmFua2luZyBMaW1pdGVkIGFuZCBhc3NvY2lhdGVkIE9wZW4gQmFua2luZyBTZXJ2aWNlcy4gSXRzIHJlY2VpcHQsIHBvc3Nlc3Npb24gb3IgdXNlIGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIE9wZW4gQmFua2luZyBMaW1pdGVkIENlcnRpZmljYXRlIFBvbGljeSBhbmQgcmVsYXRlZCBkb2N1bWVudHMgdGhlcmVpbi4wcgYIKwYBBQUHAQEEZjBkMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA6BggrBgEFBQcwAoYuaHR0cDovL29iLnRydXN0aXMuY29tL3Byb2R1Y3Rpb24vaXNzdWluZ2NhLmNydDA/BgNVHR8EODA2MDSgMqAwhi5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcHJvZHVjdGlvbi9pc3N1aW5nY2EuY3JsMB8GA1UdIwQYMBaAFJ9Jv042p6zDDyvIR/QfKRvAeQsFMB0GA1UdDgQWBBQotejclChqefV9Q/F0W6fqJWZhTTANBgkqhkiG9w0BAQsFAAOCAQEAgjVv8t4VsLaIrAwDUtCVpZIeiFwyOrByhm9Gf7DGpDacset0mWWdNI186WqsF6/t+jQvdaalMjKKHr9IyFQ9It3oeVGHXLAUvYKZdddCToUXpis8oRniGjD0QYs8wtsIR2rQITGJ0tW4muqibb/E9h7b5J5QfDdEiusMM0S/jsN1C7WHbv14zTrTdugDE+LJhZbWoOMqo/SkNAD5ViWu87fmIv4aGbdEed9pSUPZ0exTnh9oH3bnG0hd4mOH5E4e6xd+hNoLo5tMDq64RkhbrOg3v+a7K3fQB7ASc0whHyeNBurIy8fonTVRX+Wp9zWT5Tx33a1YCzOLx27CsYOQpw==\"],"
			+ "\"x5t\":\"Bky8bDSkz1W0bch4YuvKkqopJq0=\","
			+ "\"x5u\":\"https://keystore.openbanking.org.uk/0015800001ZEZ3hAAH/eFwUfAJ0dQCC5fz79n-jn6VYZYY.pem\","
			+ "\"x5t#S256\":\"hbW4K0I8tcln1dv6hU50_xEOUniu962go9QkpCAPu-4=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"lqyzSEQSSpNQMNv6POwYgOtgo2Q\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"yDSgeaW59Uez9vJwjkRJtnXGnmM5wMzqcvaFKQ8sSPEq4q2K0Zt2IPMCBB-M_hf7vvUvDFSdhH0wDfEt2CjQhuMTx61OWbNW_eIzDgS04vCQR9cX4sRJ6zWgNiwRToRdJHHO44wL1oYD4b8dOB8iJCRzHDDXbqlUzTk36dQy2T51IjXwRnt-3ObAXU8l2jOtzgD5ep-MaS1_7NnRv0xV7NbJSKTBFp7tcv8cuZYHSPXuzA3xkPde7OEcPktrlkPOStWzFhtkjpksYZ0Swud4rYJXQbzqDzutIoxZ39kEAZtGNDW8I1eRPTw6knYmnVqAOHff_FalqdRzbBMQyJ_TOw\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFmzCCBIOgAwIBAgIEWf9kejANBgkqhkiG9w0BAQsFADBEMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxHzAdBgNVBAMTFk9wZW5CYW5raW5nIElzc3VpbmcgQ0EwHhcNMTkwNTEwMTIyODUxWhcNMjAwNjEwMTI1ODUxWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRVozaEFBSDEfMB0GA1UEAxMWNFNvVXMzM1ZYTXZIUlNuSURoc1ZwUjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMg0oHmlufVHs/bycI5ESbZ1xp5jOcDM6nL2hSkPLEjxKuKtitGbdiDzAgQfjP4X+771LwxUnYR9MA3xLdgo0IbjE8etTlmzVv3iMw4EtOLwkEfXF+LESes1oDYsEU6EXSRxzuOMC9aGA+G/HTgfIiQkcxww126pVM05N+nUMtk+dSI18EZ7ftzmwF1PJdozrc4A+XqfjGktf+zZ0b9MVezWyUikwRae7XL/HLmWB0j17swN8ZD3XuzhHD5La5ZDzkrVsxYbZI6ZLGGdEsLneK2CV0G86g87rSKMWd/ZBAGbRjQ1vCNXkT08OpJ2Jp1agDh33/xWpanUc2wTEMif0zsCAwEAAaOCAnYwggJyMA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIIBUgYDVR0gBIIBSTCCAUUwggFBBgsrBgEEAah1gQYBATCCATAwNQYIKwYBBQUHAgEWKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wcm9kdWN0aW9uL3BvbGljaWVzMIH2BggrBgEFBQcCAjCB6QyB5lRoaXMgQ2VydGlmaWNhdGUgaXMgc29sZWx5IGZvciB1c2Ugd2l0aCBPcGVuIEJhbmtpbmcgTGltaXRlZCBhbmQgYXNzb2NpYXRlZCBPcGVuIEJhbmtpbmcgU2VydmljZXMuIEl0cyByZWNlaXB0LCBwb3NzZXNzaW9uIG9yIHVzZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuIEJhbmtpbmcgTGltaXRlZCBDZXJ0aWZpY2F0ZSBQb2xpY3kgYW5kIHJlbGF0ZWQgZG9jdW1lbnRzIHRoZXJlaW4uMHIGCCsGAQUFBwEBBGYwZDAmBggrBgEFBQcwAYYaaHR0cDovL29iLnRydXN0aXMuY29tL29jc3AwOgYIKwYBBQUHMAKGLmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wcm9kdWN0aW9uL2lzc3VpbmdjYS5jcnQwPwYDVR0fBDgwNjA0oDKgMIYuaHR0cDovL29iLnRydXN0aXMuY29tL3Byb2R1Y3Rpb24vaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBSfSb9ONqesww8ryEf0HykbwHkLBTAdBgNVHQ4EFgQUN9Y8CpcUgBG0a+3Ck1ep/iYdafcwDQYJKoZIhvcNAQELBQADggEBAJZlAfW5L0zh2Y0UvXKVFuc9zYsnxmDSopwZZCfMib5h8R1Xra3uHy2zww3+3Z+7DcYDh1v08AXSTPbJLRr/e9rrThRi2KSRajjZO2mcd2iLPOJCgxtUNgU4Zfl7omKKbW5mkwFYvukIhEGJ01HiW4rWswJpVAG4lRy+31SyeEOYeZKZrGKhFkTGBiIAxhH275hUUVNOi3Sz5BDBt9ZAMM9FWq34snyblajuTn4+XORr/qZIHUlyWSV2h/y6cc7g/fwE932sHrxBNpD6q7hOfWtwHovKrW5Q/VK1Z10f/78mrP1K5Vx6DtJ+gFJoyXbPfRFD/di7DcHOy7V+uhVSMS4=\"],"
			+ "\"x5t\":\"tO3nGwvcofMJcqEVYBDCGwa5AhQ=\","
			+ "\"x5u\":\"https://keystore.openbanking.org.uk/0015800001ZEZ3hAAH/lqyzSEQSSpNQMNv6POwYgOtgo2Q.pem\","
			+ "\"x5t#S256\":\"8ybORyz33f-ipEs-zobf4rKkCilpeoMhWsmkolfNpxk=\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid3);

		env.putObject("server_jwks", goodServerJwksWithKid3);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorWithKid4() {

		// header: { "kid": "6xnDk3QKSqHWmjNXPS1jZEMXrMc", "typ": "JWT", "alg": "PS256" }
		JsonObject goodIdTokenWithKid4 = JsonParser.parseString("{"
			+ "\"value\":\"eyJhbGciOiJQUzI1NiIsImtpZCI6IjZ4bkRrM1FLU3FIV21qTlhQUzFqWkVNWHJNYyIsInR5cCI6IkpXVCJ9."
			+ "eyJhdWQiOiJvYXV0aDJjbGllbnRfMDAwMDlpQ2o2TjJGeDN5M2JwUVNYSiIsImV4cCI6MTU2NDg0MTgzNywiaWF0IjoxNTY0NzU1NDM3LCJpc3MiOiJodHRwczovL2FwaS5zMTAxLm5vbnByb2QtZmZzLmlvL29wZW4tYmFua2luZy8iLCJuYmYiOjE1NjQ3NTU0MzcsInN1YiI6Im9iYWlzcGFjY291bnRpbmZvcm1hdGlvbmNvbnNlbnRfMDAwMDlsVGhkRG02ZTNRaVBKeG96eCIsImF1dGhfdGltZSI6MTU2NDc1NTQzNywibm9uY2UiOiJWYkw4ZjJkQmZ1Iiwib3BlbmJhbmtpbmdfaW50ZW50X2lkIjoib2JhaXNwYWNjb3VudGluZm9ybWF0aW9uY29uc2VudF8wMDAwOWxUaGREbTZlM1FpUEp4b3p4IiwiYWNyIjoidXJuOm9wZW5iYW5raW5nOnBzZDI6c2NhIiwic19oYXNoIjoiUzdjREdNTV80MmhtTXoxY1poQmtwdyIsImNfaGFzaCI6ImNzYzZSbUF3cngxdF80V3Z4cmR3R1EifQ."
			+ "LmW8pHfasMf24jyJnZSq-l03xd5XVFHAyV1YSNcooeD9D-OBOl8akiE5OokbUf4q2soBXgiBNKrttGIQZ_CxeEc6E9rhM6K66KZttIX0WcwFfM48nHHC9yoFD04pzkjOndYQa8MBYYQ9EfeplC97NVzNPAbViMMBzPVuyPbgECliQkQcp7--PKtYAWQgwImTKqGobUrjSgpd86EzR4yBKzS030NOQYnaBPB91Mr33a5sxOtmWeAf7UwwamjidesM0yA4TzthW4QeO-JMOlUk-qBgqRr1AspT2lxGYNnmy75euOpKIiyB3XIZZy5d3Oq4VwRNQAY3UCkMr7HKhjjb7A\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid4 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"zAxT-N6lLM3pjyieDBrCMgqiAG4\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"ugDMJpFnJeU4_oLGT8es5mbw8C9TqdzqGWoHWQfDmNSCCK1H3ZifUs8OHT0krybiGpvmbXtgRt03sjo984KVbp7tXBIb0wzH_6dTrYUYDBH23bNJjoJHrS5DXoaw5uEjea9GPtOvOR-_Yfp-EWCUDelq25j9qtFpVFAIxQwOGFH68VMUSIkcwcEZI_N3r8bKwEhz6aBgst2E41QfPouxh9tf4R4M05OL1B8-u16qYBpdFK3wpAEO7uL4zRyzekSz07spDO0NWhn1835aPIZ85vOGJZTmie_kqVNxhkPft37l7dXMmLqRmBTJe89jyDSHCd5v-Ga6fryzbGvTxVVhew\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFZDCCBEygAwIBAgIEWcV4HTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMzE4MTU0NzUwWhcNMjAwNDE4MTYxNzUwWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDEwM1U5UkFBVTEfMB0GA1UEAxMWY01LSU03RWwyVUZINFJsdW00dFhyMDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALoAzCaRZyXlOP6Cxk/HrOZm8PAvU6nc6hlqB1kHw5jUggitR92Yn1LPDh09JK8m4hqb5m17YEbdN7I6PfOClW6e7VwSG9MMx/+nU62FGAwR9t2zSY6CR60uQ16GsObhI3mvRj7Trzkfv2H6fhFglA3patuY/arRaVRQCMUMDhhR+vFTFEiJHMHBGSPzd6/GysBIc+mgYLLdhONUHz6LsYfbX+EeDNOTi9QfPrteqmAaXRSt8KQBDu7i+M0cs3pEs9O7KQztDVoZ9fN+WjyGfObzhiWU5onv5KlTcYZD37d+5e3VzJi6kZgUyXvPY8g0hwneb/hmun68s2xr08VVYXsCAwEAAaOCAjAwggIsMCoGA1UdEQQjMCGCH29wZW5iYW5raW5nLnMxMDEubm9ucHJvZC1mZnMuaW8wDgYDVR0PAQH/BAQDAgeAMCAGA1UdJQEB/wQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjCB4AYDVR0gBIHYMIHVMIHSBgsrBgEEAah1gQYBZDCBwjAqBggrBgEFBQcCARYeaHR0cDovL29iLnRydXN0aXMuY29tL3BvbGljaWVzMIGTBggrBgEFBQcCAjCBhgyBg1VzZSBvZiB0aGlzIENlcnRpZmljYXRlIGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIE9wZW5CYW5raW5nIFJvb3QgQ0EgQ2VydGlmaWNhdGlvbiBQb2xpY2llcyBhbmQgQ2VydGlmaWNhdGUgUHJhY3RpY2UgU3RhdGVtZW50MG0GCCsGAQUFBwEBBGEwXzAmBggrBgEFBQcwAYYaaHR0cDovL29iLnRydXN0aXMuY29tL29jc3AwNQYIKwYBBQUHMAKGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3J0MDoGA1UdHwQzMDEwL6AtoCuGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3JsMB8GA1UdIwQYMBaAFFBzkcYhctN39P4AEgaBXHl5bj9QMB0GA1UdDgQWBBRbc3c8Ho2HtNhXFwlBEYPdaVyBlDANBgkqhkiG9w0BAQsFAAOCAQEAlCDlbUoZjfltUll2qGHJqGJQH4GMAFkzClSJ9zEZUjo+0gHw2hEVfZp+Vf7M16kI4LCsm4kCLAt4snQJE7S1h77KUYrYgeBPk0gNL9U6eUL2ePxwCagMC2oGO8uN1oUmF8X9FevYdxc1iMs3kIK8KXQbSo67hPtGOkR7AmK83pKLoL6LjMadhduXExag76y0x/CX2DFxZtjZEu0pkia6CFRyaFdheKAM0jMRfEwERBPXKggXiJIygEBKwr7jnU6a9KTJgz6UYDsM320DZZptjcd1JKejF5At8WodqsgzbVHok2XK6+69bjJe29n18nHIS4kYjW3xkLKamVUUJHLjAA==\"],"
			+ "\"x5t\":\"9iBBQ2_FJMQPWuWwWQxWeI1SQ3Y=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/001580000103U9RAAU/zAxT-N6lLM3pjyieDBrCMgqiAG4.pem\","
			+ "\"x5t#S256\":\"rXVegWtzAJN1ZizhXNSQo7hWz0BwbipyqaIuMLFvEQ4=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"6xnDk3QKSqHWmjNXPS1jZEMXrMc\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"zRPMMajgekUbVpcFfjT55SrxOklwM8FXtsgxuacDQmfT4543G2OEy4unMVW3ipDNkbnPXD2zBF_63EreT_WNgi2MDTkvXo4SovA4C6Z0bPf2BavBc05amaKoRhcZtok1kh-OCGMbrAVc5DVwhUT6THMrLFTgE3R3FcZK3r83BgcP78_80Wtql9QfEM6V5mRVp5oUVKfmYvgkl7gCDKJ1M1zvzj3mBu792WKdEJfFC4iuOKxjwr_xeutEEAk3ovcx8voAxVdEtcsmlhLdtVexn2zBHg5S7Vr2ce1GmIjU2eB5_1t9jo223P1_eoFgmSnipw50RwFadJrhRlmJEWUMoQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcV4HjANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMzE4MTU0ODM5WhcNMjAwNDE4MTYxODM5WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDEwM1U5UkFBVTEfMB0GA1UEAxMWY01LSU03RWwyVUZINFJsdW00dFhyMDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM0TzDGo4HpFG1aXBX40+eUq8TpJcDPBV7bIMbmnA0Jn0+OeNxtjhMuLpzFVt4qQzZG5z1w9swRf+txK3k/1jYItjA05L16OEqLwOAumdGz39gWrwXNOWpmiqEYXGbaJNZIfjghjG6wFXOQ1cIVE+kxzKyxU4BN0dxXGSt6/NwYHD+/P/NFrapfUHxDOleZkVaeaFFSn5mL4JJe4AgyidTNc78495gbu/dlinRCXxQuIrjisY8K/8XrrRBAJN6L3MfL6AMVXRLXLJpYS3bVXsZ9swR4OUu1a9nHtRpiI1Nngef9bfY6Nttz9f3qBYJkp4qcOdEcBWnSa4UZZiRFlDKECAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFEkDQcOvgMPmrpZMWwpW4Z3sydM+MA0GCSqGSIb3DQEBCwUAA4IBAQArXLbAYFjaBsN1/oPkZqEJV+DYq3NAjCVj/VX6BrQwcM8SCbZS1RacboCEnY5g7DLxgmYuSiaXSx2wdOfSMCTC0J2lUmrkH1GqVv8bacb7Xd1jZyLLHFAG7PcU1002yYANeRAnC9wZO8xw07Xkl7X2DeYOywdt3rLlJMcOfisJPJ0i0d+O9rOxVHazCPoMfJtslJOPImRjeZoka/dBWUAkM8RSKwo3sXPUJE1RsLPwAwzkd6Gb5W+z45CnwTyt4jrfqmO+Stt7CdO2sSybFYkt0abGvQDtlHu0b9nwfCMgJgtYH/JaW5CqdSErNqoo9s0uPIbmUx6YxUI66Z/mhyU0\"],"
			+ "\"x5t\":\"k3NT6-edV2jOvtjrcWTNXOU5_pg=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/001580000103U9RAAU/6xnDk3QKSqHWmjNXPS1jZEMXrMc.pem\","
			+ "\"x5t#S256\":\"A85gjzWun6NtG2JVG_8DU-_Np40kWQ_yMcCM_3RQWlU=\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid4);

		env.putObject("server_jwks", goodServerJwksWithKid4);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorWithKid5() {

		// header: { "kid": "CdVV3eCHr_fR6JFTJnGgOZ2z7ac", "alg": "PS256" }
		JsonObject goodIdTokenWithKid5 = JsonParser.parseString("{"
			+ "\"value\":\"eyJhbGciOiJQUzI1NiIsImtpZCI6IkNkVlYzZUNIcl9mUjZKRlRKbkdnT1oyejdhYyJ9."
			+ "eyJpc3MiOiJodHRwczovLzE1OC4xNzUuOTUuMjUxIiwic3ViIjoiZWNhMmUxZTQtNzI2ZC01YmYzLWJmMWEtN2U2NDYxZTViZTk4IiwiYXVkIjoia0RORlRUY3FuNU5wN0JaT0xtRG5VZCIsImlhdCI6MTU2NTAxMDQ4NiwiZXhwIjoxNTY1MDEwNzg2LCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiJlY2EyZTFlNC03MjZkLTViZjMtYmYxYS03ZTY0NjFlNWJlOTgiLCJub25jZSI6IjhXaTAyU2trR0ciLCJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpzY2EiLCJjX2hhc2giOiJjMlBVRTVWZjdZbUhvYkQ5ZzQ0MVB3Iiwic19oYXNoIjoiR1pEbEZRbnE5SlVGeEllN0c3WnNvUSJ9."
			+ "f_5LQqir9jMczLyKx43miUk2-EhxxTM3FRVfStzRTYUJfdfqFGyYKqSkI5S17Pr6jPDDe8D8MUO_Rrdyfaa178dYfTLtrIQXchwlApSH9eo5OQeCvX71B7LYE9I7XXP6SklC4vQWCPzC9NBfEEWLP4DgzGYrhYeFGz70JpCEUDzWLBRV-xNJpwUyaZbZMXizuxuQPSCQoledix4nZjtThAU2ND_vW1RDHF_SXTepsYQj9RPn-t4ZjhS5CMcu36prz74H6MHxfLlgtBiJZhc0hJb6O88Ae3Y6ojuL-o-CziJP1J0kKpo-TGogRX23Stdn2Id38hYGpZan9T_BnhGCtA\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid5 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"CdVV3eCHr_fR6JFTJnGgOZ2z7ac\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"zXBAnthgLmq-rHXs7swU3V1lzL9XxGEg1ejjKEJ3VN6buv9z0kHEsqJBG0jVn0jKEspF0IZRLenxruYe0Ojgg2t1ATBBMbV8Gm7ypM7WOI-WhWQNOMgp-00Js2uckg2XQhalg1dk_NBuWUAdrQ9bmFzXF8_T6G4v53nELl7U-nBBopU32dEKWrmQFpvB25sPLoYmwv-gHf6OsrEQ2YfpuIKJ98tINNDZqgaPXndr9TyKo66XCJLFkYM6_JFoeQLFZfEo59aSb9iFDXnIq4sbWyhtUGeRTfc8fvRHGLIotvxrS6vOoFkHK9PCSt-aZxhPsox-iH1YuypgZf2XvuGf_Q\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcVuEjANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMjExMTMxMDQ3WhcNMjAwMzExMTM0MDQ3WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWQ2RRa1RSYzQwdHM0cjUxY0JUcGxiWDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM1wQJ7YYC5qvqx17O7MFN1dZcy/V8RhINXo4yhCd1Tem7r/c9JBxLKiQRtI1Z9IyhLKRdCGUS3p8a7mHtDo4INrdQEwQTG1fBpu8qTO1jiPloVkDTjIKftNCbNrnJINl0IWpYNXZPzQbllAHa0PW5hc1xfP0+huL+d5xC5e1PpwQaKVN9nRClq5kBabwdubDy6GJsL/oB3+jrKxENmH6biCiffLSDTQ2aoGj153a/U8iqOulwiSxZGDOvyRaHkCxWXxKOfWkm/YhQ15yKuLG1sobVBnkU33PH70RxiyKLb8a0urzqBZByvTwkrfmmcYT7KMfoh9WLsqYGX9l77hn/0CAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFASl+iC3QtYqlBIc6E0EAPyYGN0JMA0GCSqGSIb3DQEBCwUAA4IBAQCu6c2x61uMO85KSGOC+BfRcr0ZASdu0v3DaE9DtJHK3l11igInX/G492OdqsmVIiuyTzdDop5O8XZ7in/SI+B5ECMqMdIJU0udv+38g8StRluWjhdvoYmLerE299NLfRkpCR9KMmjnXGXtxZePhZc5VzED9AgdOmEHx7m7MYJglZbQ8n/vcHmpxdUmOfr/aKS+1YXKnmDYRxO1LmTfras83Z6hKGgrq1W+Ezip/ZDaWCd1Hd3Ua4OeB74xU1y0eNjYFx4JzzkiLxeRvtQBpOzhKkdIoZ7B6SNMlZgW3K8Zqz73mJUwe6Q0ujrgfvz/v2WYEM7wl93kMfEpXT4InU6S\"],"
			+ "\"x5t\":\"6qjRqBJqvfZPSAUWldoCDZNVMvs=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/CdVV3eCHr_fR6JFTJnGgOZ2z7ac.pem\","
			+ "\"x5t#S256\":\"OMgU1eipOv1dyK42k1C1ezyZ26uV4qCSz3wOnloMcFY=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"K8uO4fmAEmrLI2Cod9yp3W2SG6o\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"uDleqP8WpaGxEWVYrxRBXeh0_FOmJkGUv0qc76imHxZzhn4eurGJC9EzJf37G1hZE87KGeIqgOzSKPFcGuWf7Wx9e_qNqQmfzNwcPoM0xvz-UyfV6C_T2If2XZ3gVbKRzwO4NZB3awRBM5lfzjJnlzNLitapf6qaU34offr5Zi7AigZzYl60SXoT7OWMNReUjpQuwDnu6K72iRvZTFS6pzTnIg4j7Oe0ZPgFoJVItO1cLdmNr5pGVvb2JVmUwtxRtedzl_GgztB1urCsf9PBLBZfhy_db1iRkLF0dd-A22dqNyIyiEWz-4Jilzp9r7PEGeLFXFHX86leGv_kWES4Bw\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcVuIjANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMjExMTU0MTA4WhcNMjAwMzExMTYxMTA4WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWNW9aRTg1TjE2NHFYdXgzdnZBcnp2SDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALg5Xqj/FqWhsRFlWK8UQV3odPxTpiZBlL9KnO+oph8Wc4Z+HrqxiQvRMyX9+xtYWRPOyhniKoDs0ijxXBrln+1sfXv6jakJn8zcHD6DNMb8/lMn1egv09iH9l2d4FWykc8DuDWQd2sEQTOZX84yZ5czS4rWqX+qmlN+KH36+WYuwIoGc2JetEl6E+zljDUXlI6ULsA57uiu9okb2UxUuqc05yIOI+zntGT4BaCVSLTtXC3Zja+aRlb29iVZlMLcUbXnc5fxoM7QdbqwrH/TwSwWX4cv3W9YkZCxdHXfgNtnajciMohFs/uCYpc6fa+zxBnixVxR1/OpXhr/5FhEuAcCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUZMLzoReL7rNN7urUnB88o0xNfH8wDQYJKoZIhvcNAQELBQADggEBAFJ9c+ELW/51JZo/tvjQigP5XIzOqQforwljlIvEenDe4wq8FB8Kcd+74YRHh686b6w1wUIMCS8t5F61WwYhaJsgB9b/2Js675SEfpThgWfrSsXvbdjfUZABgS4XklT3iW0Y1cVW8thwo8P2cIv+iEXODQU/e4GRBBIvWrnBixiHWhb4sPvCmPQX3y2YqzvxSW8nkyDZ31EIiIdCcizLkdxn5g0iJhZja4Z7D5NPzo0vDuQ7QuxMXKeYB6R2zV00lMKOGiRdnSUWlFnKrwo3Y0jhsQXY+qFzDOirsFs1iGQWYWOQgHm2zzr3hkzLMhMg/x1V7BnItP9M6CYDeLlmwGs=\"],"
			+ "\"x5t\":\"uyEmIRy4Ba6FB1PpChskD-IsZKo=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/K8uO4fmAEmrLI2Cod9yp3W2SG6o.pem\","
			+ "\"x5t#S256\":\"-WlrWaAegf0HPJINmWMFXTXJZHdYAMPZUk0Qj-rH9BQ=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"K7jS47cy_K-FitpwF8VnXgwiP9Y\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"5IoSqbPbUycVnyWg4QQjuAe62kUUluFk4DkfbXcu47UdyIf7F8oQTs-5BE4QFdiPErIx8Cz6Aye_ATYCwLHTW-WAtkg-XwFfvZHnRZCFlx042lcjZbXW_iG4NkRBc2G9rctUucbcBGDTHo4cgl3kyCAjOPfmSyQVimyCTvr57AMACUrAKJcOqlU54HZZ4hXf7hP49cdbLPky9H0eKkHhjB6E8oOc7EJfPsmY6OpuSUE7uS82WY--Qwoxk2jys03-hZFgkxh3tLLZoBo0s85V7tT6FRlzo83khIvyjAt5CgZyX-li8zdeYR78CiKynk9xjSX_KpJNpXVyiPz1O9-qBQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcVuIzANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMjExMTU0NDE0WhcNMjAwMzExMTYxNDE0WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWNW9aRTg1TjE2NHFYdXgzdnZBcnp2SDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOSKEqmz21MnFZ8loOEEI7gHutpFFJbhZOA5H213LuO1HciH+xfKEE7PuQROEBXYjxKyMfAs+gMnvwE2AsCx01vlgLZIPl8BX72R50WQhZcdONpXI2W11v4huDZEQXNhva3LVLnG3ARg0x6OHIJd5MggIzj35kskFYpsgk76+ewDAAlKwCiXDqpVOeB2WeIV3+4T+PXHWyz5MvR9HipB4YwehPKDnOxCXz7JmOjqbklBO7kvNlmPvkMKMZNo8rNN/oWRYJMYd7Sy2aAaNLPOVe7U+hUZc6PN5ISL8owLeQoGcl/pYvM3XmEe/Aoisp5PcY0l/yqSTaV1coj89TvfqgUCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFGxoR3TvLfgOHoCkoiYWXkhYPuWpMA0GCSqGSIb3DQEBCwUAA4IBAQBEi9kUEVcgIOYGmFL2g047DBQkgdDu3+RLMgvFqHNS79T3QDDSsPx9aB0SRKvXAlKSDB1oC/fz3DOGxgsP/Ay2CQOX/YxN8FaVJRpgISGmFez1zbINP/voUIQb7HL3PKTkromwKNt/CNZcKJWCtsqxlIlk95aXaMsgfU070P4S+Aw8c9HyTR4Kv+608GwsflmhU2KoclWE+/WTIXE2uOPuhrVwdtDZ06/dRHkpnTrChrn6ZgMXEPqz5KOVq1Cvv28yMVt/2KeyFc0VB3aBsoqsHLBnMWbDBKc+UmGjrUfQeahxlX072XdC3Q4Fke85KDbZJ8WXU/2DwpYHg5pvG0MB\"],"
			+ "\"x5t\":\"wkez7koVWdSdbAeJqPx2jQFYNGs=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/K7jS47cy_K-FitpwF8VnXgwiP9Y.pem\","
			+ "\"x5t#S256\":\"4nZmjIX-4zl4kjuy3nC6K3nWERwOrMamLs89VNR0Y9U=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"4qUvYZZBUoXyfX6nfRlH4J8tcnA\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"2sLo9denMGW0Dd90k-PHD_SbgHzNZ6illubinCZN8wgX_C4JE6N5PYXLf2xg1bprlsYc7TEMN320ILtGZ-jO6QjAmKVLd23HxphQ0GokD5XI-1BtvkBKHb0E6APCosjqjRkYiB6UJDeHB1EkdUrcM4xSPqpnnpSDYtONc_u-4aN7x4QK9yUqQTpef_xr9t17MvXBAdszVfJuANlCe-7W_h-1CbPFyS8s4LB3Z4v9OtX6NsHGMaIdltg3Ts2_rIMt_42N_3ADuOPP_jCFBNTbZT44SVTOFqHiBSGtuThGZR6Iq2fSLDVP_8MDT_P8k_HkaG9zQixRvaEd6yzOfqgzRQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcVx8TANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMjI2MTcwODU2WhcNMjAwMzI2MTczODU2WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWTENWakdQWVBxSDdkNTNoNTBKU09qbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANrC6PXXpzBltA3fdJPjxw/0m4B8zWeopZbm4pwmTfMIF/wuCROjeT2Fy39sYNW6a5bGHO0xDDd9tCC7RmfozukIwJilS3dtx8aYUNBqJA+VyPtQbb5ASh29BOgDwqLI6o0ZGIgelCQ3hwdRJHVK3DOMUj6qZ56Ug2LTjXP7vuGje8eECvclKkE6Xn/8a/bdezL1wQHbM1XybgDZQnvu1v4ftQmzxckvLOCwd2eL/TrV+jbBxjGiHZbYN07Nv6yDLf+Njf9wA7jjz/4whQTU22U+OElUzhah4gUhrbk4RmUeiKtn0iw1T//DA0/z/JPx5Ghvc0IsUb2hHesszn6oM0UCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQU+yX+74tKXzf+yqUoStupfg5jWGMwDQYJKoZIhvcNAQELBQADggEBABowIDhJAZgp83fg4A7ZypthgYIYMKlAJ5HVZGqLEAxkP0wMRH52oKVZtip+1AE6eV9NO9Ql6q9JkA6FTQOBlPxft3/eHaJyfedwvzkTRXuik9xpqqKtTetGN7Gdg6rImf4jG7fBNuTGveOZoJGImzhawhTwPdxpFoLLXnJSWM0/TMvA8+NejjXEX2Abg+KBp1Z0/+SbK4A52eliSlgABxaCE7GMx5ecYUBFIirx710S3k4J7ltDbsk2Kl3nDhgt4rOia+luNMdKOjF1/pOoOXntHNpCJgj9JvjUgkS+moF7oIwanfjYPie01oaQhKBs1fZpInJ0Xal8+FHFJIabG+8=\"],"
			+ "\"x5t\":\"6hvfbSIgzCOHgJHM_HohHs9Reks=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/4qUvYZZBUoXyfX6nfRlH4J8tcnA.pem\","
			+ "\"x5t#S256\":\"3KUs0QkT4l8DGh3cb3jT04va08wPa7IkSyEf4lUSSUU=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"2Xl1YufKDRZdBCU6as6vv5pBimk\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"v1-bY4zJ5PYHH7MRubnT5W9s1iBYtzCKqQ9pay1LAAQO0Mcc0ISESFmLy3wQJPF2HCFHXInDzLBqaw-Bt5ISyeIZvqWhXJI9Pz3g7CwjqnC9RxAY6gCNR4130akA2Fn237S5O_e33EnmHSrZTsTm7EM3x-avJXCrKr6ESmIYSWl7OSaAV3nqt1zO75pPUCnH6X_6C2tfhm7okJ3XMCF3cEf2SO7hDdV4KiZIQ3ZGbIgQxgFwV71Mrx3FJ1QIeMXj8qNHXJ5lt4EDS1NK-AQMaBrkbXcgsIbg-V40rorMh4PJjw2V9T57I5Tw11jikRkz0isrnDmPd2Nc26TF5ZfXow\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcV2ijANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMzEzMTgwMTUwWhcNMjAwNDEzMTgzMTUwWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWb0VJYlVzWHBYSk83bTFTUnhMRTYxczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL9fm2OMyeT2Bx+zEbm50+VvbNYgWLcwiqkPaWstSwAEDtDHHNCEhEhZi8t8ECTxdhwhR1yJw8ywamsPgbeSEsniGb6loVySPT894OwsI6pwvUcQGOoAjUeNd9GpANhZ9t+0uTv3t9xJ5h0q2U7E5uxDN8fmryVwqyq+hEpiGElpezkmgFd56rdczu+aT1Apx+l/+gtrX4Zu6JCd1zAhd3BH9kju4Q3VeComSEN2RmyIEMYBcFe9TK8dxSdUCHjF4/KjR1yeZbeBA0tTSvgEDGga5G13ILCG4PleNK6KzIeDyY8NlfU+eyOU8NdY4pEZM9IrK5w5j3djXNukxeWX16MCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFLPcSIwdOIxVNGkaYVieRWSPjNjAMA0GCSqGSIb3DQEBCwUAA4IBAQCU8yzsb5j3ZhHsJNdWj2850wTvpAuAaq501j5AgaShHzBPyFZtCyPxOn0ciFmJqxEioi+pr9F6tTeSLUkLMIVyZQ/cLaDZcD8gZxLAsJcJSw58BDVyaXviZYfsyVRdPaozUJDF3iSyKkMVJHDydnD4lHpDRK8o48NptE9MUaBjRezrj3CwYLjNVyipVwxw2st67kk3hXuJrzNd0zfWSp+CzHXoQDFO+WSJJE4mJOlZu3erDo69EBXCkr4I19qRd7kCzfFgE3rF4AY0XlLtRVg4JzDXqc3oyWEGDPkYGkXrMKTFndDm1ypNcTRqP0605FJMbA1UJmWHzGFFZPddSp/9\"],"
			+ "\"x5t\":\"izmLI-YHYbFqTtBkpdVAmhO2oIo=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/2Xl1YufKDRZdBCU6as6vv5pBimk.pem\","
			+ "\"x5t#S256\":\"7K9jjx7XXbUst8DDtENp-SvybI-00H3WhM3LHWLWiZc=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"fT5RKvHEMzT-dUFpRD4fFRxX5Fw\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"vVJuEt2_rB0WUPC__x7u-I4wyI1XQlSwq7_sLR_ebdLTC8oU4td9H67F25J8Qw5AiyyICv84_7CfiZ7nopSt0DDkgBCZ9Ybv0TRW3j6JorkjUncpQQZJkMLDNOnSJHERF3yGve6IWGN3S4RbCAhfFi7jz859Q1Selu8APr1uWDXrraAKpMl8TyRB8eoKmE8oSmgHWcEtVwVHum4eedoAOE7Hr1npHfbotxNvt-dd2bJ6EwnJiZdHEdjKg5q2cQHwyQ967jrfp2rjClHLwV9lYYxs5OmNuWqAQUBA0E8IpexUK7UTsPc0ApH7CgvytuEzRuMkRossmci4mpeKY49kDQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcV2izANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwMzEzMTgwNDA5WhcNMjAwNDEzMTgzNDA5WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWb0VJYlVzWHBYSk83bTFTUnhMRTYxczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL1SbhLdv6wdFlDwv/8e7viOMMiNV0JUsKu/7C0f3m3S0wvKFOLXfR+uxduSfEMOQIssiAr/OP+wn4me56KUrdAw5IAQmfWG79E0Vt4+iaK5I1J3KUEGSZDCwzTp0iRxERd8hr3uiFhjd0uEWwgIXxYu48/OfUNUnpbvAD69blg1662gCqTJfE8kQfHqCphPKEpoB1nBLVcFR7puHnnaADhOx69Z6R326LcTb7fnXdmyehMJyYmXRxHYyoOatnEB8MkPeu4636dq4wpRy8FfZWGMbOTpjblqgEFAQNBPCKXsVCu1E7D3NAKR+woL8rbhM0bjJEaLLJnIuJqXimOPZA0CAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUHN24Vw9lanP3aEL8TZiHEvZnN2QwDQYJKoZIhvcNAQELBQADggEBAC15NKuuNmk04Uf5b5Kko8HDjKiSuP+3zC7InbqUN3H0ybNcQATEXbGbyeuPrre3bI8vQeMVY748pa2SAUkHZ1DFSS1JUaKDOQzOz/vJhNNIgHAczkpRW7ZG43YpGz3MD8KcZTjtcsKFu93PHbHooYZz9DpxR/akB91gQH8SB0fJfHOh2Xv1XqmXKq/PRnA7Ob7smXjfkfRp6XCt3+nGPxo7xD6yT3YhQ+WIxSL7tjXaZTyzkahNUJxaSZh2Px4IPS/edwrgUlZ39IUMvRoSLfkMoDKd40N3r4JkM9ARQh0pkps605S2owgb39U+Lb7brzhmfBsvmeS+GEdXv5qFU8o=\"],"
			+ "\"x5t\":\"E2B8CeBaX_78fNs7lnmj3hBMrDE=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/fT5RKvHEMzT-dUFpRD4fFRxX5Fw.pem\","
			+ "\"x5t#S256\":\"eXwq_7yKq3c8_fehlbzkKq4usfKT0G8I9cmrfDBpywE=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"-SnmnvqTraId6FV27XfHt4ky8Qs\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"roJkawWjpGpss3cmJW9OKbAeUJEg4kp-U1PmZsIu4ip7s214qbUCXRplAHbBuWzebYMflLVxfDa8JSHflVOWNJyXkXY22U9DJZL4VuQU8yoKUZRWtwvTlxo-qEpMcmbLd1WZTIvF7n-iXpOoDXs0Bun_iq5cwanVcrWmwOJ_hz2LeZiSNMWPwqSYVoPhP5xm-4iABnCKPoz-vF2MN6sGt7VquiQi5m4kYcgqjrH26PrIBYaV7InbJefxXpv6A5dYSaEKyhqvgFZWaxtN9CbMaeQXrhNskRgmipD54P1rWmcSQR466ZYvY6MefJVEipsam-ho8RWo7NDAVLLhpsdktQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWVgTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzE4MTAwMzQ2WhcNMjAwODE4MTAzMzQ2WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWakZrRmpjVVFOaWgwNG5OamZ4aXJ3TDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK6CZGsFo6RqbLN3JiVvTimwHlCRIOJKflNT5mbCLuIqe7NteKm1Al0aZQB2wbls3m2DH5S1cXw2vCUh35VTljScl5F2NtlPQyWS+FbkFPMqClGUVrcL05caPqhKTHJmy3dVmUyLxe5/ol6TqA17NAbp/4quXMGp1XK1psDif4c9i3mYkjTFj8KkmFaD4T+cZvuIgAZwij6M/rxdjDerBre1arokIuZuJGHIKo6x9uj6yAWGleyJ2yXn8V6b+gOXWEmhCsoar4BWVmsbTfQmzGnkF64TbJEYJoqQ+eD9a1pnEkEeOumWL2OjHnyVRIqbGpvoaPEVqOzQwFSy4abHZLUCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUpHT2orb8Ld7g3FgG8GCkZl4kX44wDQYJKoZIhvcNAQELBQADggEBADSWMXQGyBC/zG5ltGK2DcSnmu3toTnqnx4pi5uUsxpidz9II91w4JPLbMHHwFEfgM88cQMWvbtpWDroXKtGkI+giOz4ewlu9s7vS9AGTUEGPZQRGkaKUxyEbccQL0Ah378MdC7dObsRpc2xC8N5Za9rc0gYRtiyqNYLVA5yursnAEZcm0RuftHZjMbLtY/G6Roc1JHQcPN+nFF86tsIcLyW7f6lqrkg+8n9Owg/SQxxhpcIpaFXGqnuK8mIYGgg2NJb2dHZnqyyFeQ5pTP8rTsNTqg6a9Dd9PmWpyqSoUNnqbbBvCvMqnDxCwLBgoKCgtK85pBhGFT6pMUOEARhIdc=\"],"
			+ "\"x5t\":\"ZBvX6h8r7roy5d36KSJixARH-ZA=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/-SnmnvqTraId6FV27XfHt4ky8Qs.pem\","
			+ "\"x5t#S256\":\"8ZtUuuopYr_2u7JNxr8v2cNPOP9ofoXQhpdi5Sfi5gc=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"b8mFlHWO76U9XJE9hWsW8PWbikc\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"mdGaYZbc-hSa8PLW2BKZh6BV9eJNm_CNdmYlRtci9tFLYUANkjTMev__gf0bh7WNoz2mLeb9aF1_qObsNQYBA2cTMccJCqVTClfLVkQr5JjxUB6iPQtxqjqbGod1jlqpk2hM9e7h_dz-zlniMoNzm6V5Xk-ZiBLPrnh3qAVixB0bohqb0-xd1vjntqtPSLruZxJd28mqlRd0sDRiRWbEaHrPh037zxzCMvHGMNhubWLR9czwvRd1HS98qykZpryt6DYzZgqLUU9qxXmQXx56Zj2r7bd103OQaLCozb4E__Q7CnWbr_kmKHIbjs1mma04lF4bR99FYpJ-xqL4n3TygQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWVgjANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzE4MTAwNjUwWhcNMjAwODE4MTAzNjUwWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWakZrRmpjVVFOaWgwNG5OamZ4aXJ3TDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJnRmmGW3PoUmvDy1tgSmYegVfXiTZvwjXZmJUbXIvbRS2FADZI0zHr//4H9G4e1jaM9pi3m/Whdf6jm7DUGAQNnEzHHCQqlUwpXy1ZEK+SY8VAeoj0Lcao6mxqHdY5aqZNoTPXu4f3c/s5Z4jKDc5uleV5PmYgSz654d6gFYsQdG6Iam9PsXdb457arT0i67mcSXdvJqpUXdLA0YkVmxGh6z4dN+88cwjLxxjDYbm1i0fXM8L0XdR0vfKspGaa8reg2M2YKi1FPasV5kF8eemY9q+23ddNzkGiwqM2+BP/0Owp1m6/5JihyG47NZpmtOJReG0ffRWKSfsai+J908oECAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFFnO0vzKvi3gBrBZM4dHBwbF0PdLMA0GCSqGSIb3DQEBCwUAA4IBAQApWl8+IXIUnr0g9ngGdgVpNXTp7XV6BG7sKsMTKVNPSG5ktek2jpvsonXTeBSs4QKwQMkM380EQbIu3r6qrYQZ5HYGX3m6M0X3ZeSRtuaOhailvz+XSqUxEsbUTTQx0jpvx8lCZsZSJlqQh5KfT86hyoXlA54YUr8Y0zpTZH/9qsH379ENftSHZss3NqZkZnHHcHHCxLXlxQZV39RqKio778X3Vv8sSiq3XOA+DSZuoCo/O+JcHJkRzPYFlwz2Sgc1yWoJkRqnWYfwPwjvQJlm91WTrsoR3PchtlcZacyPFTcAWrwGy3Zkgx25fp1qkf6G2kojFMhhQXpdf6YR8H+G\"],"
			+ "\"x5t\":\"1bgdury1XtgN1uAUVikKT8IGrwM=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/b8mFlHWO76U9XJE9hWsW8PWbikc.pem\","
			+ "\"x5t#S256\":\"wDeoB12Min_KhCxdXqBQhCEB7tn5ldQUkS4vY0XuKko=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"nISnDxpPi2TAOYcgUxPOIIDKHEE\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"01mSSa85QpipZ9Xg3c9EWrrDs6o7477EVwb32HnukQPywYAXOE9X0XN_UH8YFB6xwI3ZC7q4VzkiMf_Aq0lieQZqO7YggFRSWBv0SWfbq9FZj1pzzNuPVfs7jmphe0yMiixILd6BRN9L9zD_9iXRI8eJQbsehF8piv0ZqvL4dCpDdp2Te8dbguaPHwN_6Bdnjss9TzLcKZflZ43ICEtTFvjEyHsQp4TYurS03Qvg3p3uFYa6KVuFVVWI3S7E_SA1m66n9UMVODovKAm6G4eJM55cOr2cz2caAsZojJrMABdWZhz9wriGlD22S_jKwBftc85KEt6_K66sjh-JW_36kQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFxTCCBK2gAwIBAgIEWcWVxTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzE5MTQyNTA4WhcNMjAwODE5MTQ1NTA4WjBgMQswCQYDVQQGEwJHQjEZMBcGA1UEChMQVmlyZ2luIE1vbmV5IFBMQzEZMBcGA1UEYRMQUFNER0ItRkNBLTUwMzk2MzEbMBkGA1UEAxMSMDAxNTgwMDAwMVpFYzF6QUFEMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA01mSSa85QpipZ9Xg3c9EWrrDs6o7477EVwb32HnukQPywYAXOE9X0XN/UH8YFB6xwI3ZC7q4VzkiMf/Aq0lieQZqO7YggFRSWBv0SWfbq9FZj1pzzNuPVfs7jmphe0yMiixILd6BRN9L9zD/9iXRI8eJQbsehF8piv0ZqvL4dCpDdp2Te8dbguaPHwN/6Bdnjss9TzLcKZflZ43ICEtTFvjEyHsQp4TYurS03Qvg3p3uFYa6KVuFVVWI3S7E/SA1m66n9UMVODovKAm6G4eJM55cOr2cz2caAsZojJrMABdWZhz9wriGlD22S/jKwBftc85KEt6/K66sjh+JW/36kQIDAQABo4ICkjCCAo4wDgYDVR0PAQH/BAQDAgeAMIGLBggrBgEFBQcBAwR/MH0wEwYGBACORgEGMAkGBwQAjkYBBgMwZgYGBACBmCcCMFwwNTAzBgcEAIGYJwECDAZQU1BfUEkGBwQAgZgnAQMMBlBTUF9BSQYHBACBmCcBBAwGUFNQX0lDDBtGaW5hbmNpYWwgQ29uZHVjdCBBdXRob3JpdHkMBkdCLUZDQTAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQU0sbuj3SbT0QIdJHShgHl8g9ePOMwDQYJKoZIhvcNAQELBQADggEBADhg3b/FLl5ujE6tjpCxU29wOWpwEmSb8NZ7OceRkq306Y95gaJ5J8+ED1xbeZQOWoVwBFLUW1AS3y67icSU4VnQzGnGCLolMmwujvpaxiK8chVDAce9uu3WTynIDTNJ5griX8rsm6F6JAkSDuOI3EHGZqe7vCEnXotF/U2kpQ4mK7ASOrzpOohsOQlNzCw5Sq9R2gkU23HbVNl4NqHQe7vkOu8Y8z85TREKloJmkDgQGgoprDKYZZfsRCARauCRv7vkP3K0+qPjyUzRHM7IfJcq/GrJv95FRy/9rEbP2hECs88HtMlL38ctDcdctLL6DcByjWb5IL3uT30ovWQ8qvE=\"],"
			+ "\"x5t\":\"fa09RO8IaAOZiCd174FS3e8Tly4=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/nISnDxpPi2TAOYcgUxPOIIDKHEE.pem\","
			+ "\"x5t#S256\":\"-SXGvRyIxBxoBExeNmbdbL7lDEnXLMG-o7RyhA8X8JU=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"RwfZmPrx76e32CEvvFgoGnc8p8Y\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"op7IF4H4GtldRAq51vRQZD2TMdhNBvH6lk8IOWYRInmV7qv-w1HCV3qO3miD5Fvj0o-5H1o_kTeFWY69KJz5piV77623KUwPupwJ5skryziDViJmyNjkyWauRlD4TqZ1tHexECYYKTEZ2z_l3-kbhibkELiQPlojRoQeqAylWRhQBWUxzbn70NtZ5NT96Ve45DF8MwVEtW4wj2nUcu50ilzo3zTZg9kL1TMsJEkc00eYI8YRmYSRv2S8tbTlfnpCCMN2ifIoYxvAb9ZfdOYH2pEHx5_6ptlNr7CtVBrbJWwr8YSdBVhrEYCi55bDJl95qYW4NusCU5yu8KL_kTSrMw\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFujCCBKKgAwIBAgIEWcWWTDANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzIyMTIwOTQ3WhcNMjAwODIyMTIzOTQ3WjBgMQswCQYDVQQGEwJHQjEZMBcGA1UEChMQVmlyZ2luIE1vbmV5IFBMQzEZMBcGA1UEYRMQUFNER0ItRkNBLTUwMzk2MzEbMBkGA1UEAxMSMDAxNTgwMDAwMVpFYzF6QUFEMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAop7IF4H4GtldRAq51vRQZD2TMdhNBvH6lk8IOWYRInmV7qv+w1HCV3qO3miD5Fvj0o+5H1o/kTeFWY69KJz5piV77623KUwPupwJ5skryziDViJmyNjkyWauRlD4TqZ1tHexECYYKTEZ2z/l3+kbhibkELiQPlojRoQeqAylWRhQBWUxzbn70NtZ5NT96Ve45DF8MwVEtW4wj2nUcu50ilzo3zTZg9kL1TMsJEkc00eYI8YRmYSRv2S8tbTlfnpCCMN2ifIoYxvAb9ZfdOYH2pEHx5/6ptlNr7CtVBrbJWwr8YSdBVhrEYCi55bDJl95qYW4NusCU5yu8KL/kTSrMwIDAQABo4IChzCCAoMwDgYDVR0PAQH/BAQDAgbAMIGLBggrBgEFBQcBAwR/MH0wEwYGBACORgEGMAkGBwQAjkYBBgIwZgYGBACBmCcCMFwwNTAzBgcEAIGYJwECDAZQU1BfUEkGBwQAgZgnAQMMBlBTUF9BSQYHBACBmCcBBAwGUFNQX0lDDBtGaW5hbmNpYWwgQ29uZHVjdCBBdXRob3JpdHkMBkdCLUZDQTAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFEqSGwyRfgBJoQWTnr53//6T6srOMA0GCSqGSIb3DQEBCwUAA4IBAQCT9zLuFUL+XgcIIz8SK/ajUwTNzlrvaTxIrF5C+AABFTj0BB0FdXIwjWAmrC0iNjoc3GOxJSUk4xc5U9wc4WILnL20HjTcToo4ZdEXZ5pnKD6EAQA9oVrKf80CQ9DVC/q4pvZQ/ThGDOQWX0qRqtEz9yZduKyo9Nfp7E/hpsZqu5rAf8TKy5uWgMwuc7g8I6DOW6wWfhJY1oe8efHFSSO7spvoO0HFIo/EjWhPvnfxQYal2MfQmoNDqAh+txGtwCj5KIzb/YeKF0AU+iktvqCbkpixfNzIURRVw1DQLiEOyCH8jOXdOiTw03expGmbfXgNcSbf0OOFZkzYPZvOt2iJ\"],"
			+ "\"x5t\":\"7SKLrcPXkhDtOutTDncq6D5oFZ0=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/RwfZmPrx76e32CEvvFgoGnc8p8Y.pem\","
			+ "\"x5t#S256\":\"tVMU4V4WmzU8Hig95u6yt9YdCcl-7Az8QTbSWRjHmI0=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"VOUN-729-z-IGz4Mwl3SPnoQUBU\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"0HY4zDeAs0Ep31MKNdZE42azAL8xbWqNNeOifsJ4qHua8lKwQTcSqDysQIykXa4E0Lb6p8yrAfpNx8jiBseXe6i4Gw_ZeH2-1S4IV5QDC1EpCg0kLjTcwmg29bd9hvophwEWJ0d11ZOBIXm7nVEDqTEQX4VcEMZcAimNavZm_ZSw9HVYnaywqPWx3fauk4Qw0yv6MPq-VjRE061sY_gFt4LOs4NbgAg2nfTqoK12nvZn0lu4qw74rCl5cGPO-wAIOuDgaJYyFfO2psFuUrKmiBOnVrl5vdUjjax8ngbht9lMZDnxb3cDJ1BeI2FSJ0GKfVujN3lQnYAIgB_BKqtljQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWW6zANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzI0MTMzMTM3WhcNMjAwODI0MTQwMTM3WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWaFBSMXF5cmFHSnM1RVNEMGFpYm83MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANB2OMw3gLNBKd9TCjXWRONmswC/MW1qjTXjon7CeKh7mvJSsEE3Eqg8rECMpF2uBNC2+qfMqwH6TcfI4gbHl3uouBsP2Xh9vtUuCFeUAwtRKQoNJC403MJoNvW3fYb6KYcBFidHddWTgSF5u51RA6kxEF+FXBDGXAIpjWr2Zv2UsPR1WJ2ssKj1sd32rpOEMNMr+jD6vlY0RNOtbGP4BbeCzrODW4AINp306qCtdp72Z9JbuKsO+KwpeXBjzvsACDrg4GiWMhXztqbBblKypogTp1a5eb3VI42sfJ4G4bfZTGQ58W93AydQXiNhUidBin1bozd5UJ2ACIAfwSqrZY0CAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUohzwkabA+QZ0KONa0J60mPSHImAwDQYJKoZIhvcNAQELBQADggEBAIrDkLp2WKP+0SAojvB4kcaYqwgqntkoSNAW6XemN8JibLyKHNUypfIFaWGEC9JdxfP0d59FEMXjqgILVm4Sm19q5QwKySDjvgwRuR6VBf4+jhaRMHRHbAsaBskvGYs/OomMXerZ6T4PjchYjIP1u4/aP0LnK4wP02bBZG2YSXhWloBM6Q3gPQKXXkCca4A9tkPiWlBt9RJ4csBBE7oQq2JikPqGsXCSBEIPT9CTKWV8qRuyO78z7u6Z7lfTzo2HwoeeJsIOMEfq8gLfOSvGm6stnuEtHNgNjtqwyY8gy21ufmBuUXAcxloOAMVCnBdHq9vErYkMkE5v8QLfYqAP4XQ=\"],"
			+ "\"x5t\":\"LrTsehqqsn7tcGH-W_owYG26b0g=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/VOUN-729-z-IGz4Mwl3SPnoQUBU.pem\","
			+ "\"x5t#S256\":\"76w_0GJQHFJpQpIBUHMAZ2JGeH_ArcgijXnwyty6YzA=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"ZZmRIY4H5_39MLTyG4vOQ6id1h4\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"0L2Fam6hUR1PU7Cuw023AggwlKh16oPWq4opX4dtDRkK82bySDlI1UM72B243NurTuzAbByECYYAt7wsegKm5Qdf5jIEqtrz-BMtX8Zj6LjJ2vDE3WzO-9xxIlDGdwnaziN3zuiK8WX3xPEDieww_VhgqS-93r41fcQGwSfF1naMYHm_dVwPd3y018B4nczl4TVDvphIxHGCXrLNfLTCIQuDaUoKcE1fpbh2AtzBhC4rbeuZyxN-7zPEgKodrn_wgv8De2jj12W3GiAXZnwx6sojrLU6vxTdQivwxOHbupeYQiSFnPXqggFkbBf86PeXdYoGdQHqr0kyH1XHGhD99Q\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWW7DANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNzI0MTMzMzIwWhcNMjAwODI0MTQwMzIwWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWaFBSMXF5cmFHSnM1RVNEMGFpYm83MjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANC9hWpuoVEdT1OwrsNNtwIIMJSodeqD1quKKV+HbQ0ZCvNm8kg5SNVDO9gduNzbq07swGwchAmGALe8LHoCpuUHX+YyBKra8/gTLV/GY+i4ydrwxN1szvvccSJQxncJ2s4jd87oivFl98TxA4nsMP1YYKkvvd6+NX3EBsEnxdZ2jGB5v3VcD3d8tNfAeJ3M5eE1Q76YSMRxgl6yzXy0wiELg2lKCnBNX6W4dgLcwYQuK23rmcsTfu8zxICqHa5/8IL/A3to49dltxogF2Z8MerKI6y1Or8U3UIr8MTh27qXmEIkhZz16oIBZGwX/Oj3l3WKBnUB6q9JMh9VxxoQ/fUCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFDG3Y7jEtpkRX5DdUX+7B7bbYZlCMA0GCSqGSIb3DQEBCwUAA4IBAQBA/ptb7B1vt9+nRB42o+0Fw9RK42pfR4FxD5Bcy3OzR6scH6eS3c/UJZv2HTFpLX59qNfjPH9hh2tGt3dB6dxZ7lB3wlQ4AVb0jd/Hp/KfgLoSKntrTyNqeVDv1iTyemhsVYFHhwbAT8NDlqDHq3HdYsfq51h51uvNzCRSu2+yp8aaKIVXiTfPJgoTgM2OESlWnFtrrZKbpECVeEr0pMsB+95ta0EqYyNIvVa9wkzkCJRxEvl278Fr+g45oiCCyfMkdofucKp6opDvHQKGPlji53ZDlM+BPVT1LlY+k0lLdBsMcCE6IkTb/YPRJ3bUIWzjH6w71yl/BVXsyJ4dxHtu\"],"
			+ "\"x5t\":\"Q_V7oXEOtSQPQK-uSg-8A_i0r-c=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/ZZmRIY4H5_39MLTyG4vOQ6id1h4.pem\","
			+ "\"x5t#S256\":\"8M_cIwacgY6wGxj84sLh6GG0gLDpm4DN6n2-FF0hNIY=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"Dlqifj37FzndmA3VlZUvWRofbm8\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"7uJSYQLG2QMBkpamS1JFW_JMhVfrJKpKIKIT-oytN5y95_xkGoAB5mW-8O4LeDL8MMviUPndtgmHi7hLmU2Em7Y6qOrjfimxlyUL1DLBtYDbFd6L5RsboKz-90ID_ZgvmIJvp9SYwgsA5bchpalJDsADsrQSwLDWnqXptAo7NYvnbtX508YSG9Ax-Xt5wwGiyHXutHQFGJBwT14gMYxhsQf2Z117CC1uhrrWBVrFh0t6PJg4cEDE1UAeYVcdV_hfxbIipMEbmTk1YYetvtxiDeqfTe9MHkrRCLSisaScWYKmsLuFnnk-1LUm3_w7L2WlpAgkBm3fa01sCu0HRkHB_Q\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWYzzANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMDkzMTQ2WhcNMjAwOTAyMTAwMTQ2WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWa1NlcEFZbWpIRlhxSjFORTBqVG0yVzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAO7iUmECxtkDAZKWpktSRVvyTIVX6ySqSiCiE/qMrTecvef8ZBqAAeZlvvDuC3gy/DDL4lD53bYJh4u4S5lNhJu2Oqjq434psZclC9QywbWA2xXei+UbG6Cs/vdCA/2YL5iCb6fUmMILAOW3IaWpSQ7AA7K0EsCw1p6l6bQKOzWL527V+dPGEhvQMfl7ecMBosh17rR0BRiQcE9eIDGMYbEH9mddewgtboa61gVaxYdLejyYOHBAxNVAHmFXHVf4X8WyIqTBG5k5NWGHrb7cYg3qn03vTB5K0Qi0orGknFmCprC7hZ55PtS1Jt/8Oy9lpaQIJAZt32tNbArtB0ZBwf0CAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUwf9wtZO2SLlsl0SUpzmxi4RAOQIwDQYJKoZIhvcNAQELBQADggEBAFM2cR6T76edKLsMAlsL5+Vjn3+pwfjeRoo8Xitu0AsT6FOHmDrliPbaaY5+gQXidWh2YaEPGkolSKE86/VnCjH6vrdWJjIfHfP+eW5FFWh2jSWg6BdMUzxVmeI1TSa/nJCJ5tN/9KiSkOwi4zB4FXRBtqwHW4fkYaEheueo6Gz/duGsLpyD1n+TKd5CXcYFlk38CHrGpYSp8itXBq4lO6QfJZ6SHx/iFlpfS4oxTxIq+LMWjnBUW86BTWKgek+yzTEqPGWCvDAlfU2vsEPMzIF7TtbZTgxx6Eu0hi4eG2oPvWQkAEf5+Envgu3k12hCiqgBoRfomVY9MXgdHjUVWyw=\"],"
			+ "\"x5t\":\"xr496ZsEM3zRNblN0-KQuV1N8mU=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/Dlqifj37FzndmA3VlZUvWRofbm8.pem\","
			+ "\"x5t#S256\":\"4Kj1Qp512y3hE0l4ahfSe3hoMXnvnOSKzxNeAwLcu2A=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"q4Mx-j45OWshLh3IPXSHMLLkrZ8\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"weg1vXt2AKtEiElp7lMmW3C7SH5ghK5XMZuTz2Az1lhQPObZlLF6ajT-naAL0BSvDatrqzu-9_rFTEP5Nuh12rVXBi3bla_Z1RbjVGpipf3SwMiHtx6ZbCw1Xl0al0K6c4gkEBuHctlD9-OxfUW0zOPcp3Z14ewArVlEHdC_08hxU_3pjHsBg5U-PWZhCcWYTBV1N1jA5auUWY7MJ26O8ANqo3m0LhLbvCq91vy1v78VrglYeaHrL7a_d7lcxnxHyXghKms8xBIhTgqeBfYXXNt1j9n2ZqeqqlyWIR5ovr8ln6lJxfFE-wnpgZAOSkvLEpiCINCzxR6nbmKwUeleZw\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWY0DANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMDkzMzU1WhcNMjAwOTAyMTAwMzU1WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWa1NlcEFZbWpIRlhxSjFORTBqVG0yVzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMHoNb17dgCrRIhJae5TJltwu0h+YISuVzGbk89gM9ZYUDzm2ZSxemo0/p2gC9AUrw2ra6s7vvf6xUxD+Tboddq1VwYt25Wv2dUW41RqYqX90sDIh7cemWwsNV5dGpdCunOIJBAbh3LZQ/fjsX1FtMzj3Kd2deHsAK1ZRB3Qv9PIcVP96Yx7AYOVPj1mYQnFmEwVdTdYwOWrlFmOzCdujvADaqN5tC4S27wqvdb8tb+/Fa4JWHmh6y+2v3e5XMZ8R8l4ISprPMQSIU4KngX2F1zbdY/Z9manqqpcliEeaL6/JZ+pScXxRPsJ6YGQDkpLyxKYgiDQs8Uep25isFHpXmcCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFIahkmFHQ9IN47zhn2kVguRZ6V/rMA0GCSqGSIb3DQEBCwUAA4IBAQBESP01m1g3Fwm9geZTnWMiFlAiU3OEjAcxBhEHoy26g+XjBL5bWivyVhq1EQ/W0x++uKh5XhkvOIaDmdSatFGZDiDlz/1zk1pnfPG/osiy4rqjq8XdNoPo7ViNfJrz0A4iDRyMhvkgUAKutqkWt0LashLsw+5PudDb8jAk+h/YhGOpGXv/JLd41BTR9K5L3QKRxbxE0HWWCN2p9mL4TFEFB7q/FpwHVaY1a6ub2jlRqE9q6FmQUNxeH7GboZAxDrNgIwN4Aq6uV7VzXufSJY0+MqPchxAptpTy6c/rLDSgO/vcvzP3z71Kx73B0CRteeylgUIw/elL0Yj+8Lp2SLkU\"],"
			+ "\"x5t\":\"s20oULfh-c9zd8AHRLLS-qdOHqk=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/q4Mx-j45OWshLh3IPXSHMLLkrZ8.pem\","
			+ "\"x5t#S256\":\"VDe2XIMWhv1ylVKVxi52p1uBqi4fvcLfldk4PCMWFho=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"o3V0DnoTPWmoPP41ViIoM_9rhn8\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"0oW7NviT5xozszrXEGggc1vada5-jW2r7EpTeRoYkPQGIkOFl6smAWPqGm8VmrqCIDoGbnfjbfUSMbA8rqdOqMDSxloxoOwTWrhNf0FE09tAw58fNX7dgxpC5D6d29HIt2k5Xab_ZlolORlsurXRXYPcMsA9K8qm5AI05l2Sro2E5RsyZOSrhM632fNTEpHI3ExQxW4Q2cq_AvL06S8sweQ_8YGyS8kp_rV3oFh-MVQPk-zJcZpQQAexAJKpV4jbpHZn4Gje8-W3xvQdRHHKEWGkl1K2lCR3ONz0peFEfLWMV1VbjOxGlWh1OVmYFK1Os8E5OUzWtqx3CATax3NXhQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWY0jANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMDk0NDU5WhcNMjAwOTAyMTAxNDU5WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWMmJuZUJ3cmhHS0ZoWVoxN3dhRWtyRzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANKFuzb4k+caM7M61xBoIHNb2nWufo1tq+xKU3kaGJD0BiJDhZerJgFj6hpvFZq6giA6Bm534231EjGwPK6nTqjA0sZaMaDsE1q4TX9BRNPbQMOfHzV+3YMaQuQ+ndvRyLdpOV2m/2ZaJTkZbLq10V2D3DLAPSvKpuQCNOZdkq6NhOUbMmTkq4TOt9nzUxKRyNxMUMVuENnKvwLy9OkvLMHkP/GBskvJKf61d6BYfjFUD5PsyXGaUEAHsQCSqVeI26R2Z+Bo3vPlt8b0HURxyhFhpJdStpQkdzjc9KXhRHy1jFdVW4zsRpVodTlZmBStTrPBOTlM1rasdwgE2sdzV4UCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUPv7vaqI1SoLXsvmKD58tUjjH9iMwDQYJKoZIhvcNAQELBQADggEBAA3FPzK9+XgFfT31pQ8tqlh8Lpok1vv1U44LBc4Furn5Rrq2VIgq1Rh0FOu9o1o7ddcb5AOXWIPdbRXD/Ej64tzmyphxtwhL/58pcgkRu5CxXVUlJxHWTB1D5ZyX1JV4AJcR+KWyWp+x6jbKIVOtlBLx7bDg3xfsd+RDEmeb+Pzwa0VB0d3ReBdJfjmxET/YAT/wcgeRn+QwtQ24wZI4Ml6DJDnQBkiD2OPi9pAA3DsoSwyOVLvCfDFMB2NmsnwWJqP8lDjldQ7MhHgNqcgi+F+9zgFh2Rj44RKZxTc7XLtAyZ4IORsKbD3byCk9S1c+lLWdRTdiHRfgrJeSGi80LX8=\"],"
			+ "\"x5t\":\"bLJrvl9-2796p5AvsVaRvZ5R1KE=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/o3V0DnoTPWmoPP41ViIoM_9rhn8.pem\","
			+ "\"x5t#S256\":\"qloPqDCpKEnnXvlSB0Kog0B8Y3tpA8CxeUeGNFuBsWg=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"V3qpm3lQ1jFNMZFmuhE0bF0dbw0\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"r1n6veoLTQvGWaazzjrSs-S7Hz1A2mOQBV40i31SeYuhSP1tCS3m4NIQDnRYvefRC0bDiUENHNp547um2pVoFjDtacIpkIfiZAEX3hfOfDa4H2JdXtdCGkBvZedxTppoiPuNlajTAz-pYeY501x8YOKlIO0eJN8cp6iB2vceJ0S5w3Nx0siRPZGp9dYYzj-62M7M3RYOxL2kcqqzWOM-lAHbdL7RhC2lnl2oDpvXPV3yfPz0U-rmZe7vRzg7zcthcAANpHz18jcY8hNj7R9e4_iwkbdRIq3YqpWJLe1HM-ESRRzKSwWDbHXp0At5JSQIJuuDnJk6I-iC2ROT8dFGiw\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWY0zANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMDk0ODQzWhcNMjAwOTAyMTAxODQzWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWMmJuZUJ3cmhHS0ZoWVoxN3dhRWtyRzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK9Z+r3qC00Lxlmms8460rPkux89QNpjkAVeNIt9UnmLoUj9bQkt5uDSEA50WL3n0QtGw4lBDRzaeeO7ptqVaBYw7WnCKZCH4mQBF94Xznw2uB9iXV7XQhpAb2XncU6aaIj7jZWo0wM/qWHmOdNcfGDipSDtHiTfHKeogdr3HidEucNzcdLIkT2RqfXWGM4/utjOzN0WDsS9pHKqs1jjPpQB23S+0YQtpZ5dqA6b1z1d8nz89FPq5mXu70c4O83LYXAADaR89fI3GPITY+0fXuP4sJG3USKt2KqViS3tRzPhEkUcyksFg2x16dALeSUkCCbrg5yZOiPogtkTk/HRRosCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFLOmoz8V83RC1ufv/EurgY5jdg2IMA0GCSqGSIb3DQEBCwUAA4IBAQBET9kMj2/FoLX9M1+eXATcUZ8h+9ZRj+rNhYkqhm/7wExJKMzlYS7jDIgzE2yw9XZH6AisB3OO/t4KvvbASx1YrYWrQL/ZqIVsv1c9tXiLEpdCxNmSlRzi9m9W+m8qdZ9OTBxL7eVo5rpmIqXgk6zNnI/JL686XO0jnpjTce67NdUMSycoLBIHjVBb2nYbisjYq86OsdHV/aupIcAciMEeEYzSN7u/fCWZuh6mOBid4nWnQwvcx4r9OeNS7hiSVLjPtQk44+GBzDgSZ7BJVOinPowUdvG2/5GVaobOPPOwZlFdmZakl7/7g1gcj5yX38TZUV8FxM2xTRfkyegP3/d3\"],"
			+ "\"x5t\":\"77lbSsQ2lrADprsvfPGm3IVSD-Y=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/V3qpm3lQ1jFNMZFmuhE0bF0dbw0.pem\","
			+ "\"x5t#S256\":\"KCEYIALO7EY0DSYUCVmci12KDmLsw0IhnT0bUure3wI=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"vPw2UIeeORPXymY1u_apIHxwDyE\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"3ewvUqIPKaqnvaVdUa6obemBuqM2mEpqB_nDhoWCbL_RyRo5FYV5gr9UND_CLK4qh9c2NcjyaO5UI1yY08lj80r5sDjemyAzQ_2WX60TDEwImD8ZiIDnwc1JEphWEOzLSj8esZWYtKLEtQTdXr0G0NZkAQVC7FAP4N-AhQKFRlXXvTD1r3LIBpyIgXL3g4dyVTvwKVD0L-Q30QVOSjVgRr-FSNAvHl6_5Y3zVSse6mu-ECy0R7VU6Nz8eB7UYRaFMEOBHwCXzEeouPhMjHksU9V7V9TVBaU-xkMs0SZMz0OAmtkdfHEzNjsH0ured6OEx5dd_yfBplcjOBvFJ-3eSw\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWY4DANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMTMyODA4WhcNMjAwOTAyMTM1ODA4WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWVjdjbm16UTN2WkNnRHdaaVM3RGVrSTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAN3sL1KiDymqp72lXVGuqG3pgbqjNphKagf5w4aFgmy/0ckaORWFeYK/VDQ/wiyuKofXNjXI8mjuVCNcmNPJY/NK+bA43psgM0P9ll+tEwxMCJg/GYiA58HNSRKYVhDsy0o/HrGVmLSixLUE3V69BtDWZAEFQuxQD+DfgIUChUZV170w9a9yyAaciIFy94OHclU78ClQ9C/kN9EFTko1YEa/hUjQLx5ev+WN81UrHuprvhAstEe1VOjc/Hge1GEWhTBDgR8Al8xHqLj4TIx5LFPVe1fU1QWlPsZDLNEmTM9DgJrZHXxxMzY7B9Lq3nejhMeXXf8nwaZXIzgbxSft3ksCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUmx0vSuBU4RXUJhM6xKnzTkAZWs4wDQYJKoZIhvcNAQELBQADggEBAF3nP77iyXniRcaBuKynoU/Ld1cVMThntrJNEXa2DI2KlB0wTfjEECc3Qib20/GQHkO4WfT0V54RqRWvOcqAOP28h+ePQ8o/zxO0IF+y+IUEmzzKDIlD03W3P5pQX99qOvCdKEgT+rMXWYUyTnFw5RM+XOdiPkyqwpdzf9TaZ126FAp0U9P0wb0hRugsO+ANviDV/w1KUSHKtcZxT77hRh55kdqvnklrJThkYAP7/39PRd/DlGrS97v6UMRA3juNtiFMpGO3NpyAqY2NdB2Ienx4zTGg1PCErDXHSoztizoR0ewMmPOi2USMT97WwOtu3Dd0S/mFkBQWB8J8eQHdD+c=\"],"
			+ "\"x5t\":\"16Wu5bg8nspaxdVjX6WMUhSoG3k=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/vPw2UIeeORPXymY1u_apIHxwDyE.pem\","
			+ "\"x5t#S256\":\"CiL3jWME-vVoQMoSnv_ih5DBkZfsAM188Qo0jwjN-Lw=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"zDe3eImNtwCLSBcj_yNkK94c7M0\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"tWWE5EDsPolj6xUCd_blNbLioMmQsTZtzwr_5J896DWeDUJ-TqVvIuwqqUOPAJMn6BKFkjAjOMTdPWRKZ8pFT2STAD8CpTEkJRxcsc16aqmXi3FHSv89woNGBLCMeyG2BRU-E94ujtqM2LrutVsPp35DjYq1w3hMNTPC2bX_s-NlLVN3vcMp6CI1ZsqItFeaVe2dL9COPUDgv5GCQAiHh5f1iwfjwV2XknASzoteuwV2yiDTDyTk2PL2svJZF1o1S38NrXI_OjG7J1I9GLBzI8_RvFq0X7NbvRGVVVkBMv9cdMlo9-WNxOnG79NMQ_kO9S1c1QFs-7gLeZTWOoxFVQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWY4TANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwODAyMTMzMzE2WhcNMjAwOTAyMTQwMzE2WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxekFBRDEfMB0GA1UEAxMWVjdjbm16UTN2WkNnRHdaaVM3RGVrSTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALVlhORA7D6JY+sVAnf25TWy4qDJkLE2bc8K/+SfPeg1ng1Cfk6lbyLsKqlDjwCTJ+gShZIwIzjE3T1kSmfKRU9kkwA/AqUxJCUcXLHNemqpl4txR0r/PcKDRgSwjHshtgUVPhPeLo7ajNi67rVbD6d+Q42KtcN4TDUzwtm1/7PjZS1Td73DKegiNWbKiLRXmlXtnS/Qjj1A4L+RgkAIh4eX9YsH48Fdl5JwEs6LXrsFdsog0w8k5Njy9rLyWRdaNUt/Da1yPzoxuydSPRiwcyPP0bxatF+zW70RlVVZATL/XHTJaPfljcTpxu/TTEP5DvUtXNUBbPu4C3mU1jqMRVUCAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFBIf54ukJr+rWdywkhUMhDLH+rcKMA0GCSqGSIb3DQEBCwUAA4IBAQBeS6UHNybEMVCfMsjAjZxoU2JBhW9TzhWroWxtdtR3kEZifkz3uRFtcp6JQR1PZJI5RXIsBudhHAI7irWDGVWYEKxoy7fHWk6o4aC2fjvLPWfDPCpZraWmk7XawXthHEsMWpyWvBnCmy3NL3nyHJivrvhAN4CVPtr7nxzGWf/cewfD9Lwjy19Ql5awlbod93j8k2jC1rkCs3S7QJ1L8tvd4KMiL3AEMRnV/CJ5MUcARoKyD7J5NBS43/qjxrq2U9MP4bONxF/woEUAP0/2IOOBAmihzSKzjRTBhaso6dJvB8681q0jlMPKlhKIdJDmDXlz3WYfH52/R/yxbMbx4oQm\"],"
			+ "\"x5t\":\"juHZ6qj4H_ps06zdVQ6OvoRVrTg=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1zAAD/zDe3eImNtwCLSBcj_yNkK94c7M0.pem\","
			+ "\"x5t#S256\":\"8I7GO0Sr2dbkFJsONeYwxpoKmlbuky65iF51LsLD6H0=\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid5);

		env.putObject("server_jwks", goodServerJwksWithKid5);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_noErrorWithKid6() {

		// header: { "kid": "QcJfxsS-m1hoWHzpI8Kbjdf3EFA", "typ": "JWT", "alg": "PS256" }
		JsonObject goodIdTokenWithKid6 = JsonParser.parseString("{"
			+ "\"value\":\"eyJraWQiOiJRY0pmeHNTLW0xaG9XSHpwSThLYmpkZjNFRkEiLCJ0eXAiOiJKV1QiLCJhbGciOiJQUzI1NiJ9."
			+ "eyJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpzY2EiLCJzdWIiOiJ1cm46aW52ZXN0ZWM6b2I6YWNjOmIwZDc5ZGVlLTQxMWEtNDI4ZC1hOWNkLWVhZDZhZDNmNmU3NCIsImF1ZCI6InBBWEFOS1FHSFFjNHphaU1BS1JGd01GVW5wU0JDeWkzIiwib3BlbmJhbmtpbmdfaW50ZW50X2lkIjoidXJuOmludmVzdGVjOm9iOmFjYzpiMGQ3OWRlZS00MTFhLTQyOGQtYTljZC1lYWQ2YWQzZjZlNzQiLCJpc3MiOiJodHRwczpcL1wvb3BlbmFwaXNhbmRib3hkZXYuaW52ZXN0ZWMuY29tIiwiZXhwIjoxNTY0NjU3NTA1LCJub25jZSI6Ikdjck55YlFoYmMiLCJpYXQiOjE1NjQ2NTM5MDV9."
			+ "V1D1nCDIpLZXggb1snTnNSXiW0qc5E2_g-IvlBqEfi88vg-bAk0-Rr_8LqvbqvOjCudbWZ3ewNj2DR9mADH2bYdBCT3mW0qlxNqCZdPjAipswCQE1kThoKjsM5iQ8EIE63ZC62b4qeT6DQNHAQ-dtYmfrITxbJtNj84nHI3fqguMswdrU4qpJ6oBD9WjZtghmzeajNwQlU2GuGaQb9nhdSTXxAlf3znm-9Z5-7ldXiEJJNx6S9TTqYUrfIv4raOqtjEToJdCzg0xTrTmElAKRQZgm033aYWhwSyxu1li9FMgWCs4kVUhqw38DMhMHOZN-8MGhWrWxJmAs15Pbh7A2Q\""
			+ "}").getAsJsonObject();

		JsonObject goodServerJwksWithKid6 = JsonParser.parseString("{"
			+ "\"keys\":["
			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"QcJfxsS-m1hoWHzpI8Kbjdf3EFA\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"oNLpddMOrbQz7JYmnXxRTdiIZRItqr1PaNzMHFeiJNiSVusi1sFv5_1VovofV0cTHuDblulALr4N4qxpETVxQkj31l54RLHLPEHpXVgyMew93m2IxnIVo-bHBabO-iSrI2qbAJFfglJTlkhp2XhiRrXvnUjsTibhh2uJzNhBuiQo4HD1PzXEGbI89PiFZ7lxSkTN2Q7oTpcakmlkgJIYx5_mgYnGZV0Cz7PA4VJs49bka1JmJ7Gaq-s2V0zzzAbG2fG1LrAs3eGBO7Ojge3DOotB2PwlusMmp_LDKjqwaZ7C2XF1IdGjoR9KodiZc-W12fTJc__PidNsPcKbvEn13Q\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWBzTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNDI1MTQyMjQ3WhcNMjAwNTI1MTQ1MjQ3WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxcUFBRDEfMB0GA1UEAxMWQ1lkR1Zzb01VSm9qZHJONlhOWWJBYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKDS6XXTDq20M+yWJp18UU3YiGUSLaq9T2jczBxXoiTYklbrItbBb+f9VaL6H1dHEx7g25bpQC6+DeKsaRE1cUJI99ZeeESxyzxB6V1YMjHsPd5tiMZyFaPmxwWmzvokqyNqmwCRX4JSU5ZIadl4Yka1751I7E4m4YdriczYQbokKOBw9T81xBmyPPT4hWe5cUpEzdkO6E6XGpJpZICSGMef5oGJxmVdAs+zwOFSbOPW5GtSZiexmqvrNldM88wGxtnxtS6wLN3hgTuzo4HtwzqLQdj8JbrDJqfywyo6sGmewtlxdSHRo6EfSqHYmXPltdn0yXP/z4nTbD3Cm7xJ9d0CAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFDPD11K+QTDateEoJkxa7v7CCDtfMA0GCSqGSIb3DQEBCwUAA4IBAQAZwM2NoYRjEDLuf0KKvzE9I7jeptSYR9hMyCeNcXJ1Nh8tOe5zCerdfRJXqLRN0tkqoHWHiUWcK9V+7FLUVPG2gqkNZeeIrQ1VoBzrufX3vy8qd3gDBwqrlnW+8tLUzZ5Qq4sX7OXHylxIbxshVmeEIV+k/VUrdwtmF3Jkj1Vp5D1fegFwLSO8R2anG2lejs6KKFL970EciEcZFvuoFdMGicPLV6MABakrntxQeQezclqSVkbTcjzXBdJQEIHM9j+gvBYaiZA4F/q7WyInBLIN1iBX8FR4tuOLw2AlceBb1179kQ033aqaKaUVrfiflGkm1KInwJOANA+0WbxIFFFN\"],"
			+ "\"x5t\":\"YzMlJshi1Pm_njnLOHWWtJIKQgY=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/QcJfxsS-m1hoWHzpI8Kbjdf3EFA.pem\","
			+ "\"x5t#S256\":\"2O1_iSu-yzgAWrSKqvixKRZAtURaD3S8ChunoFUQNAw=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"d2U-dJyBKeXvP9wyBggafkMFexo\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"z7VyXghFZ77brM9Fy24t8uuwMwFJRncSVLgrPjGON9BHO34-EfVl6sdOZZJrCeLUP0ZUnzJBXjANuNHq1l0k5oQCfu4VI4CUjl0mfDg6n5p6V_grACPipBPLiWOnFjBkSuobOZG5qOHG2eRJuTtq5-WFpyBPwZUfqtCdtOXpbwlu5Z5JseJO8rXTLEqqHjNzqbCzu9GGXsYHwarRlpLAshsaZFmN1XAbdAeUcm8URz1sycG9ACsriKGxEUdRBR_oV8dUxE-ao_o8jRNCZutua1eiMKCAq589Q7U4M9hnILVRcSC4JgyzbowD9oABkRYS_bNzam4o5c1DtiLl5zgVSQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWB0DANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNDI1MTQ0NTUzWhcNMjAwNTI1MTUxNTUzWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxcUFBRDEfMB0GA1UEAxMWQ1lkR1Zzb01VSm9qZHJONlhOWWJBYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAM+1cl4IRWe+26zPRctuLfLrsDMBSUZ3ElS4Kz4xjjfQRzt+PhH1ZerHTmWSawni1D9GVJ8yQV4wDbjR6tZdJOaEAn7uFSOAlI5dJnw4Op+aelf4KwAj4qQTy4ljpxYwZErqGzmRuajhxtnkSbk7auflhacgT8GVH6rQnbTl6W8JbuWeSbHiTvK10yxKqh4zc6mws7vRhl7GB8Gq0ZaSwLIbGmRZjdVwG3QHlHJvFEc9bMnBvQArK4ihsRFHUQUf6FfHVMRPmqP6PI0TQmbrbmtXojCggKufPUO1ODPYZyC1UXEguCYMs26MA/aAAZEWEv2zc2puKOXNQ7Yi5ec4FUkCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUl+YasXl+lWoJepIckai+6tWirxEwDQYJKoZIhvcNAQELBQADggEBAKJ6O1cj9cu59GFcIoMEvLHlr/gA6ddAsf0wI8YAkQseYoCV1NfPzAJL+ilh4OFkQYFQU1mq4yQbjV2/ybR2iqwAB2qrU0nJNJmuAq5WJ5wucwz8rpNIXdgO1gwVjJqMZznBPkI/CoxgzUPw9XD6YAMQmn73et+Tep2ohpGtGrzHzr+5323rYFeBVBe383RZkQSPG1DVsPkue2W6nK2DSDNRofRsPOoPVf2QHiXkkjMGdR4391xUFaognDKAPbFUXQ1+KoLPv0wfLpjtj/XQpU8NN7ZY0T7SUJC7J0ZkBQhbnOmpXMopkUjKEbq/jzO9jgVwISjSAt9w34zNkGurdXk=\"],"
			+ "\"x5t\":\"EZ8uGnDrwYBTrQrnGHM-R48emoE=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/d2U-dJyBKeXvP9wyBggafkMFexo.pem\","
			+ "\"x5t#S256\":\"YrMA8KCYiOyh5pO5ki78NdHcNOPNXKpj3_V2KS0ibos=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"IQmfrK1Vz_I7Edc2LGQXWsVyC44\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"4U-4igfY8nUihQFgvTbeGxWEukZs8GlZgU6GmxwHsZ_nu58UBvBLKOeL_oyK_EP6vZBCs5KN8Cp7tmLSZc2e5XxTUgAKD_wwxBpVfJxU6OGsbVuEIquSRlMxXb5ArT9L4pITQTZtGkHwWcXl45Ac95mloqIRhfTpylmDLkDiGzLGobeItGNigSry4XhJ3v_FsnASlqf4JmdbBBnQLKz2rWMqZEC8-ualc9_qLyg2AjJ9q71lllxSIRBnTOXc24bq_UjzlIDRk9dgsmUI7EA4o_DtFseeNrzTMpH-IJzgEu9GgsJO5-kLIr-k0byS1FvTaNb81kcQ6IWygu3OG6jM_Q\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFLTCCBBWgAwIBAgIEWcWC9DANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNTAxMTQyMzUzWhcNMjAwNjAxMTQ1MzUzWjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxcUFBRDEfMB0GA1UEAxMWcHNiaDRMekc3RjZOM0dLblBFSzlDTjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOFPuIoH2PJ1IoUBYL023hsVhLpGbPBpWYFOhpscB7Gf57ufFAbwSyjni/6MivxD+r2QQrOSjfAqe7Zi0mXNnuV8U1IACg/8MMQaVXycVOjhrG1bhCKrkkZTMV2+QK0/S+KSE0E2bRpB8FnF5eOQHPeZpaKiEYX06cpZgy5A4hsyxqG3iLRjYoEq8uF4Sd7/xbJwEpan+CZnWwQZ0Cys9q1jKmRAvPrmpXPf6i8oNgIyfau9ZZZcUiEQZ0zl3NuG6v1I85SA0ZPXYLJlCOxAOKPw7RbHnja80zKR/iCc4BLvRoLCTufpCyK/pNG8ktRb02jW/NZHEOiFsoLtzhuozP0CAwEAAaOCAfkwggH1MA4GA1UdDwEB/wQEAwIGwDAVBgNVHSUEDjAMBgorBgEEAYI3CgMMMIHgBgNVHSAEgdgwgdUwgdIGCysGAQQBqHWBBgFkMIHCMCoGCCsGAQUFBwIBFh5odHRwOi8vb2IudHJ1c3Rpcy5jb20vcG9saWNpZXMwgZMGCCsGAQUFBwICMIGGDIGDVXNlIG9mIHRoaXMgQ2VydGlmaWNhdGUgY29uc3RpdHV0ZXMgYWNjZXB0YW5jZSBvZiB0aGUgT3BlbkJhbmtpbmcgUm9vdCBDQSBDZXJ0aWZpY2F0aW9uIFBvbGljaWVzIGFuZCBDZXJ0aWZpY2F0ZSBQcmFjdGljZSBTdGF0ZW1lbnQwbQYIKwYBBQUHAQEEYTBfMCYGCCsGAQUFBzABhhpodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2NzcDA1BggrBgEFBQcwAoYpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcnQwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL29iLnRydXN0aXMuY29tL29iX3BwX2lzc3VpbmdjYS5jcmwwHwYDVR0jBBgwFoAUUHORxiFy03f0/gASBoFceXluP1AwHQYDVR0OBBYEFBYh2XD9Rd7UEh9yE0zEv1NmErYHMA0GCSqGSIb3DQEBCwUAA4IBAQBYDQnBkjUju/P55gPktecWXv0eKTlSuuiJ6EcRxejWdeA7prQn73ifFzAt0mKSptH6lynFb2E603yGjpQIs5Y4pJXjuC1sz7hBjVxgurColLvcHLjb2ptfz940070H9iq1zYP0ilNJ2IbvUGmbfe/1B7QLfsVgdQlekTqCWVfJsGoAOpunC76u1uJuH7Av2Rf07ROsRMuovtZ8wp8adk0COxUcFKDFl2WtrUJ8U0iD+gFlldU6IawBoSMQDxOMnc/hVrT/+rlvAcXW+r86XvrW/zwzneSOrGViAkKj7PmmqvBGDzHZ1uzEWudwUzqbOYSDc0rvlx+MIHcxVop1QbJu\"],"
			+ "\"x5t\":\"X5Dspl7gJt38bIXO2fOwV8ep_Cc=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/IQmfrK1Vz_I7Edc2LGQXWsVyC44.pem\","
			+ "\"x5t#S256\":\"dgz-KseQTBAtgAPARfwRshU5g9CY5FDILsyHVPpG4sg=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"3-JSDcs9q0B7Ij-QaGIbLeJhGeg\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"1bGD2KohIgDTQz3wwcj09O4kx0j6S_u3BuWcG9yGGJfXy1qSxSO0YaFBAkULUlcfZC3qLXz3WlAObohKU5hxIR_Yqm9E-Qs5KKWy6kLPb0CveHTtRTs-5rdKQAqAf1_6goYYbtuQBChHUpfxwCKkn8-KhiOVvsWjdWIYORhq95aWEQAgq3KISXCIQsIOCaqsSlg00Dfs2DmP9ZK-EDtiJ8k1AJTTUqrfU42l-ZqGv-of8mxFaQxr9gzigbLdSHdxLIfVA_hXZfnQX6A3qcEN7-Gu4HOaIYwW8-qD_Xpa-qdTwMbhbTdbl9HYGMSESGPHaCDHsb91_ZNvcDMz6RaKFw\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFODCCBCCgAwIBAgIEWcWDGjANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNTAxMTQzMzU3WhcNMjAwNjAxMTUwMzU3WjBhMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxGzAZBgNVBAsTEjAwMTU4MDAwMDFaRWMxcUFBRDEfMB0GA1UEAxMWcHNiaDRMekc3RjZOM0dLblBFSzlDTjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANWxg9iqISIA00M98MHI9PTuJMdI+kv7twblnBvchhiX18taksUjtGGhQQJFC1JXH2Qt6i1891pQDm6ISlOYcSEf2KpvRPkLOSilsupCz29Ar3h07UU7Pua3SkAKgH9f+oKGGG7bkAQoR1KX8cAipJ/PioYjlb7Fo3ViGDkYaveWlhEAIKtyiElwiELCDgmqrEpYNNA37Ng5j/WSvhA7YifJNQCU01Kq31ONpfmahr/qH/JsRWkMa/YM4oGy3Uh3cSyH1QP4V2X50F+gN6nBDe/hruBzmiGMFvPqg/16WvqnU8DG4W03W5fR2BjEhEhjx2ggx7G/df2Tb3AzM+kWihcCAwEAAaOCAgQwggIAMA4GA1UdDwEB/wQEAwIHgDAgBgNVHSUBAf8EFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQU1taIVDb6ViAT0RN/TTLL9k+pguIwDQYJKoZIhvcNAQELBQADggEBAHUirSIUDJzgQG8sO+t589NfunU4qnENKh9/y6v+8JqkseOT9tCChzT6iqM/5u3oEIBF9RhvVNAhNCVurMbmeN7OCzvdFoRCqbfbRId9Co1S58qBvMQy6c8Ew/d646XtIVkpAY58qYlGTJ8NgZRm0I+GIl7WFfB36LvOOyNNwfKFz/9DmJzTI4Yt3K1EyY2iiA7G4RPOdxQ4v7D9pi3QLeMsXIvIfmK8azSP0CYj5sDsFPxMFdW6EgnFRYxRzh08wqJKz9JDpPDSjmGcjur3ehBJag56Qxt9fofCPFnSRicikcV6Bxsj81RtfCZt7pgD7ZbATokMSyJ9BYNiiuHYmCk=\"],"
			+ "\"x5t\":\"saKhnUaApxYIBRpr4LBvnFOT0RA=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/3-JSDcs9q0B7Ij-QaGIbLeJhGeg.pem\","
			+ "\"x5t#S256\":\"hac_twf4-v9RjUV5TmUkdn3-9B17RvKkbzMjL1BwkBs=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"XddZx_3kJW_SDmghq-YOVPK_HZ0\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"uyReQNg4NVX7dawFOG8DBoAxaGhBDIiGYUO2x3NwlcqbHMZStsZn3l2tKocQJqN4E7WSTlUAIWpGH9GOjB3Q0rpDk8uPcni7CgdlGgPayKbqIXjkCIZTvXhI8Ae6AZkaS9ouyN2Bme7JCLa-OgM6MLE3vL3MwiB2HECUJ4IyR_N6D5prU6H32OFBO7Plh4kVwvR2GcwNw92AKjGL4T6hfD6SgwkjO_7ovpa43q6G_ZtEIRi3bixsYZOWRqJGCC-G02e8W1bN2sloAVqlFuUAKWN5ngsYrcE1UhYvwhRJvZBVMuVLs33KO9UzbzEmqr6BOKPmKzdiOn1CH9p_UUnUMQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFozCCBIugAwIBAgIEWcWLNTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNjA0MDcyNTAwWhcNMjAwNzA0MDc1NTAwWjBhMQswCQYDVQQGEwJHQjEaMBgGA1UEChMRSW52ZXN0ZWMgQmFuayBQTEMxGTAXBgNVBGETEFBTREdCLUZDQS0xNzIzMzAxGzAZBgNVBAMTEjAwMTU4MDAwMDFaRWMxcUFBRDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALskXkDYODVV+3WsBThvAwaAMWhoQQyIhmFDtsdzcJXKmxzGUrbGZ95drSqHECajeBO1kk5VACFqRh/Rjowd0NK6Q5PLj3J4uwoHZRoD2sim6iF45AiGU714SPAHugGZGkvaLsjdgZnuyQi2vjoDOjCxN7y9zMIgdhxAlCeCMkfzeg+aa1Oh99jhQTuz5YeJFcL0dhnMDcPdgCoxi+E+oXw+koMJIzv+6L6WuN6uhv2bRCEYt24sbGGTlkaiRggvhtNnvFtWzdrJaAFapRblACljeZ4LGK3BNVIWL8IUSb2QVTLlS7N9yjvVM28xJqq+gTij5is3Yjp9Qh/af1FJ1DECAwEAAaOCAm8wggJrMA4GA1UdDwEB/wQEAwIHgDBpBggrBgEFBQcBAwRdMFswEwYGBACORgEGMAkGBwQAjkYBBgMwRAYGBACBmCcCMDowEzARBgcEAIGYJwEBDAZQU1BfQVMMG0ZpbmFuY2lhbCBDb25kdWN0IEF1dGhvcml0eQwGR0ItRkNBMCAGA1UdJQEB/wQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjCB4AYDVR0gBIHYMIHVMIHSBgsrBgEEAah1gQYBZDCBwjAqBggrBgEFBQcCARYeaHR0cDovL29iLnRydXN0aXMuY29tL3BvbGljaWVzMIGTBggrBgEFBQcCAjCBhgyBg1VzZSBvZiB0aGlzIENlcnRpZmljYXRlIGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIE9wZW5CYW5raW5nIFJvb3QgQ0EgQ2VydGlmaWNhdGlvbiBQb2xpY2llcyBhbmQgQ2VydGlmaWNhdGUgUHJhY3RpY2UgU3RhdGVtZW50MG0GCCsGAQUFBwEBBGEwXzAmBggrBgEFBQcwAYYaaHR0cDovL29iLnRydXN0aXMuY29tL29jc3AwNQYIKwYBBQUHMAKGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3J0MDoGA1UdHwQzMDEwL6AtoCuGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3JsMB8GA1UdIwQYMBaAFFBzkcYhctN39P4AEgaBXHl5bj9QMB0GA1UdDgQWBBQrOPbgocQUSq/7gVG5BghEI9e4PDANBgkqhkiG9w0BAQsFAAOCAQEAPslcEAXRHA0ykHqDf85JuWgminkJ/AmVmULG9fLOOLpU7/wLlZLL2hNrAAI/zLJMC1SAS1ah47L18c1xHaiZ5YliUQFvsxfzlCWGVOBL9ghu1JS8VvICI+eMfABDQbu2OFcYzaATQUZgC4VBSpMvpQkV+uPtoR6W3SxUeup2AnsfPV67/2NtFdpbPkfzpUo92SpUX6zFTLWLqZ/dZhV79+YDgkvauOXQnhmPyk0TQg52BdlAODN/ybY8HaKTe/wthRoK/cmMXLv6zaM2SVG4/IaKvzg03WZodCQeGP6OKCZ3VPjnlSDhh97umZRf9vkgE2RFLpLrxY6Yc9K2nlc2/g==\"],"
			+ "\"x5t\":\"KOVoPqv_HjFVc8bBKdz8_twXzAI=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/XddZx_3kJW_SDmghq-YOVPK_HZ0.pem\","
			+ "\"x5t#S256\":\"Ij4DXahtJXQP8w-qPfYYMb-lyNzyo5y0PNoWhJJn8GU=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"o8sw08NnmpEQZ2ZJmt7hUh0Z7Xg\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"vb1_ovVsKwNgEKGyLFr8buMbHx3KE9ykuq06XUoo2gHlyiOQmqs4p4Yq6ogZkmvpJiYnfSNahmWzaXtrJdIucdkw5_32I5pZuWbz5xvmMus1_16Isg9bMsntywymo8mWS27wQ3MXbS8WiY90-q7le3j6m5Bc5GH6vlNwuMn1gXAt5xUagwUKPDFjTmak-0p4C3Cs9mCs1Mgg9vJJGUD5KR0firkOylwCsQ-UpcS7vmA-u3q_M-aVzHOSgV9txlVKI8GVA4sOSNNLhf2n2QcY0GfWzaRJEAEJjBGQVxkhnbBHfmux-shTfK471RzBu1Ico308vMdc7OI1H-a4CuEPbQ\","
			+ "\"use\":\"sig\","
			+ "\"x5c\":[\"MIIFmDCCBICgAwIBAgIEWcWLODANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNjA0MDkxMzIwWhcNMjAwNzA0MDk0MzIwWjBhMQswCQYDVQQGEwJHQjEaMBgGA1UEChMRSW52ZXN0ZWMgQmFuayBQTEMxGTAXBgNVBGETEFBTREdCLUZDQS0xNzIzMzAxGzAZBgNVBAMTEjAwMTU4MDAwMDFaRWMxcUFBRDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL29f6L1bCsDYBChsixa/G7jGx8dyhPcpLqtOl1KKNoB5cojkJqrOKeGKuqIGZJr6SYmJ30jWoZls2l7ayXSLnHZMOf99iOaWblm8+cb5jLrNf9eiLIPWzLJ7csMpqPJlktu8ENzF20vFomPdPqu5Xt4+puQXORh+r5TcLjJ9YFwLecVGoMFCjwxY05mpPtKeAtwrPZgrNTIIPbySRlA+SkdH4q5DspcArEPlKXEu75gPrt6vzPmlcxzkoFfbcZVSiPBlQOLDkjTS4X9p9kHGNBn1s2kSRABCYwRkFcZIZ2wR35rsfrIU3yuO9UcwbtSHKN9PLzHXOziNR/muArhD20CAwEAAaOCAmQwggJgMA4GA1UdDwEB/wQEAwIGwDBpBggrBgEFBQcBAwRdMFswEwYGBACORgEGMAkGBwQAjkYBBgIwRAYGBACBmCcCMDowEzARBgcEAIGYJwEBDAZQU1BfQVMMG0ZpbmFuY2lhbCBDb25kdWN0IEF1dGhvcml0eQwGR0ItRkNBMBUGA1UdJQQOMAwGCisGAQQBgjcKAwwwgeAGA1UdIASB2DCB1TCB0gYLKwYBBAGodYEGAWQwgcIwKgYIKwYBBQUHAgEWHmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9wb2xpY2llczCBkwYIKwYBBQUHAgIwgYYMgYNVc2Ugb2YgdGhpcyBDZXJ0aWZpY2F0ZSBjb25zdGl0dXRlcyBhY2NlcHRhbmNlIG9mIHRoZSBPcGVuQmFua2luZyBSb290IENBIENlcnRpZmljYXRpb24gUG9saWNpZXMgYW5kIENlcnRpZmljYXRlIFByYWN0aWNlIFN0YXRlbWVudDBtBggrBgEFBQcBAQRhMF8wJgYIKwYBBQUHMAGGGmh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vY3NwMDUGCCsGAQUFBzAChilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNydDA6BgNVHR8EMzAxMC+gLaArhilodHRwOi8vb2IudHJ1c3Rpcy5jb20vb2JfcHBfaXNzdWluZ2NhLmNybDAfBgNVHSMEGDAWgBRQc5HGIXLTd/T+ABIGgVx5eW4/UDAdBgNVHQ4EFgQUQ6xDHPsRBzaxkjNbxU13AJ/euyYwDQYJKoZIhvcNAQELBQADggEBAKF65rDokbTdy4C8qF8mg6z8Pg+ZEj4jIq5bjRJfjsNL4vV58ghs+qIlc2LXG86oeEgNY8M2vczYhhLPj/hCChlctPWGTAZ+qtM3Z5yqcEyiKzpozIN+773z0eIsPoJMuq1gsRXPkcdLqTt21HcRGg/d2ypFGCCmmO+uw8mMxu+ar7Vizr6iDQf3K9Fb+8FxUh8Lqc4ynt+buy9GRqqUPv0zIlM6hT62MdelxRnIII9vw9Ucjy5vHTsG+nb9qEcOq3fo2G6LCatadNs/YuWTDXyByDjGfXXkXWXCmC+d0SC9nLwNjEcvf4aJHnOLqQsieV4L+3x42EP+unnmrUMIYWo=\"],"
			+ "\"x5t\":\"Jnesm3HopKtSp5bcLIZ_vgF_urU=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/o8sw08NnmpEQZ2ZJmt7hUh0Z7Xg.pem\","
			+ "\"x5t#S256\":\"FTw8DUaUutQlHA4aiInZt8cU_WLDRmHdvtqYVVwBPhY=\""
			+ "},"

			+ "{"
			+ "\"e\":\"AQAB\","
			+ "\"kid\":\"PGfBNUof6FF4pAkjGzBKPcyhn70\","
			+ "\"kty\":\"RSA\","
			+ "\"n\":\"0wPJs0P1js3kNJvsdn9VcxvNli5ychlq6HLXMxRXF3PdfjxH3MKbbjIASuCNdJ_ZtSX8fYyhvJ1KRt6EBghiec_SNoCKucArk1jBS-rKzg5IQvdisCEBSYF5ghN3T9tsDuFcxfrpzxlfO174F-7FMHHELEvJDKdJAKDlOf5-A9fMjqWFT7TWqH04hANBjSEACckk-jHNp6zOKHH9sWwpue-6tPKbqlg9Tb8Al79dLRyjhdNbYVlP2hiB3ivkwAGJiYxtJo8bPQTPFzOfPgR8f_lvAhgGE4GiWHiVyPXMMCaTp2KnEh436aJbtdFZ3szjawjhMLvhbH_Mxxbkhyd4HQ\","
			+ "\"use\":\"tls\","
			+ "\"x5c\":[\"MIIFozCCBIugAwIBAgIEWcWLOTANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJHQjEUMBIGA1UEChMLT3BlbkJhbmtpbmcxLjAsBgNVBAMTJU9wZW5CYW5raW5nIFByZS1Qcm9kdWN0aW9uIElzc3VpbmcgQ0EwHhcNMTkwNjA0MDkyODU4WhcNMjAwNzA0MDk1ODU4WjBhMQswCQYDVQQGEwJHQjEaMBgGA1UEChMRSW52ZXN0ZWMgQmFuayBQTEMxGTAXBgNVBGETEFBTREdCLUZDQS0xNzIzMzAxGzAZBgNVBAMTEjAwMTU4MDAwMDFaRWMxcUFBRDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANMDybND9Y7N5DSb7HZ/VXMbzZYucnIZauhy1zMUVxdz3X48R9zCm24yAErgjXSf2bUl/H2MobydSkbehAYIYnnP0jaAirnAK5NYwUvqys4OSEL3YrAhAUmBeYITd0/bbA7hXMX66c8ZXzte+BfuxTBxxCxLyQynSQCg5Tn+fgPXzI6lhU+01qh9OIQDQY0hAAnJJPoxzaeszihx/bFsKbnvurTym6pYPU2/AJe/XS0co4XTW2FZT9oYgd4r5MABiYmMbSaPGz0Ezxcznz4EfH/5bwIYBhOBolh4lcj1zDAmk6dipxIeN+miW7XRWd7M42sI4TC74Wx/zMcW5IcneB0CAwEAAaOCAm8wggJrMA4GA1UdDwEB/wQEAwIHgDBpBggrBgEFBQcBAwRdMFswEwYGBACORgEGMAkGBwQAjkYBBgMwRAYGBACBmCcCMDowEzARBgcEAIGYJwEBDAZQU1BfQVMMG0ZpbmFuY2lhbCBDb25kdWN0IEF1dGhvcml0eQwGR0ItRkNBMCAGA1UdJQEB/wQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjCB4AYDVR0gBIHYMIHVMIHSBgsrBgEEAah1gQYBZDCBwjAqBggrBgEFBQcCARYeaHR0cDovL29iLnRydXN0aXMuY29tL3BvbGljaWVzMIGTBggrBgEFBQcCAjCBhgyBg1VzZSBvZiB0aGlzIENlcnRpZmljYXRlIGNvbnN0aXR1dGVzIGFjY2VwdGFuY2Ugb2YgdGhlIE9wZW5CYW5raW5nIFJvb3QgQ0EgQ2VydGlmaWNhdGlvbiBQb2xpY2llcyBhbmQgQ2VydGlmaWNhdGUgUHJhY3RpY2UgU3RhdGVtZW50MG0GCCsGAQUFBwEBBGEwXzAmBggrBgEFBQcwAYYaaHR0cDovL29iLnRydXN0aXMuY29tL29jc3AwNQYIKwYBBQUHMAKGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3J0MDoGA1UdHwQzMDEwL6AtoCuGKWh0dHA6Ly9vYi50cnVzdGlzLmNvbS9vYl9wcF9pc3N1aW5nY2EuY3JsMB8GA1UdIwQYMBaAFFBzkcYhctN39P4AEgaBXHl5bj9QMB0GA1UdDgQWBBQzQUaqkCuIxXRCP9krw86N8KrD2jANBgkqhkiG9w0BAQsFAAOCAQEADy0hs0s9CxO1Yv3KekzRlcD4v7LTT12uj2zMxsGAq7FPYqtSPyvRVfwJDVat9uiqdGHMdHoW3pmXwOIvvqkiD7OkLzNY1TZpoiVzCzLxHSNCY+VZ0sSMaG6c7zLb2j29SKJ1Q9Tu6XPh1EbRSjKaSGe3BuWfGG8lnsg6rcU1Eovv3Z3oTPYNRn6HdIsjle0VSBwnPalsrMeqc/LSV1YlNxyPgt7uhgqDPdKz7eX8u6IGrR60cnXsuUZKQzGMrlfu1UmGfmzejkehuyGk0gPtEXnDpNwv2Ha9sIB52Zr2ZQ2t+9JWWekwxWiDJ7nkYrq9nTkPDDY7wirjBY6qKzXTvg==\"],"
			+ "\"x5t\":\"Eizd2L1Yb_-jHngrSrQNieRjrKM=\","
			+ "\"x5u\":\"https://keystore.openbankingtest.org.uk/0015800001ZEc1qAAD/PGfBNUof6FF4pAkjGzBKPcyhn70.pem\","
			+ "\"x5t#S256\":\"82tH3bXehO03Q7ad5rDSv3m4hKMNZ7vgyYrtumQcCfg=\""
			+ "}"
			+ "]}").getAsJsonObject();

		env.putObject("id_token", goodIdTokenWithKid6);

		env.putObject("server_jwks", goodServerJwksWithKid6);

		Security.addProvider(new BouncyCastleProvider());

		cond.execute(env);

	}

	@Test
	public void testEvaluate_notFoundKeyWithKid() {
		assertThrows(ConditionError.class, () -> {

			// header: { "kid": "wU3ifIIaLOUAReRB/FG6eM1P1QM=", "typ": "JWT", "alg": "PS256" }
			JsonObject goodIdTokenWithKid = JsonParser.parseString("{"
				+ "\"value\":\"eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUFMyNTYifQ."
				+ "eyJzdWIiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsImF1ZGl0VHJhY2tpbmdJZCI6IjlhYmVjNzY0LTc0YmUtNDBiMy1hYWQ4LTY2NDFlZWIwZmMxMy0yMjkxNDEiLCJpc3MiOiJodHRwczovL29iLnVhdC5iZG4ucHVibGljLnNhaW5zYnVyeXNiYW5rLmNsb3VkOjQ0My9zc28vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9nZW5lcmFsIiwidG9rZW5OYW1lIjoiaWRfdG9rZW4iLCJub25jZSI6IlZiUVcwMkxEUmUiLCJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpjYSIsImF1ZCI6IjhkNWNhNDY1LTEwZjEtNDk0OS05MGJjLTdmMTc1MWFlOGYxYSIsImNfaGFzaCI6Ii1TcW9nNnFzY0JIMURXa1B4Q1JmNGciLCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiJUN2ROalhDV1BIYmFDZk5odUYyOS1ENHI4MnMiLCJzX2hhc2giOiJPRUM2MldSQjBIZzlVTXZVUm1mODNRIiwiYXpwIjoiOGQ1Y2E0NjUtMTBmMS00OTQ5LTkwYmMtN2YxNzUxYWU4ZjFhIiwiYXV0aF90aW1lIjoxNTY0NTI0Nzg3LCJyZWFsbSI6Ii9nZW5lcmFsIiwiZXhwIjoxNTY0NTI4NDAzLCJ0b2tlblR5cGUiOiJKV1RUb2tlbiIsImlhdCI6MTU2NDUyNDgwM30."
				+ "orxSbO_yU8BsdIkLFsNoV7lJU403DIkSM8gh1EhXG_z4gm5CtnHVs3nYSOtRt21SrY6UepulH2O-kYQ8vgHG9-qOPxlJuW1CWd7I7sQIt5gBCC8-26Uv6QNbPB-qywgMQK1aYpRRNfPd6PCoK0RqzooQ5fJ_Sli5525vw_o-4-w7YmDgrYnp3201rjH6KE3X-wbaj9MhwXDEHKiLgMU36s0SiXGPIWUvfBZQ8bMWiAY7q5zbmlpFNHL9Q7kdPei_Paf1Z0MK__vJffnHFZoEnZmRGWgSjuCFU56QfcMu_ECeGUmn_9pthQRPoonQhVZJigKrydc58ub-43XfFcBfgA\""
				+ "}").getAsJsonObject();

			JsonObject goodServerJwks = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
				+ "},"
				+ "{"
				+ "\"kty\":\"oct\","
				+ "\"alg\":\"HS256\","
				+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("id_token", goodIdTokenWithKid);

			env.putObject("server_jwks", goodServerJwks);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_foundKeyWithKidAndVerifyFailure() {
		assertThrows(ConditionError.class, () -> {

			// header: { "kid": "wU3ifIIaLOUAReRB/FG6eM1P1QM=", "typ": "JWT", "alg": "PS256" }
			JsonObject goodIdTokenWithKid = JsonParser.parseString("{"
				+ "\"value\":\"eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUFMyNTYifQ."
				+ "eyJzdWIiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsImF1ZGl0VHJhY2tpbmdJZCI6IjlhYmVjNzY0LTc0YmUtNDBiMy1hYWQ4LTY2NDFlZWIwZmMxMy0yMjkxNDEiLCJpc3MiOiJodHRwczovL29iLnVhdC5iZG4ucHVibGljLnNhaW5zYnVyeXNiYW5rLmNsb3VkOjQ0My9zc28vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9nZW5lcmFsIiwidG9rZW5OYW1lIjoiaWRfdG9rZW4iLCJub25jZSI6IlZiUVcwMkxEUmUiLCJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpjYSIsImF1ZCI6IjhkNWNhNDY1LTEwZjEtNDk0OS05MGJjLTdmMTc1MWFlOGYxYSIsImNfaGFzaCI6Ii1TcW9nNnFzY0JIMURXa1B4Q1JmNGciLCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiJUN2ROalhDV1BIYmFDZk5odUYyOS1ENHI4MnMiLCJzX2hhc2giOiJPRUM2MldSQjBIZzlVTXZVUm1mODNRIiwiYXpwIjoiOGQ1Y2E0NjUtMTBmMS00OTQ5LTkwYmMtN2YxNzUxYWU4ZjFhIiwiYXV0aF90aW1lIjoxNTY0NTI0Nzg3LCJyZWFsbSI6Ii9nZW5lcmFsIiwiZXhwIjoxNTY0NTI4NDAzLCJ0b2tlblR5cGUiOiJKV1RUb2tlbiIsImlhdCI6MTU2NDUyNDgwM30."
				+ "orxSbO_yU8BsdIkLFsNoV7lJU403DIkSM8gh1EhXG_z4gm5CtnHVs3nYSOtRt21SrY6UepulH2O-kYQ8vgHG9-qOPxlJuW1CWd7I7sQIt5gBCC8-26Uv6QNbPB-qywgMQK1aYpRRNfPd6PCoK0RqzooQ5fJ_Sli5525vw_o-4-w7YmDgrYnp3201rjH6KE3X-wbaj9MhwXDEHKiLgMU36s0SiXGPIWUvfBZQ8bMWiAY7q5zbmlpFNHL9Q7kdPei_Paf1Z0MK__vJffnHFZoEnZmRGWgSjuCFU56QfcMu_ECeGUmn_9pthQRPoonQhVZJigKrydc58ub-43XfFcBfgA\""
				+ "}").getAsJsonObject();

			JsonObject wrongServerJwksWithKid = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"EC\","
				+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"MUOPc5byMEN9q_9gqArkd1EDajg\","
				+ "\"x5c\":[\"MIIBwjCCAWkCCQCw3GyPBTSiGzAJBgcqhkjOPQQBMGoxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxDzANBgNVBAsTBk9wZW5BTTESMBAGA1UEAxMJZXMyNTZ0ZXN0MB4XDTE3MDIwMzA5MzQ0NloXDTIwMTAzMDA5MzQ0NlowajELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0JyaXN0b2wxEDAOBgNVBAcTB0JyaXN0b2wxEjAQBgNVBAoTCUZvcmdlUm9jazEPMA0GA1UECxMGT3BlbkFNMRIwEAYDVQQDEwllczI1NnRlc3QwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQ3sy05tV/3YUlPBi9jZm9NVPeuBmntrtcO3NP/1HDsgLsTZsqKHD6KWIeJNRQnONcriWVaIcZYTKNykyCVUz93MAkGByqGSM49BAEDSAAwRQIgZhTox7WpCb9krZMyHfgCzHwfu0FVqaJsO2Nl2ArhCX0CIQC5GgWD5jjCRlIWSEFSDo4DZgoQFXaQkJUSUbJZYpi9dA==\"],"
				+ "\"x\":\"N7MtObVf92FJTwYvY2ZvTVT3rgZp7a7XDtzT_9Rw7IA\","
				+ "\"y\":\"uxNmyoocPopYh4k1FCc41yuJZVohxlhMo3KTIJVTP3c\","
				+ "\"crv\":\"P-256\","
				+ "\"alg\":\"PS256\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("id_token", goodIdTokenWithKid);

			env.putObject("server_jwks", wrongServerJwksWithKid);

			cond.execute(env);

		});

	}

	@Test
	public void testEvaluate_foundMoreThanOneKeyWithRightKidKtyAlgAndUseSig() {
		assertThrows(ConditionError.class, () -> {

			// header: { "kid": "wU3ifIIaLOUAReRB/FG6eM1P1QM=", "typ": "JWT", "alg": "PS256" }
			JsonObject goodIdTokenWithKid = JsonParser.parseString("{"
				+ "\"value\":\"eyJ0eXAiOiJKV1QiLCJraWQiOiJ3VTNpZklJYUxPVUFSZVJCL0ZHNmVNMVAxUU09IiwiYWxnIjoiUFMyNTYifQ."
				+ "eyJzdWIiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsImF1ZGl0VHJhY2tpbmdJZCI6IjlhYmVjNzY0LTc0YmUtNDBiMy1hYWQ4LTY2NDFlZWIwZmMxMy0yMjkxNDEiLCJpc3MiOiJodHRwczovL29iLnVhdC5iZG4ucHVibGljLnNhaW5zYnVyeXNiYW5rLmNsb3VkOjQ0My9zc28vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9nZW5lcmFsIiwidG9rZW5OYW1lIjoiaWRfdG9rZW4iLCJub25jZSI6IlZiUVcwMkxEUmUiLCJhY3IiOiJ1cm46b3BlbmJhbmtpbmc6cHNkMjpjYSIsImF1ZCI6IjhkNWNhNDY1LTEwZjEtNDk0OS05MGJjLTdmMTc1MWFlOGYxYSIsImNfaGFzaCI6Ii1TcW9nNnFzY0JIMURXa1B4Q1JmNGciLCJvcGVuYmFua2luZ19pbnRlbnRfaWQiOiJ1cm4tc2ItaW50ZW50LTAyMmJlYzVhLWE1MTgtNGQwYi1hMGNhLTY3OGZlNWM0MjJkZCIsIm9yZy5mb3JnZXJvY2sub3BlbmlkY29ubmVjdC5vcHMiOiJUN2ROalhDV1BIYmFDZk5odUYyOS1ENHI4MnMiLCJzX2hhc2giOiJPRUM2MldSQjBIZzlVTXZVUm1mODNRIiwiYXpwIjoiOGQ1Y2E0NjUtMTBmMS00OTQ5LTkwYmMtN2YxNzUxYWU4ZjFhIiwiYXV0aF90aW1lIjoxNTY0NTI0Nzg3LCJyZWFsbSI6Ii9nZW5lcmFsIiwiZXhwIjoxNTY0NTI4NDAzLCJ0b2tlblR5cGUiOiJKV1RUb2tlbiIsImlhdCI6MTU2NDUyNDgwM30."
				+ "orxSbO_yU8BsdIkLFsNoV7lJU403DIkSM8gh1EhXG_z4gm5CtnHVs3nYSOtRt21SrY6UepulH2O-kYQ8vgHG9-qOPxlJuW1CWd7I7sQIt5gBCC8-26Uv6QNbPB-qywgMQK1aYpRRNfPd6PCoK0RqzooQ5fJ_Sli5525vw_o-4-w7YmDgrYnp3201rjH6KE3X-wbaj9MhwXDEHKiLgMU36s0SiXGPIWUvfBZQ8bMWiAY7q5zbmlpFNHL9Q7kdPei_Paf1Z0MK__vJffnHFZoEnZmRGWgSjuCFU56QfcMu_ECeGUmn_9pthQRPoonQhVZJigKrydc58ub-43XfFcBfgA\""
				+ "}").getAsJsonObject();

			JsonObject wrongServerJwksWithMultipleKidValid = JsonParser.parseString("{"
				+ "\"keys\":["
				+ "{"
				+ "\"kty\":\"RSA\","
				+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
				+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
				+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
				+ "\"e\":\"AQAB\","
				+ "\"alg\":\"RS512\""
				+ "},"

				+ "{"
				+ "\"kty\":\"RSA\","
				+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
				+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
				+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
				+ "\"e\":\"AQAB\","
				+ "\"alg\":\"RS256\""
				+ "},"

				+ "{"
				+ "\"kty\":\"EC\","
				+ "\"kid\":\"pZSfpEq8tQPeiIe3fnnaWnnr/Zc=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"6syJZMj8X0Adm-XNzWHHIl_3kG4\","
				+ "\"x5c\":[\"MIICSTCCAawCCQD+h7BW+8vxbTAJBgcqhkjOPQQBMGoxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdCcmlzdG9sMRAwDgYDVQQHEwdCcmlzdG9sMRIwEAYDVQQKEwlGb3JnZVJvY2sxDzANBgNVBAsTBk9wZW5BTTESMBAGA1UEAxMJZXM1MTJ0ZXN0MB4XDTE3MDIwMzA5NDA0OVoXDTIwMTAzMDA5NDA0OVowajELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0JyaXN0b2wxEDAOBgNVBAcTB0JyaXN0b2wxEjAQBgNVBAoTCUZvcmdlUm9jazEPMA0GA1UECxMGT3BlbkFNMRIwEAYDVQQDEwllczUxMnRlc3QwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABAB3VSmzQx8pvjIlIenGmqHf5LafD1zeoNcyCi85WgkjmT/NiimkLH8JbQCpzK8NdvZ1cftpLfMdSdaadQA3vR7V7QFKoUSnGLwOpRJSN1K36r6boVbMhBQUOHDPxPb+Fhp0XP6a4ok1Wv1Au2HwrUCU/RfDnNtb/4ue0qdzKv78ObnkXTAJBgcqhkjOPQQBA4GLADCBhwJCAd0cIC8QSVn2bp3DGYXxkz5vPNmR7Mv22E2WaWtHlsYcBIY8E7Kd4wxVD+otogDFf4fcFmA34tk5n4PLa67wS26CAkExH1YP2rFbF3LQZVEjTHOwTh+K5S0cIxmzTGx7nnH9+dnxSpCaxKjQ/L//pH/siWe6h/dmUkTY3Y9t939ypY1Blw==\"],"
				+ "\"x\":\"AHdVKbNDHym-MiUh6caaod_ktp8PXN6g1zIKLzlaCSOZP82KKaQsfwltAKnMrw129nVx-2kt8x1J1pp1ADe9HtXt\","
				+ "\"y\":\"AUqhRKcYvA6lElI3UrfqvpuhVsyEFBQ4cM_E9v4WGnRc_priiTVa_UC7YfCtQJT9F8Oc21v_i57Sp3Mq_vw5ueRd\","
				+ "\"crv\":\"P-521\","
				+ "\"alg\":\"ES512\""
				+ "},"

				+ "{"
				+ "\"kty\":\"RSA\","
				+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
				+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
				+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
				+ "\"e\":\"AQAB\","
				+ "\"alg\":\"PS256\""
				+ "},"

				+ "{"
				+ "\"kty\":\"RSA\","
				+ "\"kid\":\"wU3ifIIaLOUAReRB/FG6eM1P1QM=\","
				+ "\"use\":\"sig\","
				+ "\"x5t\":\"5eOfy1Nn2MMIKVRRkq0OgFAw348\","
				+ "\"x5c\":[\"MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdVbmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYDVQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3duMB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5rbm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UEChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwWaUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37oOexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19QcFbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJqqzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkCAwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjXmd0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CVh57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqcGkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzfM+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKSLT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\"],"
				+ "\"n\":\"10iGQ5l5IdqBP1l5wb5BDBZpSyLs4y_Um-kGv_se0BkRkwMZavGD_Nqjq8x3-fKNI45nU7E7COAh8gjn6LCXfug57EQfi0gOgKhOhVcLmKqIEXPmqeagvMndsXWIy6k8WPPwBzSkN5PDLKBXKG_X1BwVvOE9276nrx6lJq3CgNbmiEihovNt_6g5pCxiSarIk2uaG3T3Ve6hUJrM0W35QmqrNM9rL3laPgXtCuz4sJJN3rGnQq_25YbUawW9L1MTVbqKxWiyN5WbXoWUg8to1DhoQnXzDymIMhFa45NTLhxtdH9CDprXWXWBaWzo8mIFes5yI4AJW4ZSg1PPO2UJSQ\","
				+ "\"e\":\"AQAB\","
				+ "\"alg\":\"PS256\""
				+ "}"
				+ "]}").getAsJsonObject();

			env.putObject("id_token", goodIdTokenWithKid);

			env.putObject("server_jwks", wrongServerJwksWithMultipleKidValid);

			Security.addProvider(new BouncyCastleProvider());

			cond.execute(env);

		});

	}

}
