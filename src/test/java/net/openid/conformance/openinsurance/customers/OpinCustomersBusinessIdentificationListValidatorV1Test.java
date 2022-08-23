package net.openid.conformance.openinsurance.customers;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessIdentificationListValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class OpinCustomersBusinessIdentificationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessIdentification/legalEntityIdentificationResponse.json")
	public void validateStructure() {
		run(new OpinCustomersBusinessIdentificationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessIdentification/legalEntityIdentificationResponseWithError(missedMandatoryField).json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersBusinessIdentificationListValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("updateDateTime", new OpinCustomersBusinessIdentificationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessIdentification/legalEntityIdentificationResponseWithError(patterNotMatch).json")
	public void validateStructurePatternNotMatch() {
		assertThat(runAndFail(new OpinCustomersBusinessIdentificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("cnpjNumber", new OpinCustomersBusinessIdentificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessIdentification/legalEntityIdentificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		assertThat(runAndFail(new OpinCustomersBusinessIdentificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("civilName", new OpinCustomersBusinessIdentificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/businessIdentification/legalEntityIdentificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		assertThat(runAndFail(new OpinCustomersBusinessIdentificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new OpinCustomersBusinessIdentificationListValidatorV1().getApiName())));
	}
}
