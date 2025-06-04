package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
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

public class VCIFetchCredentialOfferFromCredentialOfferUri extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String credentialOfferUri = env.getString("vci", "credential_offer_uri");

		JsonObject credentialOfferResponse = fetchCredentialOffer(env, credentialOfferUri);
		String rawCredentialOfferResponse = OIDFJSON.getString(credentialOfferResponse.get("body"));

		env.putString("vci","credential_offer_raw", rawCredentialOfferResponse);
		logSuccess("Fetched credential offer from credential offer uri", args("credential_offer", credentialOfferResponse, "credential_offer_uri", credentialOfferUri));

		return env;
	}

	protected JsonObject fetchCredentialOffer(Environment env, String credentialOfferUrl) {
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			ResponseEntity<String> response = restTemplate.exchange(credentialOfferUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("credential-offer", response);
			return responseInfo;
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (Exception e) {
			String msg = "Unable to fetch credential offer from " + credentialOfferUrl;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}
}
