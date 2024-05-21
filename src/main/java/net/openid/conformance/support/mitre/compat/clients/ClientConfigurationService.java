package net.openid.conformance.support.mitre.compat.clients;

import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

/**
 * @author jricher
 *
 */
public interface ClientConfigurationService {

	public RegisteredClient getClientConfiguration(ServerConfiguration issuer);

}
