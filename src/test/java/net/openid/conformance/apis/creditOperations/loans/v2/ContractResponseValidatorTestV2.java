package net.openid.conformance.apis.creditOperations.loans.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/loansV2/contract/contractResponseOK.json")
public class ContractResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
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
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("CET", new ContractResponseValidatorV2().getApiName())));
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
}
