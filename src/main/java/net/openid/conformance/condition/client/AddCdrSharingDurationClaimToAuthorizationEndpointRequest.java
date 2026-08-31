package net.openid.conformance.condition.client;

public class AddCdrSharingDurationClaimToAuthorizationEndpointRequest extends AbstractAddCdrSharingDurationClaimToAuthorizationEndpointRequest {

	// no particular reason for this value; it's just the one in the example at https://consumerdatastandardsaustralia.github.io/standards/#request-object
	public static final long SHARING_DURATION_SECONDS = 7776000;

	@Override
	protected long sharingDuration() {
		return SHARING_DURATION_SECONDS;
	}

}
