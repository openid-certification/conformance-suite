package net.openid.conformance.apis.creditOperations.advances.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v2.AdvancesPaymentsResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AdvancesPaymentsResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractPayments/advanceContractPaymentsResponse.json")
	public void validateStructure() {
		run(new AdvancesPaymentsResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractPayments/advanceContractPaymentsResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new AdvancesPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("isOverParcelPayment",
			new AdvancesPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractPayments/advanceContractPaymentsResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new AdvancesPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("chargeType",
			new AdvancesPaymentsResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/advances/advancesV2/contractPayments/advanceContractPaymentsResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new AdvancesPaymentsResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("paidDate",
			new AdvancesPaymentsResponseValidatorV2().getApiName())));
	}
}
