package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
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
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		String requestUri = env.getString("authorization_endpoint_request", "params.request_uri");
		if (!Strings.isNullOrEmpty(requestUri)) {
			log("Fetching request object from request_uri", args("request_uri", requestUri));
			String requestObjectString = "";
			try {
				RestTemplate restTemplate = createRestTemplate(env);

				requestObjectString = restTemplate.getForObject(requestUri, String.class);

				log("Downloaded request object", args("request_object", requestObjectString));

				JWT jwt = JWTParser.parse(requestObjectString);

				JsonObject header = new JsonParser().parse(jwt.getHeader().toJSONObject().toJSONString()).getAsJsonObject();
				JsonObject claims = new JsonParser().parse(jwt.getJWTClaimsSet().toJSONObject().toJSONString()).getAsJsonObject();

				JsonObject o = new JsonObject();
				o.addProperty("value", requestObjectString); // save the original string to allow for crypto operations
				o.add("header", header);
				o.add("claims", claims);

				env.putObject("authorization_request_object", o);

				logSuccess("Parsed request object", args("request_object", o));

				return env;

			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				throw error("Exception while fetching client keys", e);
			} catch (JsonSyntaxException e) {
				throw error("Client JWKs set string is not JSON", e);
			} catch (ParseException e) {
				throw error("Couldn't parse request object", e, args("request", requestObjectString));
			}

		} else {
			throw error("Authorization endpoint request does not contain a request_uri parameter");
		}

	}
}
