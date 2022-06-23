package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalQualificationResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonQualificationV2/naturalPersonQualificationResponse.json")
public class PersonalQualificationResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new PersonalQualificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonQualificationV2/naturalPersonQualificationResponseWithError(informedIncome).json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new PersonalQualificationResponseValidatorV2());
		String expected = ErrorMessagesUtils.createElementNotFoundMessage("year", new PersonalQualificationResponseValidatorV2().getApiName());
		assertThat(error.getMessage(), containsString(expected));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonQualificationV2/naturalPersonQualificationResponseWithError(patternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		assertThat(runAndFail(new PersonalQualificationResponseValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj", new PersonalQualificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonQualificationV2/naturalPersonQualificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		assertThat(runAndFail(new PersonalQualificationResponseValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaximum("year", new PersonalQualificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/naturalPersonQualificationV2/naturalPersonQualificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		assertThat(runAndFail(new PersonalQualificationResponseValidatorV2()).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("occupationCode", new PersonalQualificationResponseValidatorV2().getApiName())));
	}
}
