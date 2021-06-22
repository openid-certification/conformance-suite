package net.openid.conformance.apis.registrationData;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.openbanking_brasil.registrationData.NaturalPersonalQualificationResponseValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponse.json")
public class NaturalPersonalQualificationResponseValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		NaturalPersonalQualificationResponseValidator condition = new NaturalPersonalQualificationResponseValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(informedIncome).json")
	public void validateStructureWithMissingField() {
		NaturalPersonalQualificationResponseValidator condition = new NaturalPersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createElementNotFoundMessage("$.data.informedIncome")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(patternNotMatch).json")
	public void validateStructurePatternNotMatch() {
		NaturalPersonalQualificationResponseValidator condition = new NaturalPersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueNotMatchPatternMessage("companyCnpj")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(excessMaxLength).json")
	public void validateStructureExcessMaxLength() {
		NaturalPersonalQualificationResponseValidator condition = new NaturalPersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueIsMoreThanMaxLengthMessage("year")));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/naturalPersonQualificationResponseWithError(enumNotMatch).json")
	public void validateStructureEnumNotMatch() {
		NaturalPersonalQualificationResponseValidator condition = new NaturalPersonalQualificationResponseValidator();
		assertThat(runAndFail(condition).getMessage(),
			containsString(condition.createFieldValueNotMatchEnumerationMessage("occupationCode")));
	}
}
