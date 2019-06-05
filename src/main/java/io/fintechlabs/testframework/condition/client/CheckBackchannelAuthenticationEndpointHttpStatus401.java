package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpStatus;

public class CheckBackchannelAuthenticationEndpointHttpStatus401 extends AbstractCheckBackchannelAuthenticationEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
