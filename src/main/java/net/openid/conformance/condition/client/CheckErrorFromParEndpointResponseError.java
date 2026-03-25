package net.openid.conformance.condition.client;

import java.util.Objects;

public class CheckErrorFromParEndpointResponseError extends AbstractCheckErrorFromParEndpointResponseError {

	protected final String[] expectedErrors;

	public CheckErrorFromParEndpointResponseError(String first, String... more) {
		String[] expectedErrors = new String[more.length + 1];
		expectedErrors[0] = Objects.requireNonNull(first);
		if (more.length > 0) {
			System.arraycopy(more, 0, expectedErrors, 1, more.length);
		}
		this.expectedErrors = expectedErrors.clone();
	}

	@Override
	protected String[] getExpectedError() {
		return expectedErrors.clone();
	}
}
