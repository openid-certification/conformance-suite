package net.openid.conformance.openid.ssf;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openid.ssf.conditions.events.OIDSSFEnsureNoAuthorizationHeaderIsPresentInPushRequest;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-transmitter-push-no-auth",
	displayName = "OpenID Shared Signals Framework: Validate anonymous Push-Based SET delivery",
	summary = """
		This test verifies that the SET event delivery via HTTP PUSH works without an authorization header.
		The test uses no push authorization header and ensures that the Transmitter also uses no authorization header for the HTTP PUSH delivery.
		Then the test triggers a verification and awaits the verification event via HTTP PUSH delivery
		The test fails if the push delivery is performed with a push authorization header.
		""",
	profile = "OIDSSF"
)
@VariantNotApplicable(parameter = SsfDeliveryMode.class, values = "poll")
public class OIDSSFTransmitterPushNoAuthTest extends OIDSSFTransmitterEventsTest {

	@Override
	protected String generatePushAuthorizationHeader() {
		// deliberately generate no push authorization header
		return null;
	}

	@Override
	protected void onPushDeliveryReceived(String path, JsonObject requestParts) {
		callAndContinueOnFailure(OIDSSFEnsureNoAuthorizationHeaderIsPresentInPushRequest.class, Condition.ConditionResult.FAILURE, "OIDSSF-6.1.1");
	}
}
