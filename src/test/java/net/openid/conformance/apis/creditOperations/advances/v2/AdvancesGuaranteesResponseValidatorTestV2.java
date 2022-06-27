package net.openid.conformance.apis.creditOperations.advances.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v2.AdvancesGuaranteesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesV2/guarantees/advanceContractGuaranteesResponse.json")
public class AdvancesGuaranteesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new AdvancesGuaranteesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/guarantees/advanceContractGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AdvancesGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantyType",
			new AdvancesGuaranteesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/guarantees/advanceContractGuaranteesResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new AdvancesGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("warrantySubType",
			new AdvancesGuaranteesResponseValidatorV2().getApiName())));
	}
}

