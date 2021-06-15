package net.openid.conformance.openbanking_brasil.creditOperations.financing;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;

/**
 * This is validator for API - Pagamentos do Contrato - Contract Payments
 * https://openbanking-brasil.github.io/areadesenvolvedor/#financiamentos-pagamentos-do-contrato
 */

@ApiName("Financing Payments")
public class FinancingPaymentsResponseValidator extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {

		JsonObject body = bodyFrom(environment);
		assertHasField(body, "$.data");
		assertHasIntField(body, "$.data.paidInstalments");
		assertHasDoubleField(body, "$.data.contractOutstandingBalance");

		assertHasField(body, "$.data.releases");
		assertHasBooleanField(body, "$.data.releases[0].isOverParcelPayment");
		assertHasStringField(body, "$.data.releases[0].instalmentId");
		assertHasStringField(body, "$.data.releases[0].paidDate");
		assertHasStringField(body, "$.data.releases[0].currency");
		assertHasDoubleField(body, "$.data.releases[0].paidAmount");

		assertHasField(body, "$.data.releases[0].overParcel");
		assertHasField(body, "$.data.releases[0].overParcel.fees");

		assertHasStringField(body, "$.data.releases[0].overParcel.fees[0].feeName");
		assertHasStringField(body, "$.data.releases[0].overParcel.fees[0].feeCode");
		assertHasDoubleField(body, "$.data.releases[0].overParcel.fees[0].feeAmount");

		assertHasField(body, "$.data.releases[0].overParcel.charges");
		assertHasStringField(body, "$.data.releases[0].overParcel.charges[0].chargeType");
		assertHasStringField(body, "$.data.releases[0].overParcel.charges[0].chargeAdditionalInfo");
		assertHasDoubleField(body, "$.data.releases[0].overParcel.charges[0].chargeAmount");

		return environment;
	}
}
