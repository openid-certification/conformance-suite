package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus200 extends AbstractCheckTokenEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.OK;
	}

}
