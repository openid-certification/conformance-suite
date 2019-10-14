package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckBackchannelAuthenticationEndpointHttpStatus401 extends AbstractCheckBackchannelAuthenticationEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
