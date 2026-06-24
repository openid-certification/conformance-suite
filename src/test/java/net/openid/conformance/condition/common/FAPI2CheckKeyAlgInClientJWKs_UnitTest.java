package net.openid.conformance.condition.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FAPI2CheckKeyAlgInClientJWKs_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private FAPI2CheckKeyAlgInClientJWKs cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new FAPI2CheckKeyAlgInClientJWKs();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addClientJwks(JsonObject key) {
		JsonArray keys = new JsonArray();
		keys.add(key);
		JsonObject jwks = new JsonObject();
		jwks.add("keys", keys);
		env.putObject("client_jwks", jwks);
	}

	private JsonObject keyWithAlgAndCrv(String alg, String crv) {
		JsonObject key = new JsonObject();
		key.addProperty("kty", "OKP");
		key.addProperty("use", "sig");
		key.addProperty("alg", alg);
		if (crv != null) {
			key.addProperty("crv", crv);
		}
		return key;
	}

	@Test
	public void ed25519AlgWithEd25519CurvePasses() {
		addClientJwks(keyWithAlgAndCrv("Ed25519", "Ed25519"));
		cond.execute(env);
	}

	@Test
	public void eddsaAlgWithEd25519CurveStillPasses() {
		addClientJwks(keyWithAlgAndCrv("EdDSA", "Ed25519"));
		cond.execute(env);
	}

	@Test
	public void ed25519AlgWithMissingCrvFails() {
		assertThrows(ConditionError.class, () -> {
			addClientJwks(keyWithAlgAndCrv("Ed25519", null));
			cond.execute(env);
		});
	}

	@Test
	public void ed25519AlgWithEd448CurveFails() {
		assertThrows(ConditionError.class, () -> {
			addClientJwks(keyWithAlgAndCrv("Ed25519", "Ed448"));
			cond.execute(env);
		});
	}

	@Test
	public void nonPermittedAlgFails() {
		assertThrows(ConditionError.class, () -> {
			addClientJwks(keyWithAlgAndCrv("RS256", null));
			cond.execute(env);
		});
	}
}
