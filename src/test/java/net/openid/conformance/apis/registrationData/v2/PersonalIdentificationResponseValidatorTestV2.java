package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalIdentificationResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class PersonalIdentificationResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/naturalPersonIdentificationResponseOK.json")
	public void validateStructure() {
		run(new PersonalIdentificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/naturalPersonIdentificationResponseOK(missingNonMandatoryField).json")
	public void validateStructureWithMissingNotMandatoryField() {
		run(new PersonalIdentificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/errors/naturalPersonIdentificationResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new PersonalIdentificationResponseValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("sex", new PersonalIdentificationResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/errors/naturalPersonIdentificationResponse(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		ConditionError error = runAndFail(new PersonalIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("birthDate", new PersonalIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/errors/naturalPersonIdentificationResponse(ExessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		ConditionError error = runAndFail(new PersonalIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage(
				"civilName", new PersonalIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/errors/naturalPersonIdentificationResponse(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		ConditionError error = runAndFail(new PersonalIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("sex", new PersonalIdentificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/personIdentificationV2/errors/naturalPersonIdentificationResponse(coordinateNotMatch).json")
	public void validateStructureCoordinatesNotMatch() {
		ConditionError error = runAndFail(new PersonalIdentificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(
				"latitude", new PersonalIdentificationResponseValidatorV2().getApiName())));
	}
}
