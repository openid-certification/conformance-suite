package net.openid.conformance.openinsurance.customers;

	import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
	import net.openid.conformance.condition.ConditionError;
	import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
    import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalIdentificationListValidatorV1;
    import net.openid.conformance.util.UseResurce;
	import org.junit.Test;

	import static org.hamcrest.MatcherAssert.assertThat;
	import static org.hamcrest.Matchers.containsString;


public class OpinCustomersPersonalIdentificationListValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/naturalPersonIdentificationResponseOK.json")
	public void validateStructure() {
		run(new OpinCustomersPersonalIdentificationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/naturalPersonIdentificationResponseOK(missingNonMandatoryField).json")
	public void validateStructureWithMissingNotMandatoryField() {
		run(new OpinCustomersPersonalIdentificationListValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/errors/naturalPersonIdentificationResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinCustomersPersonalIdentificationListValidatorV1());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("cnpjNumber", new OpinCustomersPersonalIdentificationListValidatorV1().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/errors/naturalPersonIdentificationResponse(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		ConditionError error = runAndFail(new OpinCustomersPersonalIdentificationListValidatorV1());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("birthDate", new OpinCustomersPersonalIdentificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/errors/naturalPersonIdentificationResponse(ExessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		ConditionError error = runAndFail(new OpinCustomersPersonalIdentificationListValidatorV1());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage(
				"civilName", new OpinCustomersPersonalIdentificationListValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/customers/v1/personaIdentification/errors/naturalPersonIdentificationResponse(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		ConditionError error = runAndFail(new OpinCustomersPersonalIdentificationListValidatorV1());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("civilStatusCode", new OpinCustomersPersonalIdentificationListValidatorV1().getApiName())));
	}
}
