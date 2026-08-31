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
	}

	@Test
	public void absentRequestedExpiryRequiresDataConsentMaximum() {
		putResponseExpiresIn(86_400);
		condition.execute(env);
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
	public void missingExpiresInFails() {
		env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in is missing or is not a JSON number");
	}

	@Test
	public void stringExpiresInFails() {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", "86400");
		env.putObject("backchannel_authentication_endpoint_response", response);

		assertThatThrownBy(() -> condition.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("expires_in is missing or is not a JSON number");
	}

	private void putResponseExpiresIn(int expiresIn) {
		JsonObject response = new JsonObject();
		response.addProperty("expires_in", expiresIn);
		env.putObject("backchannel_authentication_endpoint_response", response);
	}
}
