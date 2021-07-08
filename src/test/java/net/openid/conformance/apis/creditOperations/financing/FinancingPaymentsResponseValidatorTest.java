package net.openid.conformance.apis.creditOperations.financing;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.FinancingPaymentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/financing/financingPayments/financingPaymentsResponse.json")
public class FinancingPaymentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		FinancingPaymentsResponseValidator condition = new FinancingPaymentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingPayments/financingPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		FinancingPaymentsResponseValidator condition = new FinancingPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("isOverParcelPayment")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingPayments/financingPaymentsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		FinancingPaymentsResponseValidator condition = new FinancingPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("chargeType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/financing/financingPayments/financingPaymentsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		FinancingPaymentsResponseValidator condition = new FinancingPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("paidDate")));
	}
}
