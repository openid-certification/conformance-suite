package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateVpClientMetadataJwksKeysArePublic_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVpClientMetadataJwksKeysArePublic cond;

	@BeforeEach
	public void setUp() {
		cond = new ValidateVpClientMetadataJwksKeysArePublic();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_validEcPublicKey() {
		JsonObject jwk = new JsonObject();
		jwk.addProperty("kty", "EC");
		jwk.addProperty("crv", "P-256");
		jwk.addProperty("x", "eUGfQbu08W7anOJOxiAUpoygQw88UESRBpoyvn1E5mw");
		jwk.addProperty("y", "YjKX1p9FTmdQPNDEIHmlBRM2-eYkT8Zj_VW59XHMdd8");
		jwk.addProperty("alg", "ECDH-ES");

		setUpClientMetadataWithJwks(jwk);
		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	@Test
	public void testEvaluate_privateKeyFails() {
		JsonObject jwk = new JsonObject();
		jwk.addProperty("kty", "EC");
		jwk.addProperty("crv", "P-256");
		jwk.addProperty("x", "eUGfQbu08W7anOJOxiAUpoygQw88UESRBpoyvn1E5mw");
		jwk.addProperty("y", "YjKX1p9FTmdQPNDEIHmlBRM2-eYkT8Zj_VW59XHMdd8");
		jwk.addProperty("d", "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA");

		setUpClientMetadataWithJwks(jwk);
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	public void testEvaluate_noJwksSkips() {
		JsonObject effectiveParams = new JsonObject();
		JsonObject clientMetadata = new JsonObject();
		effectiveParams.add("client_metadata", clientMetadata);
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveParams);

		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	@Test
	public void testEvaluate_emptyKeysSkips() {
		JsonObject jwks = new JsonObject();
		jwks.add("keys", new JsonArray());

		JsonObject clientMetadata = new JsonObject();
		clientMetadata.add("jwks", jwks);

		JsonObject effectiveParams = new JsonObject();
		effectiveParams.add("client_metadata", clientMetadata);
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveParams);

		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	private void setUpClientMetadataWithJwks(JsonObject jwk) {
		JsonArray keys = new JsonArray();
		keys.add(jwk);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keys);

		JsonObject clientMetadata = new JsonObject();
		clientMetadata.add("jwks", jwks);

		JsonObject effectiveParams = new JsonObject();
		effectiveParams.add("client_metadata", clientMetadata);
		env.putObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, effectiveParams);
	}
}
