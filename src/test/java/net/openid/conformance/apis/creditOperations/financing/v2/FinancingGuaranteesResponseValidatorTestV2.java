package net.openid.conformance.apis.creditOperations.financing.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.FinancingGuaranteesResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingV2/guarantees/financingGuaranteesResponse.json")
public class FinancingGuaranteesResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new FinancingGuaranteesResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/guarantees/financingGuaranteesResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new FinancingGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("warrantySubType",
				new FinancingGuaranteesResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/guarantees/financingGuaranteesResponse(WrongEnum).json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new FinancingGuaranteesResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(
				"warrantyType", new FinancingGuaranteesResponseValidatorV2().getApiName())));
	}
}
