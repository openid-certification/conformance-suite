package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddUnusableKeysToServerPublicJwks_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddUnusableKeysToServerPublicJwks cond;

	@BeforeEach
	public void setUp() {
		cond = new AddUnusableKeysToServerPublicJwks();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void appendsUnusableKeysAndKeepsRealKey() {
		env.putObject("server_public_jwks", JsonParser.parseString("""
			{
			  "keys": [
			    {
			      "kty": "RSA",
			      "e": "AQAB",
			      "use": "sig",
			      "kid": "real-signing-key",
			      "alg": "RS256",
			      "n": "nJclr5TJ3Y21Ggt0lz2EO7wWKn6jTaIlMv1sNMy2VmkcSf8EVsFqJ1vSXjFxWvBj7RolFCyaChFwI_jog9c2rAkIwF8Voi5eB3PRjl3OaNRUYILRgLsaclTj02NWMvwbiJ18yJ63D4Ojzif8_RyAHuM3HO2rs6nPEyZMW3Xd0z3Lw099TpIcxA4Ktfo2DliUfMZh9s3lB_f6DSxX5Z9CXqrzNsoCCxqJZ55WuUUNA4LmYl5OgrH8sD7_TvY1QTjjmRzUptgj1S-gwagIjrkn9ooALa8gRN4etKztA2topBn0KO2VwEo_P4iejBn2Z3I2FlQnDNu0t7xNwBhsM2Vg8Q"
			    }
			  ]
			}""").getAsJsonObject());

		cond.execute(env);

		JsonArray keys = env.getObject("server_public_jwks").getAsJsonArray("keys");
		assertEquals(6, keys.size());
		assertEquals("real-signing-key", OIDFJSON.getString(keys.get(0).getAsJsonObject().get("kid")));

		JsonObject pqKey = keys.get(1).getAsJsonObject();
		assertEquals("unusable-pq-sig-key", OIDFJSON.getString(pqKey.get("kid")));
		assertEquals("AKP", OIDFJSON.getString(pqKey.get("kty")));
		assertEquals("sig", OIDFJSON.getString(pqKey.get("use")));

		JsonObject unknownKtyKey = keys.get(2).getAsJsonObject();
		assertEquals("unusable-unknown-kty-key", OIDFJSON.getString(unknownKtyKey.get("kid")));
		assertEquals("OIDF-CONFORMANCE-UNSUPPORTED", OIDFJSON.getString(unknownKtyKey.get("kty")));
		assertEquals("sig", OIDFJSON.getString(unknownKtyKey.get("use")));

		JsonObject rsaUnknownAlgKey = keys.get(3).getAsJsonObject();
		assertEquals("unusable-rsa-unknown-alg-key", OIDFJSON.getString(rsaUnknownAlgKey.get("kid")));
		assertEquals("RSA", OIDFJSON.getString(rsaUnknownAlgKey.get("kty")));
		assertEquals("RS9999", OIDFJSON.getString(rsaUnknownAlgKey.get("alg")));
		assertEquals("sig", OIDFJSON.getString(rsaUnknownAlgKey.get("use")));

		JsonObject ecUnknownAlgKey = keys.get(4).getAsJsonObject();
		assertEquals("unusable-ec-unknown-alg-key", OIDFJSON.getString(ecUnknownAlgKey.get("kid")));
		assertEquals("EC", OIDFJSON.getString(ecUnknownAlgKey.get("kty")));
		assertEquals("P-256", OIDFJSON.getString(ecUnknownAlgKey.get("crv")));
		assertEquals("ES9999", OIDFJSON.getString(ecUnknownAlgKey.get("alg")));
		assertEquals("sig", OIDFJSON.getString(ecUnknownAlgKey.get("use")));

		JsonObject ecUnknownCrvKey = keys.get(5).getAsJsonObject();
		assertEquals("unusable-ec-unknown-crv-key", OIDFJSON.getString(ecUnknownCrvKey.get("kid")));
		assertEquals("EC", OIDFJSON.getString(ecUnknownCrvKey.get("kty")));
		assertEquals("P-9999", OIDFJSON.getString(ecUnknownCrvKey.get("crv")));
		assertFalse(ecUnknownCrvKey.has("alg"));
		assertEquals("sig", OIDFJSON.getString(ecUnknownCrvKey.get("use")));
	}

	@Test
	public void failsWhenServerPublicJwksHasNoKeysArray() {
		env.putObject("server_public_jwks", JsonParser.parseString("{}").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
