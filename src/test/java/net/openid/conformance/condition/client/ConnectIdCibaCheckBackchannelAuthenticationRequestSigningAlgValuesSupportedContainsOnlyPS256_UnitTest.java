package net.openid.conformance.condition.client;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256 cond;

	@BeforeEach
	public void setUp() {
		cond = new ConnectIdCibaCheckBackchannelAuthenticationRequestSigningAlgValuesSupportedContainsOnlyPS256();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_ps256Only() {
		env.putObject("server", serverWithSigningAlgs("[\"PS256\"]"));
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingMetadataFails() {
		env.putObject("server", JsonParser.parseString("{}").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_es256Fails() {
		env.putObject("server", serverWithSigningAlgs("[\"ES256\"]"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_ps256AndEs256Fails() {
		env.putObject("server", serverWithSigningAlgs("[\"PS256\", \"ES256\"]"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private JsonObject serverWithSigningAlgs(String signingAlgs) {
		return JsonParser.parseString("{"
			+ "\"backchannel_authentication_request_signing_alg_values_supported\": " + signingAlgs
			+ "}")
			.getAsJsonObject();
	}
}
