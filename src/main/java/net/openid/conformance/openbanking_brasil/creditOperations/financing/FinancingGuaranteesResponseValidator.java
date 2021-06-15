package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Operações de Crédito - Financiamentos  | Garantias do Contrato
 * https://openbanking-brasil.github.io/areadesenvolvedor/#financiamentos-garantias-do-contrato
 */

@ApiName("Financing Guarantees")
public class FinancingGuaranteesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].currency");
		assertHasStringField(body, "$.data[0].warrantyType");
		assertHasStringField(body, "$.data[0].warrantySubType");

		return environment;
	}
}
