package net.openid.conformance.condition.client;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddUnusableEncryptionKeyToClientMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private AddUnusableEncryptionKeyToClientMetadata cond;

	@BeforeEach
	public void setUp() {
		cond = new AddUnusableEncryptionKeyToClientMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void putRequestWithUsableEncKey() {
		env.putObject("authorization_endpoint_request", JsonParser.parseString("""
			{
			  "client_metadata": {
			    "jwks": {
			      "keys": [
			        {
			          "crv": "P-256",
			          "kid": "usable-p256-key",
			          "kty": "EC",
			          "use": "enc",
			          "alg": "ECDH-ES",
			          "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			          "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4"
			        }
			      ]
			    }
			  }
			}""").getAsJsonObject());
	}

	@Test
	public void appendsUnusableKeysAndKeepsUsableKey() {
		putRequestWithUsableEncKey();

		cond.execute(env);

		JsonObject jwks = (JsonObject) env.getElementFromObject("authorization_endpoint_request", "client_metadata.jwks");
		JsonArray keys = jwks.getAsJsonArray("keys");

		assertEquals(3, keys.size());
		assertEquals("usable-p256-key", OIDFJSON.getString(keys.get(0).getAsJsonObject().get("kid")));

		JsonObject pqKey = keys.get(1).getAsJsonObject();
		assertEquals("unusable-pq-enc-key", OIDFJSON.getString(pqKey.get("kid")));
		assertEquals("AKP", OIDFJSON.getString(pqKey.get("kty")));
		assertEquals("enc", OIDFJSON.getString(pqKey.get("use")));

		JsonObject unknownKey = keys.get(2).getAsJsonObject();
		assertEquals("unusable-unknown-enc-key", OIDFJSON.getString(unknownKey.get("kid")));
		assertEquals("OIDF-CONFORMANCE-UNSUPPORTED", OIDFJSON.getString(unknownKey.get("kty")));
		assertEquals("enc", OIDFJSON.getString(unknownKey.get("use")));
	}

	@Test
	public void failsWhenClientMetadataJwksMissing() {
		env.putObject("authorization_endpoint_request", JsonParser.parseString("""
			{ "client_metadata": {} }""").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
