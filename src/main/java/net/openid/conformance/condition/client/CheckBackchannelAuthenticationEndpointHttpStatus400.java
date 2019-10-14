package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckBackchannelAuthenticationEndpointHttpStatus400 extends AbstractCheckBackchannelAuthenticationEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.BAD_REQUEST;
	}
}
