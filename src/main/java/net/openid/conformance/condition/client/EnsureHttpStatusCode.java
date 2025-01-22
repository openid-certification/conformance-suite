package net.openid.conformance.condition.client;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public final class EnsureHttpStatusCode extends AbstractEnsureHttpStatusCode {

	public static final EnsureHttpStatusCode OK_200 = new EnsureHttpStatusCode(200);

	private final int expectedStatusCode;

	public EnsureHttpStatusCode(int expectedStatusCode) {
		Objects.requireNonNull(HttpStatus.resolve(expectedStatusCode), "Invalid HTTP status code: " + expectedStatusCode);
		this.expectedStatusCode = expectedStatusCode;
	}

	@Override
	protected int getExpectedStatusCode() {
		return expectedStatusCode;
	}

}
