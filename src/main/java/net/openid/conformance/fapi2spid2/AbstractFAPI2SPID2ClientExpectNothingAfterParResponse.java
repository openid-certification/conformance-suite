package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;

/**
 * Base class for tests that return an invalid authorization response
 * Client must stop after receiving an invalid authorization response
 */
public abstract class AbstractFAPI2SPID2ClientExpectNothingAfterParResponse extends AbstractFAPI2SPID2ClientTest {

	@Override
	protected String getResponseClientMustStopAfter() {
		return "an invalid PAR response (" + getParResponseErrorMessage() + ")";
	}

	@Override
	protected JsonObject createPAREndpointResponse() {
		// after this the request routers refuse all endpoints; in particular the token
		// endpoint must keep working until here, as consent based profiles legitimately
		// call the token endpoint before the PAR request
		startWaitingForTimeout();
		return super.createPAREndpointResponse();
	}

	protected abstract String getParResponseErrorMessage();
}
