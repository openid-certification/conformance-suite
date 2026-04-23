package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureNoAuthorizationHeaderIsPresentInPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-stream-verification-error-push-no-auth",
	displayName = "OpenID Shared Signals Framework: Validate anonymous Push-Based SET delivery",
	summary = """
		This test verifies PUSH delivery without a push authorization header.
		The testsuite expects to observe the following interactions:
		 * create a stream without configuring a push authorization header
		 * trigger a verification event
		 * receive the verification event via PUSH delivery without an Authorization header
		 * validate the verification event

		The test fails if the transmitter includes an Authorization header on the push delivery.
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "poll")
public class OIDSSFTransmitterStreamVerificationPushNoAuthTest extends OIDSSFTransmitterStreamVerificationPushTest {

	@Override
	protected String generatePushAuthorizationHeader() {
		// deliberately generate no push authorization header
		return null;
	}

	@Override
	protected void onPushDeliveryReceived(String path, JsonObject requestParts) {
		callAndContinueOnFailure(OIDSSFEnsureNoAuthorizationHeaderIsPresentInPushRequest.class, Condition.ConditionResult.WARNING, "OIDSSF-6.1.1");
	}
}
