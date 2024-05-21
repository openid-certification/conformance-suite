package net.openid.conformance.support.mitre.compat.clients;

import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

import java.util.HashMap;
import java.util.Map;


/**
 * @author jricher
 *
 */
public class InMemoryRegisteredClientService implements RegisteredClientService {

	private Map<String, RegisteredClient> clients = new HashMap<>();

	/* (non-Javadoc)
	 * @see org.mitre.openid.connect.client.service.RegisteredClientService#getByIssuer(java.lang.String)
	 */
	@Override
	public RegisteredClient getByIssuer(String issuer) {
		return clients.get(issuer);
	}

	/* (non-Javadoc)
	 * @see org.mitre.openid.connect.client.service.RegisteredClientService#save(org.mitre.oauth2.model.RegisteredClient)
	 */
	@Override
	public void save(String issuer, RegisteredClient client) {
		clients.put(issuer, client);
	}

}
