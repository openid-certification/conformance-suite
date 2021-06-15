package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API-Cartão de Crédito | Limites de cartão de crédito
 * https://openbanking-brasil.github.io/areadesenvolvedor/#limites-de-cartao-de-credito
 */

@ApiName("Credit Card Accounts Limits")
public class CreditCardAccountsLimitsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body,"$.data[0].creditLineLimitType");
		assertHasStringField(body,"$.data[0].consolidationType");
		assertHasStringField(body,"$.data[0].identificationNumber");
		assertHasStringField(body,"$.data[0].lineName");
		assertHasStringField(body,"$.data[0].lineNameAdditionalInfo");
		assertHasBooleanField(body,"$.data[0].isLimitFlexible");
		assertHasStringField(body,"$.data[0].limitAmountCurrency");
		assertHasDoubleField(body,"$.data[0].limitAmount");
		assertHasStringField(body,"$.data[0].usedAmountCurrency");
		assertHasDoubleField(body,"$.data[0].usedAmount");
		assertHasStringField(body,"$.data[0].availableAmountCurrency");
		assertHasDoubleField(body,"$.data[0].availableAmount");

		return environment;
	}
}
