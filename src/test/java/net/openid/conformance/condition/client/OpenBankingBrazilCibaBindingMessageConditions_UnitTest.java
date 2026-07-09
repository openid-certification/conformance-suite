package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenBankingBrazilCibaBindingMessageConditions_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
	}

	@Test
	public void setsUrlBindingMessageOnAuthorizationEndpointRequest() {
		SetAuthorizationEndpointRequestBindingMessageToUrl cond =
			new SetAuthorizationEndpointRequestBindingMessageToUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "binding_message"))
			.isEqualTo(SetAuthorizationEndpointRequestBindingMessageToUrl.URL_BINDING_MESSAGE);
	}

	@Test
	public void warningConditionSucceedsWhenBindingMessageIsMissing() {
		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		cond.execute(env);
	}

	@Test
	public void warningConditionSucceedsWhenBindingMessageHasNoUrl() {
		env.putString("authorization_endpoint_request", "binding_message", "Review consent details");

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		cond.execute(env);
	}

	@Test
	public void warningConditionThrowsWhenBindingMessageContainsUrl() {
		env.putString("authorization_endpoint_request", "binding_message",
			SetAuthorizationEndpointRequestBindingMessageToUrl.URL_BINDING_MESSAGE);

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("contains a URL");
	}

	@Test
	public void warningConditionThrowsWhenBindingMessageContainsWwwUrl() {
		env.putString("authorization_endpoint_request", "binding_message", "Review www.example.test/consent");

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("contains a URL");
	}

	@Test
	public void warningConditionThrowsWhenBindingMessageContainsBareHostPath() {
		env.putString("authorization_endpoint_request", "binding_message", "Review example.test/consent");

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("contains a URL");
	}

	@Test
	public void warningConditionThrowsWhenBindingMessageContainsMailtoUrl() {
		env.putString("authorization_endpoint_request", "binding_message", "Contact mailto:support@example.test");

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("contains a URL");
	}

	@Test
	public void warningConditionThrowsWhenBindingMessageIsNotAString() {
		env.getObject("authorization_endpoint_request").add("binding_message", new JsonObject());

		WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl cond =
			new WarnIfAuthorizationEndpointRequestBindingMessageContainsUrl();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'binding_message' in authorization endpoint request is not a string");
	}
}
