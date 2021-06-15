package net.openid.conformance.openbanking_brasil.creditOperations.advances;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for Adiantamento a Depositantes - Garantias do Contrato | Contract Guarantees"
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#adiantamento-a-depositantes-garantias-do-contrato
 **/

@ApiName("Advances Guarantees")
public class AdvancesGuaranteesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data[0].currency");
		assertHasStringField(body, "$.data[0].warrantyType");
		assertHasStringField(body, "$.data[0].warrantySubType");
		assertHasDoubleField(body, "$.data[0].warrantyAmount");


		return environment;
	}
}
