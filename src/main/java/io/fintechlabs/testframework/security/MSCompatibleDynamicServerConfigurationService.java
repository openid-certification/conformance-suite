package io.fintechlabs.testframework.security;

import org.apache.http.client.HttpClient;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.config.ServerConfiguration;

public class MSCompatibleDynamicServerConfigurationService extends DynamicServerConfigurationService
{
	private String msCommonIssuer;
	private String msTenantIdPlaceholderIssuer;

	public MSCompatibleDynamicServerConfigurationService(String msCommonIssuer, String msTenantIdPlaceholderIssuer) {
		super();
		this.msCommonIssuer = msCommonIssuer;
		this.msTenantIdPlaceholderIssuer = msTenantIdPlaceholderIssuer;
	}

	public MSCompatibleDynamicServerConfigurationService(HttpClient httpClient, String msCommonIssuer, String msTenantIdPlaceholderIssuer) {
		super(httpClient);
		this.msCommonIssuer = msCommonIssuer;
		this.msTenantIdPlaceholderIssuer = msTenantIdPlaceholderIssuer;
	}

	/**
	 * uses the Microsoft common issuer value instead of the {tenantid} one
	 * @param issuer
	 * @return
	 */
	@Override
	public ServerConfiguration getServerConfiguration(String issuer) {
		if(issuer.equals(this.msTenantIdPlaceholderIssuer)) {
			issuer = this.msCommonIssuer;
		}
		return super.getServerConfiguration(issuer);
	}


}
