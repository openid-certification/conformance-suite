package net.openid.conformance.apis.creditOperations.financing.financingV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.financingV2.FinancingContractResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingContract/financingContractResponse.json")
public class FinancingContractResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new FinancingContractResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingContract/financingContractResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new FinancingContractResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage(
				"productType", new FinancingContractResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingContract/financingContractResponseWrongRegexp.json")
	public void validateStructureWrongPattern() {
		ConditionError error = runAndFail(new FinancingContractResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("currency",
				new FinancingContractResponseValidatorV2().getApiName())));
	}
}
