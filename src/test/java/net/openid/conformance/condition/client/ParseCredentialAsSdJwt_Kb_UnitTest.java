package net.openid.conformance.condition.client;

import com.authlete.sd.Disclosure;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ParseCredentialAsSdJwt_Kb_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ParseCredentialAsSdJwtKb cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ParseCredentialAsSdJwtKb();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link ValidateIdTokenSignature#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		String sdJwtStr = "eyJjdHkiOiJjcmVkZW50aWFsLWNsYWltcy1zZXQranNvbiIsInR5cCI6InZjK3NkLWp3dCIsImFsZyI6IkVTMjU2In0."+
			"eyJjcmVkZW50aWFsU3ViamVjdCI6eyJfc2QiOlsiQXN0c3NCb0tVbVNiREZXdUZmMmtRNU5CbFMtR1NBejF2LVhmQkJSQnJESSIsImw4VTl"+
			"pcUphUzFlWmxheDRCdEF3WTJNaS1aMUxXSTdHa1dBYnBtVndCaWMiLCJ5QkYtMUpIclJJczJBdVVIdEd5WktuQ3RNaUNtNzRxZEpreU5XVUdS" +
			"NDcwIl19LCJfc2RfYWxnIjoic2hhLTI1NiIsImlzcyI6ImRpZDpqd2s6ZXlKcmRIa2lPaUpGUXlJc0ltTnlkaUk2SWxBdE1qVTJJaXdpZUN" +
			"JNkluQlJNWFpZZEZOVmRGRmxXWEY2U2t4aUxXVXlaV000TTJKR2RFazJkbXg2VG5SbE4yUXdPRkZLZWpRaUxDSjVJam9pVW5abU9WbFVSME" +
			"ZyTVRCNk56SnpSbTB6UldsbVZqaFNSVTl1WWs1eFgxWlhZMlZoT0d4NFVtZzBUU0lzSW1Gc1p5STZJa1ZUTWpVMkluMD0iLCJjbmYiOnsian" +
			"drIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiQVNDVW1OQ2dQTk9BVFJiZDhrc3UxdVVNQmpkLXYzVElYNjNxSEtsQzZVQSIsIn" +
			"kiOiJWc055Y1Rkb3ZZb1p2bHVtbTJPTjFQc0tqelFGald1cmNZYjFWS2o1TzFzIn19LCJ0eXBlIjoiVmVyaWZpZWRFTWFpbCIsImV4cCI6M" +
			"TY4MzQ3MDc3NywiaWF0IjoxNjgyNjA2Nzc3fQ.-1lAonblykatcmb7tmJYmI4SmsRSWLp1TmujK0nlvgqYuw-bP2Me29fBnnvQrmh-phW6i" +
			"K7XG1LbQoe7fbhlUQ~WyJsUWlWQVBub1V0Vlo5Z3NHVGhobGlBIiwiZW1haWwiLCJqb3NlcGhAaGVlbmFuLm1lLnVrIl0~eyJ0eXAiOiJKV1" +
			"QiLCJhbGciOiJFUzI1NiJ9.eyJub25jZSI6IjV1Mm1sMmJFTUUiLCJpYXQiOjE2OTA5MDQ5OTUsImF1ZCI6Imh0dHBzOi8vbG9jYWxob3N0Lm" +
			"Vtb2JpeC5jby51azo4NDQzL3Rlc3QvYS9vaWRmLXZjLXRlc3QvY2FsbGJhY2sifQ.oEXwTsJiOq2da037fl2cbKKzPCq4iPYReQPnQA8ZtWxG" +
			"74D3CoYRCyPT6GrL-H8xi1PUI7AAvGmsJaStDZifPA";

		env.putString("credential", sdJwtStr);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("credential");
	}

	private static String b64(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * An SD-JWT+KB with one disclosure for the 'email' claim. The signatures are not real; the
	 * parser does not verify them.
	 *
	 * @param sdAlgJson the JSON value of the _sd_alg claim
	 * @param digestAlg the algorithm the digest in _sd is made with
	 */
	private static String sdJwtWithSdAlg(String sdAlgJson, String digestAlg) {
		Disclosure disclosure = new Disclosure("salt", "email", "user@example.com");
		String payload = "{\"iss\":\"https://issuer.example.com\",\"_sd\":[\"" + disclosure.digest(digestAlg) + "\"],\"_sd_alg\":" + sdAlgJson + "}";
		return b64("{\"alg\":\"ES256\",\"typ\":\"dc+sd-jwt\"}") + "." + b64(payload) + ".c2ln"
			+ "~" + disclosure.getDisclosure()
			+ "~" + b64("{\"alg\":\"ES256\",\"typ\":\"kb+jwt\"}") + "." + b64("{\"nonce\":\"n\"}") + ".c2ln";
	}

	@Test
	public void testEvaluate_sha3SdAlg() {
		env.putString("credential", sdJwtWithSdAlg("\"sha3-256\"", "sha3-256"));

		cond.execute(env);

		assertEquals("user@example.com", env.getString("sdjwt", "decoded.email"));
	}

	@Test
	public void testEvaluate_sdAlgIsCaseSensitive() {
		env.putString("credential", sdJwtWithSdAlg("\"SHA-256\"", "sha-256"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unregisteredSdAlgThrows() {
		// the JVM can compute md5, but it is not a registered hash name
		env.putString("credential", sdJwtWithSdAlg("\"md5\"", "md5"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_unknownSdAlgThrows() {
		env.putString("credential", sdJwtWithSdAlg("\"not-a-hash\"", "sha-256"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_nonStringSdAlgThrows() {
		env.putString("credential", sdJwtWithSdAlg("256", "sha-256"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
