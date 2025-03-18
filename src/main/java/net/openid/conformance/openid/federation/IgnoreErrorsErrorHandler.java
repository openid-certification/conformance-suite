package net.openid.conformance.openid.federation;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class IgnoreErrorsErrorHandler extends DefaultResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) {
		// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
		// status code meaning the rest of our code can handle http status codes how it likes
		return false;
	}

}
