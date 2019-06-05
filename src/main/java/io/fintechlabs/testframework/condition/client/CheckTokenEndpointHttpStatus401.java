package io.fintechlabs.testframework.condition.client;

import org.springframework.http.HttpStatus;

public class CheckTokenEndpointHttpStatus401 extends AbstractCheckTokenEndpointHttpStatus {

	protected HttpStatus getExpectedHttpStatus() {
		return HttpStatus.UNAUTHORIZED;
	}
}
