package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The caller runs this only when the PDP's discovery metadata carried no `signed_metadata`, and
 * invokes it with ConditionResult.WARNING — signed metadata is OPTIONAL, so a configured issuer or
 * key set with nothing to apply to is the tester's problem, not a PDP conformance failure.
 */
@ExtendWith(MockitoExtension.class)
class WarnUnusedSignedMetadataConfiguration_UnitTest {

	private static final String JWKS = """
		{ "keys": [ { "kty": "oct", "kid": "k1", "alg": "HS256", "k": "c2VjcmV0" } ] }""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private WarnUnusedSignedMetadataConfiguration cond;

	@BeforeEach
	public void setUp() {
		cond = new WarnUnusedSignedMetadataConfiguration();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
	}

	private void putConfig(JsonElement metadataIssuer, JsonElement jwks) {
		JsonObject config = new JsonObject();
		JsonObject pdp = new JsonObject();
		if (metadataIssuer != null) {
			pdp.add("metadata_issuer", metadataIssuer);
		}
		if (jwks != null) {
			pdp.add("jwks", jwks);
		}
		config.add("pdp", pdp);
		env.putObject("config", config);
	}

	private static JsonElement json(String value) {
		return JsonParser.parseString(value);
	}

	@Test
	public void notConfigured_succeeds() {
		putConfig(null, null);
		cond.execute(env);
	}

	@Test
	public void configuredButEmpty_succeeds() {
		putConfig(json("\"\""), json("{}"));
		cond.execute(env);
	}

	@Test
	public void emptyStringAndEmptyArrayJwks_succeeds() {
		putConfig(null, json("\"\""));
		cond.execute(env);
		putConfig(null, json("[]"));
		cond.execute(env);
	}

	@Test
	public void issuerWithWrongType_warns() {
		// Nothing has validated the field's type on this path — ValidatePDPMetadataIssuer only runs when
		// signed_metadata is present — so reading it as a string here would throw UnexpectedTypeException
		// and fail the test with an internal error instead of the intended warning.
		putConfig(json("42"), null);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'Signed Metadata Issuer' is configured"), e.getMessage());
	}

	@Test
	public void issuerWithNoSignedMetadata_warns() {
		putConfig(json("\"https://attester.example.com\""), null);
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'Signed Metadata Issuer' is configured"), e.getMessage());
		assertTrue(e.getMessage().contains("no `signed_metadata` for it to apply to"), e.getMessage());
		assertFalse(e.getMessage().contains("PDP JWK Set"), e.getMessage());
	}

	@Test
	public void jwksWithNoSignedMetadata_warns() {
		putConfig(null, json(JWKS));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP JWK Set' is configured"), e.getMessage());
		assertFalse(e.getMessage().contains("Signed Metadata Issuer"), e.getMessage());
	}

	@Test
	public void jwksWithWrongShape_warns() {
		// A single JWK rather than a JWK Set is still a filled-in field the run ignores.
		putConfig(null, json("{ \"kty\": \"oct\", \"k\": \"c2VjcmV0\" }"));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'PDP JWK Set' is configured"), e.getMessage());
	}

	@Test
	public void bothWithNoSignedMetadata_warnsOnceNamingBoth() {
		putConfig(json("\"https://attester.example.com\""), json(JWKS));
		Throwable e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("'Signed Metadata Issuer' and 'PDP JWK Set' are configured"), e.getMessage());
		assertTrue(e.getMessage().contains("for them to apply to"), e.getMessage());
	}
}
