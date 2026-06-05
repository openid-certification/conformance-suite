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
public class ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll cond;

	@BeforeEach
	public void setUp() {
		cond = new ConnectIdCibaCheckBackchannelTokenDeliveryModesSupportedOnlyPoll();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_pollOnly() {
		env.putObject("server", serverWithDeliveryModes("[\"poll\"]"));
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingMetadataFails() {
		env.putObject("server", JsonParser.parseString("{}").getAsJsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_pingFails() {
		env.putObject("server", serverWithDeliveryModes("[\"ping\"]"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_pollAndPingFails() {
		env.putObject("server", serverWithDeliveryModes("[\"poll\", \"ping\"]"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private JsonObject serverWithDeliveryModes(String deliveryModes) {
		return JsonParser.parseString("{"
			+ "\"backchannel_token_delivery_modes_supported\": " + deliveryModes
			+ "}")
			.getAsJsonObject();
	}
}
