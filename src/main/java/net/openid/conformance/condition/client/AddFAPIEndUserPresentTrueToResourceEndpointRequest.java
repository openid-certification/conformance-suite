package net.openid.conformance.condition.client;

public class AddFAPIEndUserPresentTrueToResourceEndpointRequest extends AbstractAddFAPIEndUserPresentToResourceEndpointRequest {

	@Override
	protected boolean endUserPresent() {
		return true;
	}

}
