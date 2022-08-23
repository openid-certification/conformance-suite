package net.openid.conformance.openinsurance.consents;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinConsentDetailsIdentifiedByConsentIdValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@UseResurce("openinsuranceResponses/consents/v1/getConsent/getConsentByIdResponse.json")
public class OpinConsentDetailsIdentifiedByConsentIdValidatorV1Test extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinConsentDetailsIdentifiedByConsentIdValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/getConsent/getConsentByIdResponseWrongEnum.json")
	public void validateStructureWithWrongEnum() {
		ConditionError error = runAndFail(new OpinConsentDetailsIdentifiedByConsentIdValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchEnumerationMessage("permissions", new OpinConsentDetailsIdentifiedByConsentIdValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/getConsent/getConsentByIdResponseMissField.json")
	public void validateStructureWithMissField() {
		ConditionError error = runAndFail(new OpinConsentDetailsIdentifiedByConsentIdValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createElementNotFoundMessage("consentId", new OpinConsentDetailsIdentifiedByConsentIdValidatorV1().getApiName())));
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/getConsent/getConsentByIdResponseWrongRegexp.json")
	public void validateStructureWithWrongRegexp() {
		ConditionError error = runAndFail(new OpinConsentDetailsIdentifiedByConsentIdValidatorV1());
		assertThat(error.getMessage(), containsString(ErrorMessagesUtils.createFieldValueNotMatchPatternMessage("creationDateTime", new OpinConsentDetailsIdentifiedByConsentIdValidatorV1().getApiName())));
	}
}
