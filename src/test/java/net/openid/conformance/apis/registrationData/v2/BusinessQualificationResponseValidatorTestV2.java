package net.openid.conformance.apis.registrationData.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessQualificationResponseValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("jsonResponses/registrationData/registrationDataV2/legalEntityQualificationV2/legalEntityQualificationResponse.json")
public class BusinessQualificationResponseValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new BusinessQualificationResponseValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legalEntityQualificationV2/legalEntityQualificationResponseWithError.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new BusinessQualificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("currency",
			new BusinessQualificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legalEntityQualificationV2/legalEntityQualificationResponseWrongEnum.json")
	public void validateStructureWrongEnum() {
		ConditionError error = runAndFail(new BusinessQualificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("frequency",
			new BusinessQualificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legalEntityQualificationV2/legalEntityQualificationResponseWrongRegexp.json")
	public void validateStructureWrongRegexp() {
		ConditionError error = runAndFail(new BusinessQualificationResponseValidatorV2());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("date",
				new BusinessQualificationResponseValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/registrationData/registrationDataV2/legalEntityQualificationV2/legalEntityQualificationResponseWrongMaximum.json")
	public void validateStructureWrongMaximum() {
		ConditionError error = runAndFail(new BusinessQualificationResponseValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaximum("year",
			new BusinessQualificationResponseValidatorV2().getApiName())));
	}
}
