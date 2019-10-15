package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus400 extends AbstractCheckTokenEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.BAD_REQUEST;
	}
}
