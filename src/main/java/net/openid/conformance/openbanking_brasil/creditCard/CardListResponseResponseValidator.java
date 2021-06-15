package net.openid.conformance.openbanking_brasil.creditCard;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Cartão de Crédito "Lista de cartões de crédito"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#lista-de-cartoes-de-credito
 **/

@ApiName("Card List")
public class CardListResponseResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].creditCardAccountId");
		assertHasStringField(body, "$.data[0].brandName");
		assertHasStringField(body, "$.data[0].companyCnpj");
		assertHasStringField(body, "$.data[0].name");
		assertHasStringField(body, "$.data[0].productType");
		assertHasStringField(body, "$.data[0].productAdditionalInfo");
		assertHasStringField(body, "$.data[0].creditCardNetwork");
		assertHasStringField(body, "$.data[0].networkAdditionalInfo");

		return environment;
	}
}
