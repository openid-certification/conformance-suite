package net.openid.conformance.condition.client;

public class AddCdrSharingDurationClaimZeroToAuthorizationEndpointRequest extends AbstractAddCdrSharingDurationClaimToAuthorizationEndpointRequest {

	@Override
	protected long sharingDuration() {
		return 0;
	}

}
