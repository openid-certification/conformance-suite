package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-without-bearer-token-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing bearer token in client notification endpoint request",
	summary = "The client receives a ping notification without an Authorization header. The client must reject " +
		"the unauthenticated notification and must not redeem the auth_req_id or call a protected resource.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAClientPingWithoutBearerTokenTest extends AbstractFAPICIBAClientPingWithInvalidNotificationTest {

	@Override
	protected Class<? extends Condition> getPingNotificationCondition() {
		return PingClientNotificationEndpointWithoutBearerToken.class;
	}

	@Override
	protected void verifyPingResponse() {
		callAndContinueOnFailure(VerifyPingHttpResponseStatusCodeIs401.class, Condition.ConditionResult.WARNING,
			"CIBA-10.2", "BrazilCIBA-6.3.4");
	}
}
