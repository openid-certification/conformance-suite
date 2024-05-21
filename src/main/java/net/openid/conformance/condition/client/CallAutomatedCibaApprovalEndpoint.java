package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CallAutomatedCibaApprovalEndpoint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		final String configPath = "automated_ciba_approval_url";
		final String url = env.getString("config", configPath);

		if (Strings.isNullOrEmpty(url)) {
			log("If your server supports automated testing, you can set '"+configPath+"' in your configuration to a url like https://cibasim.example.com/action?token={auth_req_id}&type={action} (auth_req_id will be automatically substituted for the current auth_req_id by the conformance suite, action will be allow or deny depending on the test)");
			return env;
		}
			//"https://cibasim.authlete.com/api/authenticate/actionize?workspace=authlete/fapidev&action={action}&token={auth_req_id}";

		final String authReqId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");
		final String action = env.getString("request_action");

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			// extract the headers for use (below)
			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(null, headers);

			Map<String, String> uriVariables = new HashMap<>();
			uriVariables.put("auth_req_id", authReqId);
			uriVariables.put("action", action);

			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(url, request, String.class, uriVariables);
				logSuccess("Successfully called "+configPath+" endpoint", args("response", jsonString));
			} catch (RestClientResponseException e) {
				throw error("Error from the "+configPath+" endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				String msg = "Call to automated ciba approval endpoint " + configPath + " failed";
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}
}
