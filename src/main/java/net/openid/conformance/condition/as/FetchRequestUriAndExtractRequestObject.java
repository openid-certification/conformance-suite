package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

public class FetchRequestUriAndExtractRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authorization_endpoint_http_request_params", "client", "server_encryption_keys"})
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_http_request_params", "request_uri");
		if (!Strings.isNullOrEmpty(requestUri)) {
			log("Fetching request object from request_uri", args("request_uri", requestUri));
			String requestObjectString = "";
			JsonObject client = env.getObject("client");
			JsonObject serverEncKeys = env.getObject("server_encryption_keys");
			try {
				RestTemplate restTemplate = createRestTemplate(env);

				requestObjectString = restTemplate.getForObject(requestUri, String.class);

				log("Downloaded request object", args("request_object", requestObjectString));

				//request object will be decrypted if it's encrypted
				JsonObject jsonObjectForJwt = JWTUtil.jwtStringToJsonObjectForEnvironment(requestObjectString, client, serverEncKeys);

				env.putObject("authorization_request_object", jsonObjectForJwt);

				logSuccess("Parsed request object", args("request_object", jsonObjectForJwt));

				return env;

			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Unable to fetch request_uri from " + requestUri;
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			} catch (JsonSyntaxException e) {
				throw error("Response is not JSON", e);
			} catch (ParseException e) {
				throw error("Couldn't parse request object", e, args("request", requestObjectString));
			} catch (JOSEException e) {
				throw error("Couldn't decrypt request object", e,
						args("request", requestObjectString, "keys", serverEncKeys));
			}

		} else {
			throw error("Authorization endpoint request does not contain a request_uri parameter");
		}

	}
}
