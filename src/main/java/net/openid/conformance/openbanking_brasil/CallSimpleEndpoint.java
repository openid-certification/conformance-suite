package net.openid.conformance.openbanking_brasil;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

/**
 * This simple condition will invoke an external HTTP endpoint
 * and inject the response into the Environment for future processing
 */
public class CallSimpleEndpoint extends AbstractCondition {

	@Override
	/*
		PostEnvironment allows us to specify that upon exiting this evaluate method,
		a particular object or value is present in the environment *after* this
		condition is evaluated. The condition will fail if not met.
	 */
	@PreEnvironment(required = "resource")
	@PostEnvironment(required = "response")
	public Environment evaluate(Environment env) {
		JsonObject resourceConfig = env.getObject("resource");
		String url = OIDFJSON.getString(resourceConfig.get("resourceUrl"));

		HttpMethod method = getMethod(env);
		try {

			RestTemplate restTemplate = createRestTemplate(env);

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			JsonObject responseCode = new JsonObject();
			responseCode.addProperty("code", response.getStatusCodeValue());
			String responseBody = response.getBody();
			JsonObject parsedResponse = new JsonParser().parse(responseBody).getAsJsonObject();
			env.putObject("response", parsedResponse);
			logSuccess("Reponse successfully obtained from API");

		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (CertificateException exception) {
			exception.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return env;
	}

	protected HttpMethod getMethod(Environment env) {

		HttpMethod resourceMethod = HttpMethod.GET;
		String configuredMethod = env.getString("resource", "resourceMethod");
		if (!Strings.isNullOrEmpty(configuredMethod)) {
			resourceMethod = HttpMethod.valueOf(configuredMethod);
		}

		return resourceMethod;
	}
}
