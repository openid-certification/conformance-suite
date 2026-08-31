package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CdrValidateIdTokenSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CdrValidateIdTokenSigningAlg cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CdrValidateIdTokenSigningAlg();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void addIdTokenWithAlg(String alg) {
		// the condition only inspects the JOSE header, so a dummy payload/signature suffices
		String jwt = Base64URL.encode(("{\"alg\":\"" + alg + "\"}").getBytes(StandardCharsets.UTF_8))
			+ "." + Base64URL.encode("{}".getBytes(StandardCharsets.UTF_8))
			+ "." + Base64URL.encode(new byte[] { 1, 2, 3 });
		JsonObject idToken = new JsonObject();
		idToken.addProperty("value", jwt);
		env.putObject("id_token", idToken);
	}

	@Test
	public void testEvaluate_ps256IsGood() {
		addIdTokenWithAlg("PS256");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_es256IsGood() {
		addIdTokenWithAlg("ES256");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_edDsaFails() {
		assertThrows(ConditionError.class, () -> {
			addIdTokenWithAlg("EdDSA");
			cond.execute(env);
		});
	}

}
