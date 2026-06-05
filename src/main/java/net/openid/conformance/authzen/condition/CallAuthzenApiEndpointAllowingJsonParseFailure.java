package net.openid.conformance.authzen.condition;

public class CallAuthzenApiEndpointAllowingJsonParseFailure extends CallAuthzenApiEndpoint {

	@Override
	protected boolean allowJsonParseFailure() {
		return true;
	}
}
