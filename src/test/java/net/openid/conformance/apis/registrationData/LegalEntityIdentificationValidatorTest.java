package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.registrationData.LegalEntityIdentificationValidator;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonalQualificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class LegalEntityIdentificationValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponse.json")
	public void validateStructure() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(missedMandatoryField).json")
	public void validateStructureWithMissingField() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		ConditionError error = runAndFail(condition);
		String expected = condition.createElementNotFoundMessage("email"); //$.data[0].contacts.emails[0].email
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(missedNonMendotaryField).json")
	public void validateStructureWithMissingNonMandatoryField() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(patterNotMatch).json")
	public void validateStructurePatternNotMatch() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("cnpjNumber")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("civilName")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage("type")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(coordinateNotMatch).json")
	public void validateStructureCoordinatesNotMatch() {
		LegalEntityIdentificationValidator condition = new LegalEntityIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createCoordinateIsNotWithinAllowedAreaMessage("latitude")));
	}
}
