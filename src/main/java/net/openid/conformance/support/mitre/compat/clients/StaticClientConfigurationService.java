package net.openid.conformance.support.mitre.compat.clients;

import jakarta.annotation.PostConstruct;
import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

import java.util.Map;

/**
 * Client configuration service that holds a static map from issuer URL to a ClientDetails object to use at that issuer.
 *
 * Designed to be configured as a bean.
 *
 * @author jricher
 *
 */
public class StaticClientConfigurationService implements ClientConfigurationService {

	// Map of issuer URL -> client configuration information
	private Map<String, RegisteredClient> clients;

	/**
	 * @return the clients
	 */
	public Map<String, RegisteredClient> getClients() {
		return clients;
	}

	/**
	 * @param clients the clients to set
	 */
	public void setClients(Map<String, RegisteredClient> clients) {
		this.clients = clients;
	}

	/**
	 * Get the client configured for this issuer
	 *
	 * @see ClientConfigurationService#getClientConfiguration(ServerConfiguration)
	 */
	@Override
	public RegisteredClient getClientConfiguration(ServerConfiguration issuer) {

		return clients.get(issuer.getIssuer());
	}

	@PostConstruct
	public void afterPropertiesSet() {
		if (clients == null || clients.isEmpty()) {
			throw new IllegalArgumentException("Clients map cannot be null or empty");
		}

	}

}
