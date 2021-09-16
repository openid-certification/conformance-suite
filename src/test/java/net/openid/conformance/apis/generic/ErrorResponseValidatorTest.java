package net.openid.conformance.apis.generic;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/errors/goodErrorResponse.json")
public class ErrorResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateGoodResponse() {

		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		run(condition);
	}

	@Test
	public void validateBadStatusResponse() {
		putStatusCode(environment, 404);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Value from element code doesn't match the required pattern";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/badErrorBodyResponse.json")
	public void validateBadBodyResponse() {
		putStatusCode(environment, 404);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Unable to find element $.errors";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/badErrorErrorsResponse.json")
	public void validateBadErrorsResponse() {
		putStatusCode(environment, 404);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Errors field is not a Json Array";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadLengthDetail.json")
	public void validateGoodErrorsResponseBadLengthDetail() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "more than the required maxLength";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadLengthTitle.json")
	public void validateGoodErrorsResponseBadLengthTitle() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "more than the required maxLength";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadArrayLength.json")
	public void validateGoodErrorsResponseArrayLengthTooMany() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "more than the required maxItems";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadArrayLength2.json")
	public void validateGoodErrorsResponseArrayLengthNone() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "less than the required minItems";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingCode.json")
	public void validateGoodErrorsResponseMissingCode() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Unable to find element code";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingDetail.json")
	public void validateGoodErrorsResponseMissingDetail() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Unable to find element detail";

		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingTitle.json")
	public void validateGoodErrorsResponseMissingTitle() {
		putStatusCode(environment, 403);
		ErrorValidator condition = new ErrorValidator();
		ConditionError error = runAndFail(condition);
		String expected = "Unable to find element title";

		assertThat(error.getMessage(), containsString(expected));
	}

	private void putStatusCode(Environment environment, Integer code){
		environment.putInteger("resource_endpoint_response_status", code);
	}

}
