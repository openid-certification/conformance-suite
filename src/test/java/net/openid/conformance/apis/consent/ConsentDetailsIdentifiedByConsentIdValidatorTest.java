package net.openid.conformance.apis.consent;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.openbanking_brasil.consent.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@UseResurce("jsonResponses/consent/getConsentById/getConsentByIdResponse.json")
public class ConsentDetailsIdentifiedByConsentIdValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/getConsentByIdResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchEnumerationMessage("permissions")));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/getConsentByIdResponseMissField.json")
	public void validateStructureWithMissField() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createElementNotFoundMessage("consentId")));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/getConsentByIdResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(condition.createFieldValueNotMatchPatternMessage("creationDateTime")));
	}

}
