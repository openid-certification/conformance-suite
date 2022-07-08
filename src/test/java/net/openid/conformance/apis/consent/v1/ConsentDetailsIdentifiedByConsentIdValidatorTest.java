package net.openid.conformance.apis.consent.v1;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v1.ConsentDetailsIdentifiedByConsentIdValidator;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@UseResurce("jsonResponses/consent/getConsentById/v1/getConsentByIdResponse.json")
public class ConsentDetailsIdentifiedByConsentIdValidatorTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		run(condition);
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v1/getConsentByIdResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v1/getConsentByIdResponseMissField.json")
	public void validateStructureWithMissField() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("consentId", condition.getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v1/getConsentByIdResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConsentDetailsIdentifiedByConsentIdValidator condition = new ConsentDetailsIdentifiedByConsentIdValidator();
		ConditionError error = runAndFail(condition);
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime", condition.getApiName())));
	}

}
