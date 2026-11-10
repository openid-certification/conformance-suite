package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializeAccountsEndpointResponseBody_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void serializesTheResponseObjectCompactly() {
		Environment env = new Environment();
		JsonObject response = JsonParser.parseString("{ \"data\": { \"accountId\": \"CL-0001\", \"name\": \"Señora Pérez\" }, \"meta\": {} }").getAsJsonObject();
		env.putObject("accounts_endpoint_response", response);
		SerializeAccountsEndpointResponseBody cond = new SerializeAccountsEndpointResponseBody();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.FAILURE);

		cond.execute(env);

		String body = env.getString(SerializeAccountsEndpointResponseBody.ACCOUNTS_RESPONSE_BODY);
		assertThat(body).isEqualTo("{\"data\":{\"accountId\":\"CL-0001\",\"name\":\"Señora Pérez\"},\"meta\":{}}");
		assertThat(JsonParser.parseString(body)).isEqualTo(response);
	}
}
