package net.openid.conformance.apis.creditOperations.advances.unarrangedAccountsOverdraftV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.advancesV2.AdvancesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contracts/advanceContractsResponse.json")
public class AdvancesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AdvancesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contracts/advanceContractsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AdvancesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("ipocCode",
			new AdvancesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contracts/advanceContractsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new AdvancesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productSubType",
			new AdvancesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contracts/advanceContractsResponse(WrongPattern).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AdvancesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj",
			new AdvancesResponseValidatorV2().getApiName())));
	}
}
