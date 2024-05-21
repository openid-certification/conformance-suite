package net.openid.conformance.support.mitre.compat.introspect;

import net.openid.conformance.support.mitre.compat.model.RegisteredClient;

/**
 *
 * Always provides the (configured) IntrospectionURL and RegisteredClient regardless
 * of token. Useful for talking to a single, trusted authorization server.
 *
 * @author jricher
 *
 */
public class StaticIntrospectionConfigurationService implements IntrospectionConfigurationService {

	private String introspectionUrl;
	private RegisteredClient clientConfiguration;

	/**
	 * @return the clientConfiguration
	 */
	public RegisteredClient getClientConfiguration() {
		return clientConfiguration;
	}

	/**
	 * @param client the client to set
	 */
	public void setClientConfiguration(RegisteredClient client) {
		this.clientConfiguration = client;
	}

	/**
	 * @return the introspectionUrl
	 */
	public String getIntrospectionUrl() {
		return introspectionUrl;
	}

	/**
	 * @param introspectionUrl the introspectionUrl to set
	 */
	public void setIntrospectionUrl(String introspectionUrl) {
		this.introspectionUrl = introspectionUrl;
	}

	/* (non-Javadoc)
	 * @see org.mitre.oauth2.introspectingfilter.IntrospectionConfigurationService#getIntrospectionUrl(java.lang.String)
	 */
	@Override
	public String getIntrospectionUrl(String accessToken) {
		return getIntrospectionUrl();
	}

	/* (non-Javadoc)
	 * @see org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService#getClientConfiguration(java.lang.String)
	 */
	@Override
	public RegisteredClient getClientConfiguration(String accessToken) {
		return getClientConfiguration();
	}

}
