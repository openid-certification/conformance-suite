package net.openid.conformance.apis.registrationData.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v1.PersonalIdentificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;


public class PersonalIdentificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/naturalPersonIdentificationResponseOK.json")
	public void validateStructure() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/naturalPersonIdentificationResponseOK(missingNonMandatoryField).json")
	public void validateStructureWithMissingNotMandatoryField() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse_missing_consents.json")
	public void validateStructureWithMissingField() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("sex", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(PatternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("birthDate", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(ExessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage(
				"countryCode", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(EnumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("sex", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/personIdentification/errors/naturalPersonIdentificationResponse(coordinateNotMatch).json")
	public void validateStructureCoordinatesNotMatch() {
		PersonalIdentificationResponseValidator condition = new PersonalIdentificationResponseValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(
				"latitude", condition.getApiName())));
	}
}
