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
class ParseUsableAuthzenPdpJwksKeys_UnitTest {

	// 32 bytes, 48 bytes and 64 bytes, base64url
	private static final String K_256 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY";
	private static final String K_384 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVm";
	private static final String K_512 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ParseUsableAuthzenPdpJwksKeys cond;

	@BeforeEach
	public void setUp() {
		cond = new ParseUsableAuthzenPdpJwksKeys();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putString("jwks_source_label", "PDP signing keys");
	}

	private void put(String jwks) {
		env.putObject("jwks_to_validate", JsonParser.parseString(jwks).getAsJsonObject());
	}

	@Test
	public void hmacKeysOfTheHashLength_parse() {
		put("""
			{ "keys": [
				{ "kty": "oct", "alg": "HS256", "k": "%s" },
				{ "kty": "oct", "alg": "HS384", "k": "%s" },
				{ "kty": "oct", "alg": "HS512", "k": "%s" } ] }""".formatted(K_256, K_384, K_512));
		cond.execute(env);
	}

	@Test
	public void hmacKeyWithoutK_fails() {
		put("""
			{ "keys": [ { "kty": "oct", "alg": "HS256", "kid": "k1" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("cannot be parsed"), e.getMessage());
	}

	@Test
	public void hmacKeyShorterThanTheHash_fails() {
		put("""
			{ "keys": [ { "kty": "oct", "alg": "HS512", "k": "%s" } ] }""".formatted(K_384));
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("384 bits long"), e.getMessage());
	}

	@Test
	public void unparseableKeysAreReportedInIndexOrder() {
		put("""
			{ "keys": [
				{ "kty": "oct", "alg": "HS256" },
				{ "kty": "EC", "crv": "P-256", "alg": "ES256", "x": "AA", "y": "AA" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("index 0"), e.getMessage());
	}

	@Test
	public void unparseableAsymmetricKey_fails() {
		put("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "alg": "ES256", "x": "AA", "y": "AA" } ] }""");
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void unusableKey_isLeftToTheWarning() {
		put("""
			{ "keys": [
				{ "kty": "oct", "alg": "HS256", "k": "%s" },
				{ "kty": "AKP", "alg": "ML-DSA-NONEXISTENT", "pub": "AA" } ] }""".formatted(K_256));
		cond.execute(env);
	}
}
