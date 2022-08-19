package net.openid.conformance.openinsurance.customers;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.condition.ConditionError;
	import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
    import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalQualificationListValidatorV1;
    import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

	import static org.hamcrest.MatcherAssert.assertThat;
	import static org.hamcrest.Matchers.containsString;

@UseResurce("openinsuranceResponses/customers/v1/personalQualification/naturalPersonQualificationResponse.json")
public class OpinCustomersPersonalQualificationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinCustomersPersonalQualificationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalQualification/naturalPersonQualificationResponseWithError(missing field).json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersPersonalQualificationListValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("lifePensionPlans", new OpinCustomersPersonalQualificationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalQualification/naturalPersonQualificationResponseWithError(patternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		assertThat(runAndFail(new OpinCustomersPersonalQualificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("updateDateTime", new OpinCustomersPersonalQualificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalQualification/naturalPersonQualificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		assertThat(runAndFail(new OpinCustomersPersonalQualificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("year", new OpinCustomersPersonalQualificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personalQualification/naturalPersonQualificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		assertThat(runAndFail(new OpinCustomersPersonalQualificationListValidatorV1()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("occupationCodeType", new OpinCustomersPersonalQualificationListValidatorV1().getApiName())));
	}
}
