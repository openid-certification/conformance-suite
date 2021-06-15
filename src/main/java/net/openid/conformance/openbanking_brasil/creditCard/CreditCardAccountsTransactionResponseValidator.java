package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Cartão de Crédito | Transações de cartão de crédito
 * https://openbanking-brasil.github.io/areadesenvolvedor/#limites-de-cartao-de-credito
 */

@ApiName("Credit Card Accounts Transaction")
public class CreditCardAccountsTransactionResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body,"$.data[0].identificationNumber");
		assertHasStringField(body,"$.data[0].transactionName");
		assertHasStringField(body,"$.data[0].creditDebitType");
		assertHasStringField(body,"$.data[0].transactionType");
		assertHasStringField(body,"$.data[0].transactionalAdditionalInfo");
		assertHasStringField(body,"$.data[0].paymentType");
		assertHasStringField(body,"$.data[0].feeType");
		assertHasStringField(body,"$.data[0].feeTypeAdditionalInfo");
		assertHasStringField(body,"$.data[0].otherCreditsAdditionalInfo");
		assertHasStringField(body,"$.data[0].otherCreditsType");
		assertHasStringField(body,"$.data[0].chargeIdentificator");
		assertHasIntField(body,"$.data[0].chargeNumber");
		assertHasDoubleField(body,"$.data[0].brazilianAmount");
		assertHasDoubleField(body,"$.data[0].amount");
		assertHasStringField(body,"$.data[0].currency");
		assertHasStringField(body,"$.data[0].transactionDate");
		assertHasStringField(body,"$.data[0].billPostDate");
		assertHasIntField(body,"$.data[0].payeeMCC");

		return environment;
	}
}
