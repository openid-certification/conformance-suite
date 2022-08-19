package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.jsonAsserting.ErrorMessagesUtils;
import net.openid.conformance.util.UseResurce;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class AbstractConsentRejectionValidationTest extends AbstractJsonResponseConditionUnitTest {

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseRejectedOk.json")
	public void testHappyPathRejected() {
		run(new EnsureConsentAspspRevoked());
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseRejectedMissingRejectedBy.json")
	public void testUnhappyPathMissingRejectedBy() {
		ConditionError conditionError = runAndFail(new EnsureConsentAspspRevoked());
		assertThat(conditionError.getMessage(), containsString("Unable to find rejectedBy inside rejection object"));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseRejectedMissingReason.json")
	public void testUnhappyPathMissingReason() {
		ConditionError conditionError = runAndFail(new EnsureConsentAspspRevoked());
		assertThat(conditionError.getMessage(), containsString("Rejection object did not contain reason object"));
	}

	@Test
	@UseResurce("jsonResponses/consent/getConsentById/v2/getConsentByIdResponseRejectedMissingCode.json")
	public void testUnhappyPathMissingCode() {
		ConditionError conditionError = runAndFail(new EnsureConsentAspspRevoked());
		assertThat(conditionError.getMessage(), containsString("Reason object is not a json object, it should have the mandatory field code"));
	}
}

