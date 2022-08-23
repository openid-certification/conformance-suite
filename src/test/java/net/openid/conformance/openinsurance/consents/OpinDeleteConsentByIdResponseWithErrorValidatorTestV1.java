package net.openid.conformance.openinsurance.consents;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.openinsurance.validator.consents.v1.OpinDeleteConsentByIdResponseWithErrorValidatorV1;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@UseResurce("openinsuranceResponses/consents/v1/deleteConsent/deleteConsentByIdResponseWithError.json")
public class OpinDeleteConsentByIdResponseWithErrorValidatorTestV1 extends AbstractJsonResponseConditionUnitTest {

	@Test
	public void validateStructure() {
		run(new OpinDeleteConsentByIdResponseWithErrorValidatorV1());
	}

	@Test
	@UseResurce("openinsuranceResponses/consents/v1/deleteConsent/deleteConsentByIdResponseWithErrorNoField.json")
	public void validateStructureWithMissingField() {
		ConditionError error = runAndFail(new OpinDeleteConsentByIdResponseWithErrorValidatorV1());
		assertThat(error.getMessage(),
			containsString(ErrorMessagesUtils.createElementNotFoundMessage("detail", new OpinDeleteConsentByIdResponseWithErrorValidatorV1().getApiName())));
	}
}
