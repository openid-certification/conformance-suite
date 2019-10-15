package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckBackchannelAuthenticationEndpointHttpStatus200 extends AbstractCheckBackchannelAuthenticationEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.OK;
	}
}
