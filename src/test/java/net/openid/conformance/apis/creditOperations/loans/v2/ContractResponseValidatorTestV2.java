package net.openid.conformance.apis.creditOperations.loans.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ContractResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseOK.json")
	public void validateStructure() {
		run(new ContractResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseOK(FeeAmountAndpreFixedRateNull).json")
	public void validateStructureNullFee() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementCantBeNullMessage("preFixedRate", new ContractResponseValidatorV2().getApiName())));

	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("contractNumber", new ContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", new ContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("settlementDate", new ContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseAmortizationScheduleAddOK.json")
	public void validateStructureAmortizationScheduleAddOK() {
		run(new ContractResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseAmortizationScheduleAddWrong.json")
	public void validateStructureAmortizationScheduleAddWrong() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("amortizationScheduledAdditionalInfo", new ContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseOK.json")
	public void validateStructureCnpjConsigneeOK() {
		run(new ContractResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseCnpjConsigneeNotNeeded.json")
	public void validateStructureCnpjConsigneeNotNeeded() {
		run(new ContractResponseValidatorV2());
	}
	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseCnpjConsigneeMissing.json")
	public void validateStructureCnpjConsigneeMissing() {
		ConditionError error = runAndFail(new ContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("cnpjConsignee", new ContractResponseValidatorV2().getApiName())));
	}
}
