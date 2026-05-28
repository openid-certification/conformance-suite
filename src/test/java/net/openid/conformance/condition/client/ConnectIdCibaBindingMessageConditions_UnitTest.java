package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jwt.SignedJWT;
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
	public void testSetConnectIdCibaLoginHintToCardPrimaryAccountNumber() {
		env.putObject("config", new JsonObject());

		SetConnectIdCibaLoginHintToCardPrimaryAccountNumber cond =
			new SetConnectIdCibaLoginHintToCardPrimaryAccountNumber();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("config", "client.hint_type")).isEqualTo("login_hint");
		assertThat(env.getString("config", "client.hint_value"))
			.isEqualTo(SetConnectIdCibaLoginHintToCardPrimaryAccountNumber.CARD_PRIMARY_ACCOUNT_NUMBER);
	}

	@Test
	public void testSetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber() {
		SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber cond =
			new SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("request_object_claims", "login_hint"))
			.isEqualTo(SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber.MALFORMED_CARD_PRIMARY_ACCOUNT_NUMBER);
	}

	@Test
	public void testSetRequestObjectHintToIdTokenHint() throws Exception {
		env.getObject("request_object_claims").addProperty("login_hint", "6372069742108725");

		SetRequestObjectHintToIdTokenHint cond = new SetRequestObjectHintToIdTokenHint();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("login_hint")).isFalse();
		assertThat(env.getString("request_object_claims", "id_token_hint"))
			.isEqualTo(SetRequestObjectHintToIdTokenHint.UNSUPPORTED_ID_TOKEN_HINT);
		assertThat(SignedJWT.parse(env.getString("request_object_claims", "id_token_hint"))).isNotNull();
	}

	@Test
	public void testSetRequestObjectHintToLoginHintToken() {
		env.getObject("request_object_claims").addProperty("login_hint", "6372069742108725");

		SetRequestObjectHintToLoginHintToken cond = new SetRequestObjectHintToLoginHintToken();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getObject("request_object_claims").has("login_hint")).isFalse();
		assertThat(env.getString("request_object_claims", "login_hint_token"))
			.isEqualTo(SetRequestObjectHintToLoginHintToken.UNSUPPORTED_LOGIN_HINT_TOKEN);
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
