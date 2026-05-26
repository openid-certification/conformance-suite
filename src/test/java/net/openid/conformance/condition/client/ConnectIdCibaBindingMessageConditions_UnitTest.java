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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ConnectIdCibaBindingMessageConditions_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@BeforeEach
	public void setUp() {
		env.putObject("request_object_claims", new JsonObject());
	}

	@Test
	public void testSetConnectIdBindingMessageToPurpose() {
		SetConnectIdBindingMessageToPurpose cond = new SetConnectIdBindingMessageToPurpose();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("requested_binding_message"))
			.isEqualTo(SetConnectIdBindingMessageToPurpose.CONNECTID_PURPOSE);
	}

	@Test
	public void testRemoveBindingMessageFromRequestObject() {
		env.getObject("request_object_claims").addProperty("binding_message", "purpose");

		RemoveBindingMessageFromRequestObject cond = new RemoveBindingMessageFromRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("binding_message")).isFalse();
	}

	@Test
	public void testSetRequestObjectBindingMessageToTooShortPurpose() {
		SetRequestObjectBindingMessageToTooShortPurpose cond = new SetRequestObjectBindingMessageToTooShortPurpose();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("request_object_claims", "binding_message"))
			.isEqualTo(SetRequestObjectBindingMessageToTooShortPurpose.TOO_SHORT_PURPOSE);
	}

	@Test
	public void testSetRequestObjectBindingMessageToTooLongPurpose() {
		SetRequestObjectBindingMessageToTooLongPurpose cond = new SetRequestObjectBindingMessageToTooLongPurpose();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("request_object_claims", "binding_message"))
			.isEqualTo(SetRequestObjectBindingMessageToTooLongPurpose.TOO_LONG_PURPOSE);
	}

	@Test
	public void testWarnIfRequestObjectClaimsBindingMessageIsNotAscii() {
		env.getObject("request_object_claims").addProperty("binding_message", "ASCII purpose");

		WarnIfRequestObjectClaimsBindingMessageIsNotAscii cond =
			new WarnIfRequestObjectClaimsBindingMessageIsNotAscii();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);
	}

	@Test
	public void testWarnIfRequestObjectClaimsBindingMessageIsNotAsciiThrowsForNonAscii() {
		env.getObject("request_object_claims").addProperty("binding_message",
			SetRequestObjectBindingMessageToNonAsciiPurpose.NON_ASCII_PURPOSE);

		WarnIfRequestObjectClaimsBindingMessageIsNotAscii cond =
			new WarnIfRequestObjectClaimsBindingMessageIsNotAscii();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'binding_message' contains non-ASCII characters");
	}
}
