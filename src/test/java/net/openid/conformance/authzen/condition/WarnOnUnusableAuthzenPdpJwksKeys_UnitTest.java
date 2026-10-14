package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WarnOnUnusableAuthzenPdpJwksKeys_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnOnUnusableAuthzenPdpJwksKeys cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnOnUnusableAuthzenPdpJwksKeys();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putString("jwks_source_label", "PDP signing keys");
	}

	private void put(String jwks) {
		env.putObject("jwks_to_validate", JsonParser.parseString(jwks).getAsJsonObject());
	}

	@Test
	public void hmacOctKeys_areUsable() {
		put("""
			{ "keys": [
				{ "kty": "oct", "alg": "HS256", "use": "sig", "k": "c2VjcmV0" },
				{ "kty": "oct", "alg": "HS384", "k": "c2VjcmV0" },
				{ "kty": "oct", "alg": "HS512", "k": "c2VjcmV0" } ] }""");
		cond.execute(env);
	}

	@Test
	public void asymmetricKey_isUsable() {
		put("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "alg": "ES256", "x": "AA", "y": "AA" } ] }""");
		cond.execute(env);
	}

	@Test
	public void unknownAlgorithmOnOctKey_isStillReported() {
		put("""
			{ "keys": [ { "kty": "oct", "alg": "HS999", "k": "c2VjcmV0" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("HS999"), e.getMessage());
	}

	@Test
	public void hmacAlgorithmOnNonOctKey_isStillReported() {
		put("""
			{ "keys": [ { "kty": "RSA", "alg": "HS256", "n": "AQAB", "e": "AQAB" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void unsupportedKeyType_isStillReported() {
		put("""
			{ "keys": [
				{ "kty": "oct", "alg": "HS256", "k": "c2VjcmV0" },
				{ "kty": "AKP", "alg": "ML-DSA-NONEXISTENT", "pub": "AA" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("index 1"), e.getMessage());
	}
}
