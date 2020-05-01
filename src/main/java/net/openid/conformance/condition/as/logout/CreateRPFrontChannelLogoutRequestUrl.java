package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.util.UriComponentsBuilder;

public class CreateRPFrontChannelLogoutRequestUrl extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "session_state_data"})
	@PostEnvironment(strings = {"rp_frontchannel_logout_uri_request_url"})
	public Environment evaluate(Environment env) {
		String frontchannelLogoutUri = env.getString("client", "frontchannel_logout_uri");
		Boolean sessionRequired = env.getBoolean("client", "frontchannel_logout_session_required");

		if(frontchannelLogoutUri==null || frontchannelLogoutUri.isEmpty()) {
			throw error("frontchannel_logout_uri is not defined for the client");
		}

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(frontchannelLogoutUri);

		if(sessionRequired!=null && sessionRequired.booleanValue()) {
			uriBuilder.queryParam("iss", env.getString("issuer"));
			uriBuilder.queryParam("sid", env.getString("session_state_data", "sid"));
		}
		String url = uriBuilder.build().toUriString();
		log("Created frontchannel_logout_uri request url", args("url", url));
		env.putString("rp_frontchannel_logout_uri_request_url", url);
		return env;
	}

}
