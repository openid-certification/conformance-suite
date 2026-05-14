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
public class ConnectIdCibaSetBackchannelTokenDeliveryModesSupportedToPollOnly_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ConnectIdCibaSetBackchannelTokenDeliveryModesSupportedToPollOnly cond;

	@BeforeEach
	public void setUp() {
		cond = new ConnectIdCibaSetBackchannelTokenDeliveryModesSupportedToPollOnly();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_replacesExistingValuesWithPollOnly() {
		JsonObject server = JsonParser.parseString("{"
			+ "\"backchannel_token_delivery_modes_supported\": [\"ping\", \"poll\"]"
			+ "}")
			.getAsJsonObject();
		env.putObject("server", server);

		cond.execute(env);

		JsonArray values = server.getAsJsonArray("backchannel_token_delivery_modes_supported");
		assertEquals(1, values.size());
		assertEquals("poll", OIDFJSON.getString(values.get(0)));
	}
}
