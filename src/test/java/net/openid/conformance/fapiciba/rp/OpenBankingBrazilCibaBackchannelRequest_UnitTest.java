package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenBankingBrazilCibaBackchannelRequest_UnitTest {

	private Environment env;
	private EnsureBackchannelRequestObjectDoesNotContainUserCode condition;
	private EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl bindingMessageCondition;

	@BeforeEach
	public void setup() {
		env = mock(Environment.class);
		condition = new EnsureBackchannelRequestObjectDoesNotContainUserCode();
		condition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		bindingMessageCondition = new EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl();
		bindingMessageCondition.setProperties("testId", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
	}

	@Test
	public void succeedsWhenUserCodeIsMissing() {
		condition.evaluate(env);
	}

	@Test
	public void failsWhenUserCodeIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.user_code"))
			.thenReturn(new JsonPrimitive("123456"));

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not permitted for Open Finance Brasil CIBA");
	}

	@Test
	public void failsWhenUserCodeIsJsonNull() {
		when(env.getElementFromObject("backchannel_request_object", "claims.user_code"))
			.thenReturn(JsonNull.INSTANCE);

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not permitted for Open Finance Brasil CIBA");
	}

	@Test
	public void failsWhenUserCodeIsAnObject() {
		when(env.getElementFromObject("backchannel_request_object", "claims.user_code"))
			.thenReturn(new JsonObject());

		assertThatThrownBy(() -> condition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("not permitted for Open Finance Brasil CIBA");
	}

	@Test
	public void bindingMessageCheckSucceedsWhenMissing() {
		bindingMessageCondition.evaluate(env);
	}

	@Test
	public void bindingMessageCheckSucceedsWhenNoUrlIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Review consent details"));

		bindingMessageCondition.evaluate(env);
	}

	@Test
	public void bindingMessageCheckFailsWhenUrlIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Review https://example.test/consent"));

		assertThatThrownBy(() -> bindingMessageCondition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain URLs");
	}

	@Test
	public void bindingMessageCheckLogsPrivacySafeUrlDiagnostic() {
		String url = "example.test/consent?account=customer-42";
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Review " + url));
		TestInstanceEventLog diagnosticLog = mock(TestInstanceEventLog.class);
		EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl diagnosticCondition =
			new EnsureBackchannelRequestObjectBindingMessageDoesNotContainUrl();
		diagnosticCondition.setProperties("testId", diagnosticLog, Condition.ConditionResult.FAILURE);

		assertThatThrownBy(() -> diagnosticCondition.evaluate(env)).isInstanceOf(ConditionError.class);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(diagnosticLog).log(anyString(), captor.capture());
		assertThat(captor.getValue())
			.containsEntry("binding_message_url_match_type", "host_path")
			.containsEntry("binding_message_url_match", "example.test/[redacted]")
			.containsEntry("binding_message_url_match_start", 7)
			.containsEntry("binding_message_url_match_length", url.length());
		assertThat(captor.getValue().toString())
			.doesNotContain("consent", "customer-42");
	}

	@Test
	public void bindingMessageCheckFailsWhenWwwUrlIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Review www.example.test/consent"));

		assertThatThrownBy(() -> bindingMessageCondition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain URLs");
	}

	@Test
	public void bindingMessageCheckFailsWhenBareHostPathIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Review example.test/consent"));

		assertThatThrownBy(() -> bindingMessageCondition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain URLs");
	}

	@Test
	public void bindingMessageCheckFailsWhenMailtoUrlIsPresent() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonPrimitive("Contact mailto:support@example.test"));

		assertThatThrownBy(() -> bindingMessageCondition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("must not contain URLs");
	}

	@Test
	public void bindingMessageCheckFailsWhenBindingMessageIsNotAString() {
		when(env.getElementFromObject("backchannel_request_object", "claims.binding_message"))
			.thenReturn(new JsonObject());

		assertThatThrownBy(() -> bindingMessageCondition.evaluate(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("binding_message must be a string when present");
	}

}
