package net.openid.conformance.support.mitre.compat.issuer;

import jakarta.servlet.http.HttpServletRequest;
import net.openid.conformance.support.mitre.compat.clients.IssuerServiceResponse;

/**
 *
 * Gets an issuer for the given request. Might do dynamic discovery, or might be statically configured.
 *
 * @author jricher
 *
 */
public interface IssuerService {

	public IssuerServiceResponse getIssuer(HttpServletRequest request);

}
