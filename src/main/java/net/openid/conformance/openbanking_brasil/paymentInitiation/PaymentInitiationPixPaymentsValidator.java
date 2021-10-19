package net.openid.conformance.openbanking_brasil.paymentInitiation;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Doc https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_payments_apis.yaml
 * URL: /pix/payments/{paymentId}
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Payment Initiation Pix By PaymentId")
public class PaymentInitiationPixPaymentsValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> STATUS = Sets.newHashSet("PDNG", "PART", "ACSP", "ACSC", "ACCC", "RJCT");
	public static final Set<String> LOCAL_INSTRUMENTS = Sets.newHashSet("MANU", "DICT", "INIC", "QRDN", "QRES");
	public static final Set<String> REJECTION_REASON = Sets.newHashSet("ABORTED_SETTLEMENT_TIMEOUT",
		"ERROR_CREDITOR_AGENT", "TIMEOUT_DEBTOR_AGENT", "INVALID_CREDITOR_ACCOUNT_NUMBER",
		"BLOCKED_ACCOUNT", "CLOSED_CREDITOR_ACCOUNT_NUMBER", "INVALID_CREDITOR_ACCOUNTTYPE",
		"TRANSACTION_NOT_SUPPORTED", "NOT_ALLOWED_BOOK_TRANSFER", "FORBIDDEN_RETURN_PAYMENT",
		"INCORRECT_AGENT", "ZERO_AMOUNT", "NOT_ALLOWED_AMOUNT", "INSUFFICIENT_FUNDS",
		"WRONG_AMOUNT", "INVALID_AMOUNT", "INVALID_NUMBER_OF_TRANSACTIONS",
		"INCONSISTENT_WITH_END_CUSTOMER", "INVALID_IDENTIFICATION_CODE",
		"INVALID_CREDITOR_IDENTIFICATION_CODE", "CREDITOR_IDENTIFIER_INCORRECT",
		"ELEMENT_CONTENT_FORMALLY_INCORRECT", "ORDER_REJECTED", "NOT_ALLOWED_PAYMENT",
		"NOT_ALLOWED_ACCOUNT", "USER_NOT_YET_ACTIVATED", "INVALID_CREATION_DATE",
		"INVALID_CUT_OFF_DATE", "SETTLEMENT_FAILED", "INVALID_PURPOSE", "INVALID_END_TO_END_ID",
		"INVALID_DEBTOR_CLEARING_SYSTEM_MEMBER_IDENTIFIER",
		"INVALID_CREDITOR_CLEARING_SYSTEM_MEMBER_IDENTIFIER", "REGULATORY_REASON",
		"SPECIFIC_SERVICE_OFFERED_BY_CREDITOR_AGENT", "INVALID_BILL", "OPERATION_WINDOW",
		"INCOMPATIBLE_DATE", "MISMATCH_AMOUNT", "OVER_LIMIT", "INVALID_CONSENT",
		"DENIED_MULTIPLE_AUTHORISATIONS", "EXPIRED_MULTIPLE_AUTHORISATIONS", "EXPIRED_BILL");

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = environment.getObject("consent_endpoint_response");
		assertHasField(body, ROOT_PATH);
		assertJsonObject(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		assertField(body,
			new StringField
				.Builder("paymentId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMinLength(1)
				.setMaxLength(100)
				.build());

		assertField(body,
			new StringField
				.Builder("endToEndId")
				.setPattern("^([E])([0-9]{8})([0-9]{4})(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])(2[0-3]|[01][0-9])([0-5][0-9])([a-zA-Z0-9]{11})$")
				.setMinLength(32)
				.setMaxLength(32)
				.setOptional()
				.build());

		assertField(body, CommonFields.consentId());

		assertField(body,
			new DatetimeField
				.Builder("creationDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("statusUpdateDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new StringField
				.Builder("proxy")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(77)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("ibgeTownCode")
				.setMinLength(7)
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(STATUS)
				.setMaxLength(4)
				.build());

		assertField(body,
			new StringField
				.Builder("rejectionReason")
				.setMaxLength(50)
				.setEnums(REJECTION_REASON)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("localInstrument")
				.setMaxLength(4)
				.setEnums(LOCAL_INSTRUMENTS)
				.setOptional()
				.build());

		assertJsonObject(body, "payment", this::assertPayment);
		if (body.has("localInstrument") && OIDFJSON.getString(body.get("localInstrument")).equals("INIC")) {
			assertField(body,
				new StringField
					.Builder("transactionIdentification")
					.setPattern("^[a-zA-Z0-9][a-zA-Z0-9]{0,24}$")
					.setMaxLength(25)
					.build());
		} else if (body.has("localInstrument") && (OIDFJSON.getString(body.get("localInstrument")).equals("MANU"))) {
			assertField(body,
				new StringField
					.Builder("transactionIdentification")
					.setPattern("^(?![\\s\\S])")
					.setMaxLength(0)
					.build());
		} else {
			assertField(body,
				new StringField
					.Builder("transactionIdentification")
					.setPattern("^[a-zA-Z0-9][a-zA-Z0-9]{0,24}$")
					.setMaxLength(25)
					.setOptional()
					.build());
		}

		assertField(body,
			new StringField
				.Builder("remittanceInformation")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(140)
				.setOptional()
				.build());

		assertField(body,
			new StringField
				.Builder("cnpjInitiator")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertJsonObject(body, "creditorAccount", this::assertCreditorAccount);
	}

	private void assertPayment(JsonObject payment) {
		assertField(payment,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(19)
				.setPattern("^((\\d{1,16}\\.\\d{2}))$")
				.build());

		assertField(payment,
			new StringField
				.Builder("currency")
				.setPattern("^([A-Z]{3})$")
				.setMaxLength(3)
				.build());
	}

	private void assertCreditorAccount(JsonObject creditorAccount) {
		Set<String> accountTypes = Sets.newHashSet("CACC", "SLRY", "SVGS", "TRAN");

		assertField(creditorAccount,
			new StringField
				.Builder("ispb")
				.setPattern("^[0-9]{8}$")
				.setMaxLength(8)
				.setMinLength(8)
				.build());

		assertField(creditorAccount,
			new StringField
				.Builder("issuer")
				.setPattern("^\\d{4}$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(creditorAccount,
			new StringField
				.Builder("number")
				.setPattern("^\\d{3,20}$")
				.setMaxLength(20)
				.setMinLength(3)
				.build());

		assertField(creditorAccount,
			new StringField
				.Builder("accountType")
				.setEnums(accountTypes)
				.setMaxLength(4)
				.build());
	}
}
