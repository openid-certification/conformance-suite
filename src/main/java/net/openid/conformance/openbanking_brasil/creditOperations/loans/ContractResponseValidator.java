package net.openid.conformance.openbanking_brasil.creditOperations.loans;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * This is validator for API - Operações de Crédito - Empréstimos | Contrato
 * See https://openbanking-brasil.github.io/areadesenvolvedor/#emprestimos-contrato
 */

@ApiName("Contract")
public class ContractResponseValidator extends AbstractJsonAssertingCondition {

	private static final Logger logger = LoggerFactory.getLogger(ContractResponseValidator.class);

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);

		assertHasField(body, "$.data");
		assertHasStringField(body, "$.data.contractNumber");
		assertHasStringField(body, "$.data.ipocCode");
		assertHasStringField(body, "$.data.productName");
		assertHasStringField(body, "$.data.productType");
		assertHasStringField(body, "$.data.productSubType");
		assertHasStringField(body, "$.data.contractDate");
		assertHasDoubleField(body, "$.data.contractAmount");
		assertHasStringField(body, "$.data.settlementDate");
		assertHasStringField(body, "$.data.currency");
		assertHasStringField(body, "$.data.dueDate");
		assertHasStringField(body, "$.data.instalmentPeriodicity");
		assertHasStringField(body, "$.data.instalmentPeriodicityAdditionalInfo");
		assertHasStringField(body, "$.data.firstInstalmentDueDate");
		assertHasDoubleField(body, "$.data.CET");
		assertHasStringField(body, "$.data.amortizationScheduled");
		assertHasStringField(body, "$.data.amortizationScheduledAdditionalInfo");
		assertHasStringField(body, "$.data.cnpjConsignee");
		assertHasStringField(body, "$.data.disbursementDate");

		assertHasField(body, "$.data.interestRates");
		assertJsonArrays(body, "$.data.interestRates", this::assertInterestRate);

		assertHasField(body, "$.data.contractedFees");
		assertJsonArrays(body, "$.data.contractedFees", this::assertContractedFees);

		assertHasField(body, "$.data.contractedFinanceCharges");
		assertJsonArrays(body, "$.data.contractedFinanceCharges", this::assertContractedFinanceCharges);

		return environment;
	}

	private void assertInterestRate(JsonObject element) {
		assertHasStringField(element, "taxType");
		assertHasStringField(element, "interestRateType");
		assertHasStringField(element, "taxPeriodicity");
		assertHasStringField(element, "calculation");
		assertHasStringField(element, "referentialRateIndexerType");
		assertHasDoubleField(element, "preFixedRate");
		assertHasDoubleField(element, "postFixedRate");
		assertHasStringField(element, "additionalInfo");
		assertHasStringField(element, "referentialRateIndexerSubType");
		assertHasStringField(element, "referentialRateIndexerAdditionalInfo");
	}

	private void assertContractedFees(JsonObject element) {
		assertHasStringField(element, "feeName");
		assertHasStringField(element, "feeCode");
		assertHasStringField(element, "feeChargeType");
		assertHasStringField(element, "feeCharge");
		assertHasIntField(element, "feeAmount");
		assertHasDoubleField(element, "feeRate");
	}

	private void assertContractedFinanceCharges(JsonObject element) {
		assertHasStringField(element, "chargeType");
		assertHasStringField(element, "chargeAdditionalInfo");
		assertHasDoubleField(element, "chargeRate");
	}
}
