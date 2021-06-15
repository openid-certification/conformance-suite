package net.openid.conformance.openbanking_brasil.creditOperations.loans;


import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Empréstimos - Parcelas do Contrato
 * https://openbanking-brasil.github.io/areadesenvolvedor/#emprestimos-parcelas-do-contrato
 */

@ApiName("Contract Installments")
public class ContractInstallmentsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {


		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");

		assertHasStringField(body, "$.data.typeNumberOfInstalments");
		assertHasIntField(body, "$.data.totalNumberOfInstalments");
		assertHasStringField(body, "$.data.typeContractRemaining");
		assertHasIntField(body, "$.data.contractRemainingNumber");
		assertHasIntField(body, "$.data.paidInstalments");
		assertHasIntField(body, "$.data.dueInstalments");
		assertHasIntField(body, "$.data.pastDueInstalments");

		assertHasField(body, "$.data.balloonPayments");
		assertHasStringField(body, "$.data.balloonPayments[0].dueDate");
		assertHasStringField(body, "$.data.balloonPayments[0].currency");
		assertHasDoubleField(body, "$.data.balloonPayments[0].amount");


		return environment;
	}
}
