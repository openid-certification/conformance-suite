package net.openid.conformance.openbanking_brasil.creditOperations.loans;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Operações de Crédito - Empréstimos | Garantias do contrato
 * https://openbanking-brasil.github.io/areadesenvolvedor/#emprestimos-garantias-do-contrato
 */

@ApiName("Contract Guarantees")
public class ContractGuaranteesResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, ROOT_PATH);
		assertJsonArrays(body, ROOT_PATH, this::assertData);

		return environment;
	}

	private void assertData(JsonObject element) {
		assertHasStringField(element, "currency");
		assertHasStringField(element, "warrantyType");
		assertHasStringField(element, "warrantySubType");
	}
}
