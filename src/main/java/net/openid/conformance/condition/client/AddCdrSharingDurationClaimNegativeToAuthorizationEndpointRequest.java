package net.openid.conformance.condition.client;

public class AddCdrSharingDurationClaimNegativeToAuthorizationEndpointRequest extends AbstractAddCdrSharingDurationClaimToAuthorizationEndpointRequest {

	@Override
	protected long sharingDuration() {
		return -1;
	}

}
