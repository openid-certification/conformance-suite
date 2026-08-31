package net.openid.conformance.condition.client;

public class AddFAPIEndUserPresentFalseToResourceEndpointRequest extends AbstractAddFAPIEndUserPresentToResourceEndpointRequest {

	@Override
	protected boolean endUserPresent() {
		return false;
	}

}
