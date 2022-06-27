package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessIdentificationValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class LegalEntityIdentificationValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponse.json")
	public void validateStructure() {
		run(new BusinessIdentificationValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(missedMandatoryField).json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new BusinessIdentificationValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("updateDateTime", new BusinessIdentificationValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(missedNonMendotaryField).json")
	public void validateStructureWithMissingNonMandatoryField() {
		run(new BusinessIdentificationValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(patterNotMatch).json")
	public void validateStructurePatternNotMatch() {
		assertThat(runAndFail(new BusinessIdentificationValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("cnpjNumber", new BusinessIdentificationValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		assertThat(runAndFail(new BusinessIdentificationValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("civilName", new BusinessIdentificationValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		assertThat(runAndFail(new BusinessIdentificationValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("type", new BusinessIdentificationValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legarEntityIdentificationV2/legalEntityIdentificationResponseWithError(coordinateNotMatch).json")
	public void validateStructureCoordinatesNotMatch() {
		assertThat(runAndFail(new BusinessIdentificationValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createCoordinateIsNotWithinAllowedAreaMessage(
				"latitude", new BusinessIdentificationValidatorV2().getApiName())));
	}
}
