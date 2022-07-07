package net.openid.conformance.apis.consent.v2;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponse.json")
public class ConsentDetailsIdentifiedByConsentIdValidatorTestV2 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions", new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseMissField.json")
	public void validateStructureWithMissField() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("consentId", new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime", new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseMaxLengthErr.json")
	public void validateStructureMaxLength() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueIsMoreThanMaxLengthMessage("additionalInformation", new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseEMPTY.json")
	public void validateStructureEmpty() {
		ConditionError error = runAndFail(new ConsentDetailsIdentifiedByConsentIdValidatorV2());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createArrayIsLessThanMaxItemsMessage("permissions", new ConsentDetailsIdentifiedByConsentIdValidatorV2().getApiName())));
	}

}
