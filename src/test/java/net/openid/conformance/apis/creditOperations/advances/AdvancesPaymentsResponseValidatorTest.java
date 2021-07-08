package net.openid.conformance.apis.creditOperations.advances;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.AdvancesPaymentsResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/advances/advancesPayments/advancesPaymentsResponse.json")
public class AdvancesPaymentsResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		AdvancesPaymentsResponseValidator condition = new AdvancesPaymentsResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesPayments/advancesPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		AdvancesPaymentsResponseValidator condition = new AdvancesPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("isOverParcelPayment")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesPayments/advancesPaymentsResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		AdvancesPaymentsResponseValidator condition = new AdvancesPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("chargeType")));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesPayments/advancesPaymentsResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		AdvancesPaymentsResponseValidator condition = new AdvancesPaymentsResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("paidDate")));
	}
}
