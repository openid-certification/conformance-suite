package net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Direitos Creditórios Descontados - Garantias do Contrato
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#direitos-creditorios-descontados-garantias-do-contrato
 */

@ApiName("Invoice Financing Contract Guarantees")
public class InvoiceFinancingContractGuaranteesResponseValidator extends AbstractJsonAssertingCondition {

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
