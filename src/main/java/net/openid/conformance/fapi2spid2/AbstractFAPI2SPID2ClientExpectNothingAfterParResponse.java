package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Base class for tests that return an invalid authorization response
 * Client must stop after receiving an invalid authorization response
 */
public abstract class AbstractFAPI2SPID2ClientExpectNothingAfterParResponse extends AbstractFAPI2SPID2ClientTest {

	@Override
	protected void createAuthorizationEndpointResponse() {
		throw new TestFailureException(getId(), "Client has incorrectly called authorization_endpoint after receiving an invalid PAR response (" +
			getParResponseErrorMessage()+ ")");
	}

	@Override
	protected JsonObject createPAREndpointResponse() {
		startWaitingForTimeout();
		return super.createPAREndpointResponse();
	}

	@Override
	protected Object tokenEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an invalid PAR response (" +
			getParResponseErrorMessage()+ ")");
	}

	@Override
	protected Object parEndpoint(String requestId) {
		return super.parEndpoint(requestId);

	}

	@Override
	protected Object userinfoEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called userinfo_endpoint after receiving an invalid PAR response (" +
			getParResponseErrorMessage() + ")");
	}

	@Override
	protected Object accountsEndpoint(String requestId) {
		throw new TestFailureException(getId(), "Client has incorrectly called accounts endpoint after receiving an invalid PAR response (" +
			getParResponseErrorMessage() + ")");
	}

	protected abstract String getParResponseErrorMessage();
}
