package net.openid.conformance.poppy.condition;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsurePoppyClientJwksContainsOnlyPublicKeys_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsurePoppyClientJwksContainsOnlyPublicKeys cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsurePoppyClientJwksContainsOnlyPublicKeys();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void run(String jwks) {
		JsonObject client = new JsonObject();
		client.add("jwks", PoppyTestData.json(jwks));
		env.putObject("client", client);
		cond.execute(env);
	}

	@Test
	public void publicKeyPasses() {
		assertDoesNotThrow(() -> run(PoppyTestData.PUBLIC_JWKS));
	}

	@Test
	public void privateKeyFails() {
		assertThrows(ConditionError.class, () -> run("""
			{"keys": [{"kty": "EC", "crv": "P-256", "kid": "k1", "d": "abc",
			  "x": "W7GYo8l5peUH8trIGV3JoJFe_5xoJ9j37oYNLPR0GGw", "y": "qO8C9AFSbtdIoKoFuOQ_pBBcPv05iL_ys1JiYf1GT-c"}]}
			"""));
	}

	@Test
	public void symmetricKeyFails() {
		assertThrows(ConditionError.class, () -> run("{\"keys\": [{\"kty\": \"oct\", \"k\": \"c2VjcmV0\", \"kid\": \"s\"}]}"));
	}
}
