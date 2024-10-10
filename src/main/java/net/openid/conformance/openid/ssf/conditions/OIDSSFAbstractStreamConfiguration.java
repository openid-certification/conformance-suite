package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.support.StreamAdminClient;
import net.openid.conformance.testmodule.Environment;

public abstract class OIDSSFAbstractStreamConfiguration extends AbstractCondition {

	public StreamAdminClient getStreamAdminClient(Environment env) {

		String configurationEndpoint = env.getString("transmitter_metadata", "configuration_endpoint");
		String transmitterAccessToken = env.getString("transmitter_access_token");

		return new StreamAdminClient(configurationEndpoint, transmitterAccessToken);
	}
}
