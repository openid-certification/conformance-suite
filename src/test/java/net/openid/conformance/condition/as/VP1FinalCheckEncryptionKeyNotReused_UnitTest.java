package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
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
public class VP1FinalCheckEncryptionKeyNotReused_UnitTest {

	// Two distinct, well-formed P-256 public keys (coordinates differ, so RFC 7638 thumbprints differ).
	private static final String KEY_A = """
		{ "kty": "EC", "crv": "P-256",
		  "x": "_SlKY_V2SpmRPHI7zQNDcSLKRyvI1_k3SMh7XF-kgeM",
		  "y": "MOAKQxM7pA9dcrqGyP8WoLvk0hxqk_p71Pm_HFY0cj8",
		  "use": "enc", "alg": "ECDH-ES", "kid": "key-a" }""";

	private static final String KEY_B = """
		{ "kty": "EC", "crv": "P-256",
		  "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
		  "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0",
		  "use": "enc", "alg": "ECDH-ES", "kid": "key-b" }""";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VP1FinalCheckEncryptionKeyNotReused cond;

	@BeforeEach
	public void setUp() {
		RecentValueHistory.clear();
		cond = new VP1FinalCheckEncryptionKeyNotReused();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void setClientMetadataKeys(String... keysJson) {
		JsonObject request = new JsonObject();
		JsonObject clientMetadata = new JsonObject();
		JsonObject jwks = new JsonObject();
		jwks.add("keys", JsonParser.parseString("[" + String.join(",", keysJson) + "]"));
		clientMetadata.add("jwks", jwks);
		request.add("client_metadata", clientMetadata);
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, request);
	}

	@Test
	public void firstRequestSucceeds() {
		env.putString("owner_id", "user-a");
		setClientMetadataKeys(KEY_A);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void reuseOfSameKeyBySameUserFails() {
		env.putString("owner_id", "user-a");
		setClientMetadataKeys(KEY_A);
		assertDoesNotThrow(() -> cond.execute(env)); // first request records the key

		setClientMetadataKeys(KEY_A); // second request reuses the same key
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void sameKeyByDifferentUserSucceeds() {
		env.putString("owner_id", "user-a");
		setClientMetadataKeys(KEY_A);
		assertDoesNotThrow(() -> cond.execute(env));

		env.putString("owner_id", "user-b"); // a different suite user
		setClientMetadataKeys(KEY_A);
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void differentKeyBySameUserSucceeds() {
		env.putString("owner_id", "user-a");
		setClientMetadataKeys(KEY_A);
		assertDoesNotThrow(() -> cond.execute(env));

		setClientMetadataKeys(KEY_B); // fresh, ephemeral key
		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void missingOwnerScopeFails() {
		// No owner_id is a test suite bug (the module always surfaces it in configure()), so fail loudly.
		setClientMetadataKeys(KEY_A);
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void nonEncryptionKeyReuseIsIgnored() {
		// A key marked use=sig is not an encryption key, so reusing it does not trigger the check.
		String sigKey = KEY_A.replace("\"use\": \"enc\"", "\"use\": \"sig\"");
		env.putString("owner_id", "user-a");
		setClientMetadataKeys(sigKey);
		assertDoesNotThrow(() -> cond.execute(env));
		setClientMetadataKeys(sigKey);
		assertDoesNotThrow(() -> cond.execute(env));
	}
}
