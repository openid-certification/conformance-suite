package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-with-invalid-bearer-token-test",
	displayName = "FAPI-CIBA-ID1: Client test - invalid bearer token in client notification endpoint request",
	summary = "The client should perform OpenID discovery from the displayed discoveryUrl and then " +
		"call the backchannel endpoint and finally await the ping request. The client must detect that the " +
		"bearer token in the request is invalid",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
public class FAPICIBAClientPingWithInvalidBearerTokenTest extends AbstractFAPICIBAClientPingWithInvalidNotificationTest {

	@Override
	protected Class<? extends Condition> getPingNotificationCondition() {
		return PingClientNotificationEndpointWithBadBearerToken.class;
	}

	@Override
	protected void verifyPingResponse() {
		callAndContinueOnFailure(VerifyPingHttpResponseStatusCodeIs401.class, Condition.ConditionResult.WARNING, "CIBA-10.2");
	}
}
