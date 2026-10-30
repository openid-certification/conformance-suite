package net.openid.conformance.condition.common.sockets;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.function.Function;

public class HeadersBuilder {

	Function<Environment, String> bearerTokenResolver = null;
	String contentType = null;


	public static HeadersBuilder headersBuilder() {
		return new HeadersBuilder();
	}

	public HeadersBuilder bearerToken(Function<Environment, String> bearerTokenResolver) {
		this.bearerTokenResolver = bearerTokenResolver;
		return this;
	}

	public HeadersBuilder contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public HeadersBuilder json() {
		this.contentType = MediaType.APPLICATION_JSON_VALUE;
		return this;
	}

	public HeadersBuilder form() {
		this.contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
		return this;
	}

	public Function<Environment, HttpHeaders> build() {
		return environment ->  {
			HttpHeaders headers = new HttpHeaders();
			if (bearerTokenResolver != null) {
				headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearerTokenResolver.apply(environment));
			}
			if (contentType != null) {
				headers.set(HttpHeaders.CONTENT_TYPE, contentType);
			}
			return headers;
		};
	}
}
