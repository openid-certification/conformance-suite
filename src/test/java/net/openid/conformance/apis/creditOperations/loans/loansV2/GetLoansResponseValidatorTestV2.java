package net.openid.conformance.apis.creditOperations.loans.loansV2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.loansV2.GetLoansResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/creditOperations/loans/loansV2/getLoans/getLoansResponseOK.json")
public class GetLoansResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		GetLoansResponseValidatorV2 condition = new GetLoansResponseValidatorV2();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/getLoans/getLoansResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new GetLoansResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("ipocCode", new GetLoansResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/getLoans/getLoansResponse(WrongEnum).json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new GetLoansResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("productType", new GetLoansResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/getLoans/getLoansResponse(WrongMaxLength).json")
	public void validateStructureWrongMaxLength() {
		ConditionError error = runAndFail(new GetLoansResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("brandName", new GetLoansResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansV2/getLoans/getLoansResponse(WrongRegexp).json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new GetLoansResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj", new GetLoansResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/creditOperations/loans/loansErrorResponse.json")
	public void validateErrorResponse() {
		ErrorValidator condition = new ErrorValidator();
		environment.putInteger("resource_endpoint_response_status", 403);
		run(condition);
		environment.removeNativeValue("resource_endpoint_response_status");
	}
}

