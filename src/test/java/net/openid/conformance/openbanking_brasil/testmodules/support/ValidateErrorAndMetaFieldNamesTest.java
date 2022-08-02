package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.util.UseResurce;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class ValidateErrorAndMetaFieldNamesTest extends AbstractJsonResponseConditionUnitTest {


	@Before
	public void init() {
		setJwt(true);
	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorWithMetaDataResponse.json")
	public void validateGood422ErrorWithMeta() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorResponse.json")
	public void validateGood422ErrorWithoutMeta() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponse.json")
	public void validateGoodError() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		run(condition);

	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorBadArrayLengthResponse.json")
	public void validateGoodErrorWithBadArraySize() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsLessThanMinimum("totalRecords", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorBadArrayLengthResponse2.json")
	public void validateGoodErrorWithBadArraySize2() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaximum("totalRecords", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorBadMetaDataResponse.json")
	public void validateGoodErrorWithBadAMetaData() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsLessThanMinimum("totalPages", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/422/good422ErrorBadMetaDataResponse2.json")
	public void validateGoodErrorWithBadAMetaData2() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("requestDateTime", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/badErrorBodyResponse.json")
	public void validateBadErrorBodyResponse() {
		setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString("Unable to find element errors on the ValidateErrorAndMetaFieldNames API response"));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadLengthCode.json")
	public void validateGoodErrorResponseBadLengthCode() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("code", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadLengthDetail.json")
	public void validateGoodErrorResponseBadLengthDetail() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("detail", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorResponseBadLengthTitle.json")
	public void validateGoodErrorResponseBadLengthTitle() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("title", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingCode.json")
	public void validateGoodErrorsMissingCode() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString("Unable to find element code on the ValidateErrorAndMetaFieldNames API response"));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingDetail.json")
	public void validateGoodErrorsMissingDetail() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString("Unable to find element detail on the ValidateErrorAndMetaFieldNames API response"));
	}

	@Test
	@UseResurce("jsonResponses/errors/goodErrorsMissingTitle.json")
	public void validateGoodErrorsMissingTitle() {
		setStatus(HttpStatus.FORBIDDEN.value());
		ValidateErrorAndMetaFieldNames condition = new ValidateErrorAndMetaFieldNames();
		ConditionError conditionError = runAndFail(condition);
		assertThat(conditionError.getMessage(), containsString("Unable to find element title on the ValidateErrorAndMetaFieldNames API response"));
	}
}
