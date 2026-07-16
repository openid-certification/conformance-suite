package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-ping-with-wrong-auth-req-id-test",
	displayName = "FAPI-CIBA-ID1: Client test - wrong auth_req_id in client notification endpoint request",
	summary = "The client receives an authenticated ping notification containing an auth_req_id that was not issued " +
		"for the current flow. The client must not redeem an auth_req_id or call a protected resource.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = CIBAMode.class, values = {"poll"})
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAClientPingWithWrongAuthReqIdTest extends AbstractFAPICIBAClientPingWithInvalidNotificationTest {

	@Override
	protected Class<? extends Condition> getPingNotificationCondition() {
		return PingClientNotificationEndpointWithWrongAuthReqId.class;
	}
}
