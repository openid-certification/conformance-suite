package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Cartão de Crédito "Identificação de cartão de crédito"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#saldos-da-conta
 **/
@ApiName("Card Identification")
public class CardIdentificationResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.name");
		assertHasStringField(body, "$.data.productType");
		assertHasStringField(body, "$.data.productAdditionalInfo");
		assertHasStringField(body, "$.data.creditCardNetwork");
		assertHasStringField(body, "$.data.networkAdditionalInfo");

		assertHasField(body, "$.data.paymentMethod");
		assertHasStringField(body, "$.data.paymentMethod.identificationNumber");
		assertHasBooleanField(body, "$.data.paymentMethod.isMultipleCreditCard");

		return environment;
	}
}
