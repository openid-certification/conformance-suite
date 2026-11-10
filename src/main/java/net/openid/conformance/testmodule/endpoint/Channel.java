package net.openid.conformance.testmodule.endpoint;

/**
 * The entry point through which a request reached the test module.
 */
public enum Channel {

	/** {@code TestModule.handleHttp}: the regular TLS endpoints, path relative to the test's base URL. */
	HTTP,

	/** {@code TestModule.handleHttpMtls}: the mTLS endpoints, path relative to the test's mTLS base URL. */
	MTLS,

	/** {@code TestModule.handleWellKnown}: the host-level well-known endpoints, path starting with {@code /.well-known/}. */
	WELL_KNOWN
}
