package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ValidateOpenBankingBrazilCibaAuthenticationRequestExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateOpenBankingBrazilCibaAuthenticationRequestExpiresIn condition;

	@BeforeEach
	public void setUp() {
		condition = new ValidateOpenBankingBrazilCibaAuthenticationRequestExpiresIn();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("authorization_endpoint_request", new JsonObject());
		env.putObject("client", new JsonObject());
	}

	@Test
	public void absentRequestedExpiryMayUseProductSpecificMaximumBelowDefaultCeiling() {
		putResponseExpiresIn(3_600);
		condition.execute(env);
	}

	@Test
	public void configuredMaximumMakesAbsentRequestedExpiryExact() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "3600");
		putResponseExpiresIn(3_600);

		condition.execute(env);
	}

	@Test
	public void configuredMaximumRejectsLowerExpiresInWhenRequestedExpiryIsAbsent() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "3600");
		putResponseExpiresIn(3_599);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in does not match Open Finance Brasil requested_expiry rules");
	}

	@Test
	public void requestedExpiryAboveDefaultCeilingMayUseLowerProductSpecificMaximum() {
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", 86_401);
		putResponseExpiresIn(3_600);

		condition.execute(env);
	}

	@Test
	public void requestedExpiryAboveConfiguredMaximumRequiresExactMaximum() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "3600");
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", 3_601);
		putResponseExpiresIn(3_599);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in does not match Open Finance Brasil requested_expiry rules");
	}

	@Test
	public void expiresInAboveDefaultCeilingFailsWhenRequestedExpiryIsAbsent() {
		putResponseExpiresIn(86_401);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in is outside the permitted Open Finance Brasil range");
	}

	@Test
	public void nonPositiveExpiresInIsLeftToCoreValidation() {
		putResponseExpiresIn(0);

		condition.execute(env);
	}

	@Test
	public void invalidConfiguredMaximumFailsWithTestConfigurationLabel() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "not-an-integer");
		putResponseExpiresIn(3_600);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Brazil CIBA maximum expiry' field")
			.hasMessageContaining("in the test configuration");
	}

	@Test
	public void nonPositiveConfiguredMaximumFailsWithTestConfigurationLabel() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "0");
		putResponseExpiresIn(3_600);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Brazil CIBA maximum expiry' field")
			.hasMessageContaining("in the test configuration");
	}

	@Test
	public void requestedExpiryBelowMaximumMustBeReturned() {
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", 10);
		putResponseExpiresIn(10);
		condition.execute(env);
	}

	@Test
	public void requestedExpiryAsStringMustBeReturned() {
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", "30");
		putResponseExpiresIn(30);
		condition.execute(env);
	}

	@Test
	public void requestedExpiryAboveMaximumMustBeCapped() {
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", 86_401);
		putResponseExpiresIn(86_400);
		condition.execute(env);
	}

	@Test
	public void mismatchedExpiresInFails() {
		env.getObject("authorization_endpoint_request").addProperty("requested_expiry", 10);
		putResponseExpiresIn(11);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in does not match Open Finance Brasil requested_expiry rules");
	}

	@Test
	public void missingExpiresInIsLeftToCoreValidation() {
		env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

		condition.execute(env);
	}

	@Test
	public void stringExpiresInIsLeftToCoreValidation() {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", "86400");
		env.putObject("backchannel_authentication_endpoint_response", response);

		condition.execute(env);
	}

	@Test
	public void fractionalExpiresInIsLeftToCoreValidation() {
		env.getObject("client").addProperty("brazil_ciba_maximum_expiry", "3600");
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", 3_600.5);
		env.putObject("backchannel_authentication_endpoint_response", response);

		condition.execute(env);
	}

	private void putResponseExpiresIn(int expiresIn) {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", expiresIn);
		env.putObject("backchannel_authentication_endpoint_response", response);
	}
}
