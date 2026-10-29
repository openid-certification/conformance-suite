package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class VP1FinalEnsureMinimumNonceEntropy_UnitTest {

	private static final int RANDOM_SAMPLES = 10_000;

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private final SecureRandom random = new SecureRandom();

	private void evaluate(String nonce) {
		// a fresh instance per call: a condition aborts after logging 10,000 entries
		VP1FinalEnsureMinimumNonceEntropy cond = new VP1FinalEnsureMinimumNonceEntropy();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("nonce", nonce);
		cond.execute(env);
	}

	private byte[] randomBytes(int n) {
		byte[] b = new byte[n];
		random.nextBytes(b);
		return b;
	}

	@Test
	public void testEvaluate_base64url16BytesFromLaunchpadLog() {
		// measures 92.1 bits; warned under the previous 96-bit threshold (issue #2060)
		evaluate("K7W5q0R3JQ_7Pc2-cuLg9Q");
	}

	@Test
	public void testEvaluate_base64url16BytesWithRepeatedCharacters() {
		// a random 16-byte value that measures 76.6 bits, in the lowest 0.05% of random 22-character values
		evaluate("-Q-ZF8w-3YFTwG8_ZuF-Ew");
	}

	@Test
	public void testEvaluate_randomBase64url16Bytes() {
		Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
		for (int i = 0; i < RANDOM_SAMPLES; i++) {
			evaluate(enc.encodeToString(randomBytes(16)));
		}
	}

	@Test
	public void testEvaluate_randomHex16Bytes() {
		HexFormat hex = HexFormat.of();
		for (int i = 0; i < RANDOM_SAMPLES; i++) {
			evaluate(hex.formatHex(randomBytes(16)));
		}
	}

	@Test
	public void testEvaluate_randomUuidV4() {
		for (int i = 0; i < RANDOM_SAMPLES; i++) {
			evaluate(UUID.randomUUID().toString());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"aaaaaaaaaaaaaaaaaaaaaa",
		"abababababababababababab",
		"nonce-nonce-nonce-nonce",
		"1234567890123456",
		"00000000000000000000000000000001",
		"aaaabbbbccccddddeeeeffff"
	})
	public void testEvaluate_lowEntropyNonce(String nonce) {
		assertThrows(ConditionError.class, () -> evaluate(nonce));
	}

	@Test
	public void testEvaluate_emptyNonce() {
		assertThrows(ConditionError.class, () -> evaluate(""));
	}
}
