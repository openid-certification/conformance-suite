package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.OIDFJSON;
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
		env.putString("config", "client.card_primary_account_number", "5123450000000008");

		SetConnectIdCibaLoginHintToCardPrimaryAccountNumber cond =
			new SetConnectIdCibaLoginHintToCardPrimaryAccountNumber();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("config", "client.hint_type")).isEqualTo("login_hint");
		assertThat(env.getString("config", "client.hint_value"))
			.isEqualTo("5123450000000008");
	}

	@Test
	public void testSetConnectIdCibaLoginHintToCardPrimaryAccountNumberFailsWhenMissing() {
		env.putObject("config", new JsonObject());

		SetConnectIdCibaLoginHintToCardPrimaryAccountNumber cond =
			new SetConnectIdCibaLoginHintToCardPrimaryAccountNumber();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Card primary account number' field is missing from the 'Client' section in the test configuration");
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

	@Test
	public void testAddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject() {
		addValid3DSPaymentConfig();

		AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject cond =
			new AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		JsonArray authorizationDetailsArray = env.getObject("request_object_claims")
			.getAsJsonArray("authorization_details");
		JsonObject authorizationDetails = authorizationDetailsArray.get(0).getAsJsonObject();
		JsonObject instructedAmount = authorizationDetails.getAsJsonObject("instructed_amount");

		assertThat(authorizationDetailsArray).hasSize(1);
		assertThat(OIDFJSON.getString(authorizationDetails.get("type")))
			.isEqualTo(AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject.TYPE);
		assertThat(OIDFJSON.getDouble(instructedAmount.get("amount")))
			.isEqualTo(222.22);
		assertThat(OIDFJSON.getString(instructedAmount.get("currency")))
			.isEqualTo("AUD");
		assertThat(OIDFJSON.getString(authorizationDetails.get("source_account")))
			.isEqualTo("6372069742108725");
		assertThat(OIDFJSON.getString(authorizationDetails.get("beneficiary_name")))
			.isEqualTo("Byron Bay Cookies");
		assertThat(OIDFJSON.getString(authorizationDetails.get("payment_desc")))
			.isEqualTo("Online purchase of cookies with age verification");
	}

	@Test
	public void testAddConnectIdCiba3DSPaymentAuthorizationDetailsFailsWhenCardPanMissing() {
		env.putObject("config", new JsonObject());

		AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject cond =
			new AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Card primary account number' field is missing from the 'Client' section in the test configuration");
	}

	@Test
	public void testAddConnectIdCiba3DSPaymentAuthorizationDetailsFailsWhenPaymentAmountMissing() {
		addValid3DSPaymentConfig();
		env.getObject("config").getAsJsonObject("client").remove("payment_amount");

		AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject cond =
			new AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Payment amount' field is missing from the 'Client' section in the test configuration");
	}

	@Test
	public void testAddConnectIdCiba3DSPaymentAuthorizationDetailsFailsWhenPaymentAmountIsNotANumber() {
		addValid3DSPaymentConfig();
		env.putString("config", "client.payment_amount", "not-an-amount");

		AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject cond =
			new AddConnectIdCiba3DSPaymentAuthorizationDetailsToRequestObject();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> cond.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'Payment amount' field in the 'Client' section in the test configuration must be a number");
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentAuthorizationDetailsFromRequestObject() {
		runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentAuthorizationDetailsFromRequestObject());

		assertThat(env.getObject("request_object_claims").has("authorization_details")).isFalse();
	}

	@Test
	public void testSetConnectIdCiba3DSPaymentAuthorizationDetailsTypeToInvalid() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new SetConnectIdCiba3DSPaymentAuthorizationDetailsTypeToInvalid());

		assertThat(OIDFJSON.getString(authorizationDetails.get("type"))).isEqualTo("payment");
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentInstructedAmount() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentInstructedAmount());

		assertThat(authorizationDetails.has("instructed_amount")).isFalse();
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentAmount() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentAmount());

		assertThat(authorizationDetails.getAsJsonObject("instructed_amount").has("amount")).isFalse();
	}

	@Test
	public void testSetConnectIdCiba3DSPaymentAmountToInvalid() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new SetConnectIdCiba3DSPaymentAmountToInvalid());

		assertThat(OIDFJSON.getString(authorizationDetails.getAsJsonObject("instructed_amount").get("amount")))
			.isEqualTo("not-an-amount");
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentCurrency() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentCurrency());

		assertThat(authorizationDetails.getAsJsonObject("instructed_amount").has("currency")).isFalse();
	}

	@Test
	public void testSetConnectIdCiba3DSPaymentCurrencyToInvalid() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new SetConnectIdCiba3DSPaymentCurrencyToInvalid());

		assertThat(OIDFJSON.getString(authorizationDetails.getAsJsonObject("instructed_amount").get("currency")))
			.isEqualTo("NOT_A_CURRENCY");
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentSourceAccount() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentSourceAccount());

		assertThat(authorizationDetails.has("source_account")).isFalse();
	}

	@Test
	public void testSetConnectIdCiba3DSPaymentSourceAccountToInvalid() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new SetConnectIdCiba3DSPaymentSourceAccountToInvalid());

		assertThat(OIDFJSON.getString(authorizationDetails.get("source_account")))
			.isEqualTo(SetConnectIdCiba3DSPaymentSourceAccountToInvalid.INVALID_SOURCE_ACCOUNT);
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentBeneficiaryName() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentBeneficiaryName());

		assertThat(authorizationDetails.has("beneficiary_name")).isFalse();
	}

	@Test
	public void testRemoveConnectIdCiba3DSPaymentDesc() {
		JsonObject authorizationDetails = runInvalid3DSPaymentAuthorizationDetailsCondition(
			new RemoveConnectIdCiba3DSPaymentDesc());

		assertThat(authorizationDetails.has("payment_desc")).isFalse();
	}

	private JsonObject runInvalid3DSPaymentAuthorizationDetailsCondition(AbstractCondition cond) {
		addValid3DSPaymentConfig();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		cond.execute(env);

		if (!env.getObject("request_object_claims").has("authorization_details")) {
			return null;
		}
		return env.getObject("request_object_claims")
			.getAsJsonArray("authorization_details")
			.get(0)
			.getAsJsonObject();
	}

	private void addValid3DSPaymentConfig() {
		env.putObject("config", new JsonObject());
		env.putString("config", "client.card_primary_account_number", "6372069742108725");
		env.putString("config", "client.payment_amount", "222.22");
		env.putString("config", "client.payment_currency", "AUD");
		env.putString("config", "client.payment_beneficiary_name", "Byron Bay Cookies");
		env.putString("config", "client.payment_desc", "Online purchase of cookies with age verification");
	}
}
