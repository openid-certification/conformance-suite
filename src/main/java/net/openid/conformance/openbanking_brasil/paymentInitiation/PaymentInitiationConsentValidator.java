package net.openid.conformance.openbanking_brasil.paymentInitiation;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Doc https://openbanking-brasil.github.io/areadesenvolvedor/swagger/swagger_payments_apis.yaml
 * URL: /consents/{consentId}
 * Api git hash: 35bf08ce0287a76880add849532a9cf83d8ba558
 */

@ApiName("Payment Initiation Consent")
public class PaymentInitiationConsentValidator extends AbstractJsonAssertingCondition {

	public static final Set<String> STATUS_LIST = Sets.newHashSet("AWAITING_AUTHORISATION", "AUTHORISED", "REJECTED", "CONSUMED");
	public static final Set<String> PERSON_TYPES = Sets.newHashSet("PESSOA_NATURAL", "PESSOA_JURIDICA");
	public static final Set<String> TYPES = Sets.newHashSet("PIX");
	public static final Set<String> LOCAL_INSTRUMENTS = Sets.newHashSet("MANU", "DICT", "QRDN", "QRES", "INIC");
	public static final Set<String> ACCOUNT_TYPES = Sets.newHashSet("CACC", "SLRY", "SVGS", "TRAN");

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = environment.getObject("consent_endpoint_response");
		assertHasField(body, ROOT_PATH);
		assertField(body, new ObjectField.Builder(ROOT_PATH).setValidator(this::assertInnerFields).build());

		return environment;
	}

	private void assertInnerFields(JsonObject body) {

		assertField(body, CommonFields.consentId());

		assertField(body,
			new DatetimeField
				.Builder("creationDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new DatetimeField
				.Builder("expirationDateTime")
				.setSecondsOlderThan(300, "creationDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
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
			new DatetimeField
				.Builder("statusUpdateDateTime")
				.setPattern(DatetimeField.ALTERNATIVE_PATTERN)
				.build());

		assertField(body,
			new StringField
				.Builder("status")
				.setEnums(STATUS_LIST)
				.setMaxLength(22)
				.build());

		assertField(body, new ObjectField.Builder("loggedUser").setValidator(this::assertLoggedUser).build());

		assertField(body,
			new ObjectField
				.Builder("businessEntity")
				.setValidator(this::assertBusinessEntity)
				.setOptional()
				.setNullable()
				.build());

		assertField(body, new ObjectField.Builder("creditor").setValidator(this::assertCreditor).build());
		assertField(body, new ObjectField.Builder("payment").setValidator(this::assertPayment).build());

		assertField(body,
			new ObjectField
				.Builder("debtorAccount")
				.setValidator(this::assertDebtorAccount)
				.setOptional()
				.build());
	}

	private void assertLoggedUser(JsonObject loggedUser) {
		assertField(loggedUser, new ObjectField.Builder("document").setValidator(this::assertLoggedUserDocument).build());
	}

	private void assertLoggedUserDocument(JsonObject document) {
		assertField(document,
			new StringField
				.Builder("identification")
				.setPattern("^\\d{11}$")
				.setMaxLength(11)
				.build());

		assertField(document,
			new StringField
				.Builder("rel")
				.setPattern("^[A-Z]{3}$")
				.setMaxLength(3)
				.build());
	}

	private void assertBusinessEntity(JsonObject businessEntity) {
		assertField(businessEntity, new ObjectField.Builder("document").setValidator(this::assertBusinessEntityDocument).build());
	}

	private void assertBusinessEntityDocument(JsonObject document) {
		assertField(document,
			new StringField
				.Builder("identification")
				.setPattern("^\\d{14}$")
				.setMaxLength(14)
				.build());

		assertField(document,
			new StringField
				.Builder("rel")
				.setPattern("^[A-Z]{4}$")
				.setMaxLength(4)
				.build());
	}

	private void assertCreditor(JsonObject creditor) {

		assertField(creditor,
			new StringField
				.Builder("personType")
				.setEnums(PERSON_TYPES)
				.setMaxLength(15)
				.build());

		assertField(creditor,
			new StringField
				.Builder("cpfCnpj")
				.setPattern("^\\d{11}$|^\\d{14}$")
				.setMinLength(11)
				.setMaxLength(14)
				.build());

		assertField(creditor,
			new StringField
				.Builder("name")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(140)
				.build());
	}

	private void assertPayment(JsonObject payment) {

		assertField(payment,
			new StringField
				.Builder("type")
				.setMaxLength(3)
				.setEnums(TYPES)
				.build());

		assertField(payment,
			new DatetimeField
				.Builder("date")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$")
				.setMaxLength(10)
				.build());

		assertField(payment,
			new StringField
				.Builder("currency")
				.setPattern("^([A-Z]{3})$")
				.setMaxLength(3)
				.build());

		assertField(payment,
			new StringField
				.Builder("amount")
				.setMinLength(4)
				.setMaxLength(19)
				.setPattern("^((\\d{1,16}\\.\\d{2}))$")
				.build());

		assertField(payment,
			new StringField
				.Builder("ibgeTownCode")
				.setMinLength(7)
				.setMaxLength(7)
				.setPattern("^\\d{7}$")
				.setOptional()
				.build());

		assertField(payment,
			new ObjectField
				.Builder("details")
				.setValidator(this::assertDetails)
				.build());
	}

	private void assertDetails(JsonObject details) {

		assertField(details,
			new StringField
				.Builder("localInstrument")
				.setEnums(LOCAL_INSTRUMENTS)
				.setMaxLength(4)
				.build());

		if(OIDFJSON.getString(details.get("localInstrument")).equalsIgnoreCase("QRES") || OIDFJSON.getString(details.get("localInstrument")).equalsIgnoreCase("QRDN")){
			assertField(details,
				new StringField
					.Builder("qrCode")
					.setPattern("[\\w\\W\\s]*")
					.setMaxLength(512)
					.build());
		}

		if(!OIDFJSON.getString(details.get("localInstrument")).equalsIgnoreCase("MANU")) {
			assertField(details,
				new StringField
					.Builder("proxy")
					.setPattern("[\\w\\W\\s]*")
					.setMaxLength(77)
					.build());
		}

		assertField(details,
			new ObjectField
				.Builder("creditorAccount")
				.setValidator(this::assertDebtorAccount)
				.build());
	}

	private void assertDebtorAccount(JsonObject debtorAccount) {

		assertField(debtorAccount,
			new StringField
				.Builder("ispb")
				.setPattern("^[0-9]{8}$")
				.setMaxLength(8)
				.setMinLength(8)
				.build());

		assertField(debtorAccount,
			new StringField
				.Builder("issuer")
				.setPattern("^\\d{4}$")
				.setMaxLength(4)
				.setOptional()
				.build());

		assertField(debtorAccount,
			new StringField
				.Builder("number")
				.setPattern("^\\d{3,20}$")
				.setMaxLength(20)
				.setMinLength(3)
				.build());

		assertField(debtorAccount,
			new StringField
				.Builder("accountType")
				.setEnums(ACCOUNT_TYPES)
				.setMaxLength(4)
				.build());
	}
}
