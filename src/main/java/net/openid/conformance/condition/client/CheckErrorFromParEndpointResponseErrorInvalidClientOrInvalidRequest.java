package net.openid.conformance.condition.client;

public class CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest extends CheckErrorFromParEndpointResponseError {

	public CheckErrorFromParEndpointResponseErrorInvalidClientOrInvalidRequest() {
		super("invalid_request", "invalid_client");
	}
}
