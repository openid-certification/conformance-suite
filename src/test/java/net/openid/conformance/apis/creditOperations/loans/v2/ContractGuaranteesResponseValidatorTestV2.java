package net.openid.conformance.apis.creditOperations.loans.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractGuaranteesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractGuarantees/contractGuaranteesResponse.json")
public class ContractGuaranteesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ContractGuaranteesResponseValidatorV2 condition = new ContractGuaranteesResponseValidatorV2();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractGuarantees/contractGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		ContractGuaranteesResponseValidatorV2 condition = new ContractGuaranteesResponseValidatorV2();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantySubType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractGuarantees/contractGuaranteesResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ContractGuaranteesResponseValidatorV2 condition = new ContractGuaranteesResponseValidatorV2();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("warrantyType", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/contractGuarantees/contractGuaranteesResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		ContractGuaranteesResponseValidatorV2 condition = new ContractGuaranteesResponseValidatorV2();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("currency", condition.getApiName())));
	}
}
