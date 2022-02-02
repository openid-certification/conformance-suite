package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.PersonalQualificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponse.json")
public class PersonalQualificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		PersonalQualificationResponseValidator condition = new PersonalQualificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(informedIncome).json")
	public void validateStructureWithMissingField() {
		PersonalQualificationResponseValidator condition = new PersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("$.data.informedIncome", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(patternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		PersonalQualificationResponseValidator condition = new PersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("companyCnpj", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		PersonalQualificationResponseValidator condition = new PersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaximum("year", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		PersonalQualificationResponseValidator condition = new PersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("occupationCode", condition.getApiName())));
	}
}
