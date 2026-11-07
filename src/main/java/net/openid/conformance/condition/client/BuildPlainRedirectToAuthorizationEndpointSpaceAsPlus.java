package net.openid.conformance.condition.client;

/**
 * Same as {@link BuildPlainRedirectToAuthorizationEndpoint} but writes each space in a parameter value as "+",
 * the encoding RFC 6749 appendix B's example uses, to check the server decodes it as a space.
 */
public class BuildPlainRedirectToAuthorizationEndpointSpaceAsPlus extends BuildPlainRedirectToAuthorizationEndpoint {

	@Override
	protected boolean encodeSpaceAsPlus() {
		return true;
	}

}
