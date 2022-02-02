package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.BusinessIdentificationValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class LegalEntityIdentificationValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponse.json")
	public void validateStructure() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(missedMandatoryField).json")
	public void validateStructureWithMissingField() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		ConditionError error = runAndFail(condition);
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("updateDateTime", condition.getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(missedNonMendotaryField).json")
	public void validateStructureWithMissingNonMandatoryField() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(patterNotMatch).json")
	public void validateStructurePatternNotMatch() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("cnpjNumber", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("civilName", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/legalEntityIdentificationResponseWithError(coordinateNotMatch).json")
	public void validateStructureCoordinatesNotMatch() {
		BusinessIdentificationValidator condition = new BusinessIdentificationValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(
				"latitude", condition.getApiName())));
	}
}
