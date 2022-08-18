package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.*;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class PingClientNotificationEndpointWithBadBearerToken extends PingClientNotificationEndpoint {

	@Override
	protected String getBearerToken(Environment env) {
		return env.getString("client_notification_token") + "1";
	}

	@Override
	protected Environment handleClientResponseException(Environment env, RestClientResponseException e) {
		env.putInteger("client_notification_endpoint_response_http_status", e.getRawStatusCode());
		env.putBoolean("client_was_pinged", true);

		return env;
	}
}
