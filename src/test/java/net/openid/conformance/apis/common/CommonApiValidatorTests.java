package net.openid.conformance.apis.common;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.common.GetOutagesValidator;
import net.openid.conformance.openbanking_brasil.common.GetStatusValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class CommonApiValidatorTests extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/common/GetOutagesResponse.json")
	public void validateGetOutagesValidator() {
		GetOutagesValidator condition = new GetOutagesValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/common/GetOutagesResponse(missField).json")
	public void validateGetOutagesValidatorWithMissingField() {
		GetOutagesValidator condition = new GetOutagesValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("duration", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/common/GetStatusResponse.json")
	public void validateGetStatusValidator() {
		GetStatusValidator condition = new GetStatusValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/common/GetStatusResponse(missField).json")
	public void validateGetStatusValidatorWithMissingField() {
		GetStatusValidator condition = new GetStatusValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("explanation", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}
}
