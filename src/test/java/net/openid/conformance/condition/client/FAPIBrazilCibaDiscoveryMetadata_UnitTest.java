package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FAPIBrazilCibaDiscoveryMetadata_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void tokenDeliveryModesAcceptsPingOnly() {
		assertValid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(),
			"{\"backchannel_token_delivery_modes_supported\":[\"ping\"]}");
	}

	@Test
	public void tokenDeliveryModesRejectsMissingValue() {
		assertInvalid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(), "{}");
	}

	@Test
	public void tokenDeliveryModesRejectsWrongType() {
		assertInvalid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(),
			"{\"backchannel_token_delivery_modes_supported\":\"ping\"}");
	}

	@Test
	public void tokenDeliveryModesRejectsPollOnly() {
		assertInvalid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(),
			"{\"backchannel_token_delivery_modes_supported\":[\"poll\"]}");
	}

	@Test
	public void tokenDeliveryModesRejectsPingAndPoll() {
		assertInvalid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(),
			"{\"backchannel_token_delivery_modes_supported\":[\"ping\",\"poll\"]}");
	}

	@Test
	public void tokenDeliveryModesRejectsNonStringValue() {
		assertInvalid(new FAPIBrazilCibaCheckTokenDeliveryModesSupportedOnlyPing(),
			"{\"backchannel_token_delivery_modes_supported\":[true]}");
	}

	@Test
	public void userCodeSupportAcceptsMissingValueAsFalse() {
		assertValid(new FAPIBrazilCibaCheckUserCodeParameterNotSupported(), "{}");
	}

	@Test
	public void userCodeSupportAcceptsFalse() {
		assertValid(new FAPIBrazilCibaCheckUserCodeParameterNotSupported(),
			"{\"backchannel_user_code_parameter_supported\":false}");
	}

	@Test
	public void userCodeSupportRejectsTrue() {
		assertInvalid(new FAPIBrazilCibaCheckUserCodeParameterNotSupported(),
			"{\"backchannel_user_code_parameter_supported\":true}");
	}

	@Test
	public void userCodeSupportRejectsWrongType() {
		assertInvalid(new FAPIBrazilCibaCheckUserCodeParameterNotSupported(),
			"{\"backchannel_user_code_parameter_supported\":\"false\"}");
	}

	private void assertValid(AbstractCondition condition, String serverJson) {
		Environment env = environmentWithServer(serverJson);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertDoesNotThrow(() -> condition.execute(env));
	}

	private void assertInvalid(AbstractCondition condition, String serverJson) {
		Environment env = environmentWithServer(serverJson);
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	private Environment environmentWithServer(String serverJson) {
		Environment env = new Environment();
		JsonObject server = JsonParser.parseString(serverJson).getAsJsonObject();
		env.putObject("server", server);
		return env;
	}
}
