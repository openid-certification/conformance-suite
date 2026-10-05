package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class WarnIfSdJwtDigestAlgorithmNotSha256_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnIfSdJwtDigestAlgorithmNotSha256 cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnIfSdJwtDigestAlgorithmNotSha256();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void credentialWithClaims(String claimsJson) {
		env.putObject("sdjwt", JsonParser.parseString("{\"credential\":{\"claims\":" + claimsJson + "}}").getAsJsonObject());
	}

	@Test
	public void testEvaluate_noSdAlgMeansSha256() {
		credentialWithClaims("{\"vct\":\"urn:eudi:pid:1\"}");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_explicitSha256() {
		credentialWithClaims("{\"_sd_alg\":\"sha-256\"}");

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void testEvaluate_sha384Throws() {
		credentialWithClaims("{\"_sd_alg\":\"sha-384\"}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_sdAlgIsCaseSensitive() {
		// RFC 9901 section 4.1.1: the value is a case-sensitive string, so this is not sha-256
		credentialWithClaims("{\"_sd_alg\":\"SHA-256\"}");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
