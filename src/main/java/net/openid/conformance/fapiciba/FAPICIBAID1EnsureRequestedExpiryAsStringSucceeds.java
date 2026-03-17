package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddRequestedExp30sAsStringToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-as-string-succeeds",
	displayName = "FAPI-CIBA-ID1: Ensure requested_expiry as string succeeds",
	summary = "This test makes a CIBA request but with the requested_expiry as a JSON string, which must succeed. As per CIBA-7.1.1, 'requested_expiry [...] may be sent as either a JSON string or a JSON number, the OP must accept either type.'",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureRequestedExpiryAsStringSucceeds extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExp30sAsStringToAuthorizationEndpointRequest.class, "CIBA-7.1", "CIBA-7.1.1");
	}
}
