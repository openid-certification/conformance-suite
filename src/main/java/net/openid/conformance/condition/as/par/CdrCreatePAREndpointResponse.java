package net.openid.conformance.condition.as.par;

public class CdrCreatePAREndpointResponse extends CreatePAREndpointResponse {

	@Override
	protected int expiresIn() {
		// CDR requires the request_uri to expire between 10 seconds and 90 seconds
		return 90;
	}

}
