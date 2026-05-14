package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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

@ExtendWith(MockitoExtension.class)
public class ConnectIdCibaSetBackchannelAuthenticationRequestSigningAlgValuesSupportedToPS256Only_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ConnectIdCibaSetBackchannelAuthenticationRequestSigningAlgValuesSupportedToPS256Only cond;

	@BeforeEach
	public void setUp() {
		cond = new ConnectIdCibaSetBackchannelAuthenticationRequestSigningAlgValuesSupportedToPS256Only();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_replacesExistingValuesWithPs256Only() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"backchannel_authentication_request_signing_alg_values_supported\": [\"PS256\", \"ES256\"]"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		JsonArray values = server.getAsJsonArray("backchannel_authentication_request_signing_alg_values_supported");
		assertEquals(1, values.size());
		assertEquals("PS256", OIDFJSON.getString(values.get(0)));
	}
}
