package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.*;

import java.util.Set;

/**
 * Api: swagger_credit_cards_apis.yaml
 * Api endpoint: /accounts/{creditCardAccountId}/transactions
 * Api git hash: 127e9783733a0d53bde1239a0982644015abe4f1
 */

@ApiName("Credit Card Accounts Transaction")
public class CreditCardAccountsTransactionResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, ROOT_PATH);
		assertField(body,
			new ArrayField.Builder(ROOT_PATH)
				.setMinItems(1)
				.build());
		assertJsonArrays(body, ROOT_PATH, this::assertInnerFields);

		return environment;
	}

	private void assertInnerFields(JsonObject data) {
		Set<String> enumCreditDebitType = Sets.newHashSet("CREDITO", "DEBITO");

		assertField(data,
			new StringField
				.Builder("transactionId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("identificationNumber")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.build());

		assertField(data, CommonFields.lineName().build());

		assertField(data,
			new StringField
				.Builder("transactionName")
				.setPattern("[\\w\\W\\s]*")
				.setMaxLength(100)
				.build());

		assertField(data,
			new StringField
				.Builder("billId")
				.setPattern("^[a-zA-Z0-9][a-zA-Z0-9\\-]{0,99}$")
				.setMaxLength(100)
				.setOptional()
				.build());

		assertField(data,
			new StringField
				.Builder("creditDebitType")
				.setMaxLength(7)
				.setEnums(enumCreditDebitType)
				.build());

		assertField(data, CommonFields.transactionType().build());

		assertField(data,
			new StringField
				.Builder("transactionalAdditionalInfo")
				.setMaxLength(140)
				.build());

		assertField(data, CommonFields.paymentType().build());

		assertField(data, CommonFields.feeType().build());

		assertField(data,
			new StringField
				.Builder("feeTypeAdditionalInfo")
				.setMaxLength(140)
				.build());

		assertField(data, CommonFields.otherCreditsType().build());

		assertField(data,
			new StringField
				.Builder("otherCreditsAdditionalInfo")
				.setMaxLength(50)
				.build());

		assertField(data,
			new StringField
				.Builder("chargeIdentificator")
				.setMaxLength(50)
				.setPattern("[\\w\\W\\s]*")
				.build());

		assertField(data,
			new IntField
				.Builder("chargeNumber")
				.setMaxLength(2)
				.setNullable()
				.build());

		assertField(data,
			new DoubleField
				.Builder("brazilianAmount")
				.setMaxLength(20)
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setNullable()
				.build());

		assertField(data,
			new DoubleField
				.Builder("amount")
				.setPattern("^-?\\d{1,15}\\.\\d{2,4}$")
				.setMaxLength(20)
				.setNullable()
				.build());

		assertField(data,
			new StringField
				.Builder("currency")
				.setMaxLength(3)
				.setPattern("^(\\w{3}){1}$|^NA$")
				.build());

		assertField(data,
			new DatetimeField
				.Builder("transactionDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());

		assertField(data,
			new DatetimeField
				.Builder("billPostDate")
				.setPattern("^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$|^NA$")
				.build());

		assertField(data,
			new IntField
				.Builder("payeeMCC")
				.setNullable()
				.setMaxLength(4).build());
	}
}
