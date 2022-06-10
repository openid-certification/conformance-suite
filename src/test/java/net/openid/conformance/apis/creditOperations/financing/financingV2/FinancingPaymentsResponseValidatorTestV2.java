package net.openid.conformance.apis.creditOperations.financing.financingV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.financingV2.FinancingPaymentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingPayments/financingPaymentsResponse.json")
public class FinancingPaymentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new FinancingPaymentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingPayments/financingPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new FinancingPaymentsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("isOverParcelPayment",
				new FinancingPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingPayments/financingPaymentsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new FinancingPaymentsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("chargeType",
				new FinancingPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingV2/financingPayments/financingPaymentsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new FinancingPaymentsResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("paidDate",
				new FinancingPaymentsResponseValidatorV2().getApiName())));
	}
}
