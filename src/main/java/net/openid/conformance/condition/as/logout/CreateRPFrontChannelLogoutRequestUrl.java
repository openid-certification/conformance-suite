package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.dynregistration.AbstractClientValidationCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Sends a GET request to frontchannel_logout_uri and adds response to env for further processing
 */
public class CreateRPFrontChannelLogoutRequestUrl extends AbstractCondition
{

	@Override
	@PreEnvironment(required = { "client", "session_state_data"})
	@PostEnvironment(strings = {"rp_frontchannel_logout_uri_request_url"})
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");
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
