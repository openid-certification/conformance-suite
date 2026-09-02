package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum condition;

	@BeforeEach
	public void setUp() {
		condition = new EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("client", new JsonObject());
		JsonObject config = new JsonObject();
		config.add("client", new JsonObject());
		env.putObject("config", config);
	}

	@Test
	public void acceptsExpiresInAtConfiguredMaximum() {
		setConfiguredMaximum(3_600);
		putResponseExpiresIn(3_600);

		condition.execute(env);
	}

	@Test
	public void rejectsExpiresInAboveConfiguredMaximum() {
		setConfiguredMaximum(3_600);
		putResponseExpiresIn(3_601);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in exceeds the Open Finance Brasil product or service maximum");
	}

	@Test
	public void rejectsExpiresInAboveDefaultMaximum() {
		putResponseExpiresIn(86_401);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in exceeds the Open Finance Brasil product or service maximum");
	}

	@Test
	public void leavesInvalidExpiresInToCibaCoreValidation() {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", "3600");
		env.putObject("backchannel_authentication_endpoint_response", response);

		condition.execute(env);
	}

	@Test
	public void rejectsInvalidConfiguredMaximumWithTestConfigurationLabel() {
		configuredClient().addProperty("brazil_ciba_maximum_expiry", "not-an-integer");
		putResponseExpiresIn(3_600);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Brazil CIBA maximum expiry' field")
			.hasMessageContaining("in the test configuration");
	}

	private void setConfiguredMaximum(int seconds) {
		configuredClient().addProperty("brazil_ciba_maximum_expiry", seconds);
	}

	private void putResponseExpiresIn(int seconds) {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", seconds);
		env.putObject("backchannel_authentication_endpoint_response", response);
	}

	private JsonObject configuredClient() {
		return env.getObject("config").getAsJsonObject("client");
	}
}
