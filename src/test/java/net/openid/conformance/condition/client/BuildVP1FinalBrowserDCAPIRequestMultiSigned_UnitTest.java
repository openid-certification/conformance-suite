package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class BuildVP1FinalBrowserDCAPIRequestMultiSigned_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private BuildVP1FinalBrowserDCAPIRequestMultiSigned cond;

	private JsonObject requestObjectJson;

	@BeforeEach
	public void setUp() {
		cond = new BuildVP1FinalBrowserDCAPIRequestMultiSigned();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Minimal JWS JSON Serialization structure
		requestObjectJson = JsonParser.parseString("""
			{
				"payload": "eyJub25jZSI6InRlc3QifQ",
				"signatures": [
					{
						"protected": "eyJhbGciOiJFUzI1NiJ9",
						"signature": "abc123"
					},
					{
						"protected": "eyJhbGciOiJFUzI1NiJ9",
						"signature": "def456"
					}
				]
			}
			""").getAsJsonObject();
	}

	@Test
	public void testEvaluate_success() {
		env.putObject("request_object_json", requestObjectJson);

		cond.execute(env);

		JsonObject result = env.getObject("browser_api_request");
		assertThat(result).isNotNull();

		// Verify structure: digital.requests[0].protocol and digital.requests[0].data.request
		JsonObject digital = result.getAsJsonObject("digital");
		assertThat(digital).isNotNull();

		JsonArray requests = digital.getAsJsonArray("requests");
		assertThat(requests).hasSize(1);

		JsonObject request = requests.get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(request.get("protocol"))).isEqualTo("openid4vp-v1-multisigned");

		JsonObject data = request.getAsJsonObject("data");
		assertThat(data).isNotNull();

		// data.request should be a JSON object (not a string) for multi-signed
		assertThat(data.get("request").isJsonObject()).isTrue();
		JsonObject requestObj = data.getAsJsonObject("request");
		assertThat(requestObj.has("payload")).isTrue();
		assertThat(requestObj.has("signatures")).isTrue();
	}

	@Test
	public void testEvaluate_missingRequestObjectJson() {
		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

}
