package net.openid.conformance.apis.creditOperations.advances.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v2.AdvancesContractResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AdvancesContractResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/agreement/advanceAgreementResponse.json")
	public void validateStructure() {
		run(new AdvancesContractResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/agreement/advanceAgreementResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AdvancesContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("productType",
			new AdvancesContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/agreement/advanceAgreementResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new AdvancesContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType",
			new AdvancesContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/agreement/advanceAgreementResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AdvancesContractResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("settlementDate",
			new AdvancesContractResponseValidatorV2().getApiName())));
	}
}
