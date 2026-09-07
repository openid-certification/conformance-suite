package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The three algorithm-restricted variants of {@link OIDCCGenerateServerConfiguration} must all
 * include RS256, which OpenID Connect Discovery 1.0 section 3 requires to be present in
 * id_token_signing_alg_values_supported.
 */
@ExtendWith(MockitoExtension.class)
public class OIDCCGenerateServerConfigurationIdTokenSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() throws Exception {
		env.putString("base_url", "https://localhost.emobix.co.uk:8443/test/a/unit-test");
	}

	private List<String> algsFor(OIDCCGenerateServerConfiguration cond) {
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		cond.execute(env);

		JsonArray algs = env.getObject("server").getAsJsonArray("id_token_signing_alg_values_supported");
		List<String> values = new ArrayList<>();
		for (int i = 0; i < algs.size(); i++) {
			values.add(OIDFJSON.getString(algs.get(i)));
		}
		return values;
	}

	@Test
	public void testEvaluate_hs256() {
		assertEquals(List.of("HS256", "RS256"),
			algsFor(new OIDCCGenerateServerConfigurationIdTokenSigningAlgHS256AndRS256()));
	}

	@Test
	public void testEvaluate_es256() {
		assertEquals(List.of("ES256", "RS256"),
			algsFor(new OIDCCGenerateServerConfigurationIdTokenSigningAlgES256AndRS256()));
	}

	@Test
	public void testEvaluate_rs256() {
		assertEquals(List.of("RS256"),
			algsFor(new OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only()));
	}
}
