package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the VICAL endpoint responded with Content-Type application/cbor, as described in
 * ISO/IEC 18013-5 for VICAL distribution URLs. The VICAL provider is a third party rather than
 * the entity under test, so this is expected to be called as a WARNING.
 */
public class EnsureVicalEndpointContentTypeIsApplicationCbor extends AbstractCheckEndpointContentTypeReturned {

	@Override
	@PreEnvironment(required = "vical_endpoint_response")
	public Environment evaluate(Environment env) {
		return checkContentType(env, "vical_endpoint_response", "headers.", "application/cbor");
	}
}
