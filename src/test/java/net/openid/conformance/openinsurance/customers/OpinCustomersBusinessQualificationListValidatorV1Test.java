package net.openid.conformance.openinsurance.customers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessQualificationListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("openinsuranceResponses/customers/v1/businessQualification/legalEntityQualificationResponse.json")
public class OpinCustomersBusinessQualificationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinCustomersBusinessQualificationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessQualification/legalEntityQualificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersBusinessQualificationListValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("updateDateTime",
			new OpinCustomersBusinessQualificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessQualification/legalEntityQualificationResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new OpinCustomersBusinessQualificationListValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("incomeFrequency",
			new OpinCustomersBusinessQualificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessQualification/legalEntityQualificationResponseWrongMaximum.json")
	public void validateStructureWrongMaximumLength() {
		ConditionError error = runAndFail(new OpinCustomersBusinessQualificationListValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("year",
			new OpinCustomersBusinessQualificationListValidatorV1().getApiName())));
	}
}
