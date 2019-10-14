package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus503 extends AbstractCheckTokenEndpointHttpStatus {

	@Override
	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.SERVICE_UNAVAILABLE;
	}

}
