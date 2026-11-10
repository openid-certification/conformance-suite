package net.openid.conformance.testmodule.endpoint;

/**
 * Why a request did not resolve to an endpoint handler.
 */
public enum Unmatched {

	/** No endpoint on any channel has this path. */
	NO_ROUTE,

	/** An endpoint has this path, but only on other channels (for example mTLS-only). */
	CHANNEL_NOT_ALLOWED,

	/** An endpoint has this path on this channel, but does not accept the request's HTTP method. */
	METHOD_NOT_ALLOWED
}
