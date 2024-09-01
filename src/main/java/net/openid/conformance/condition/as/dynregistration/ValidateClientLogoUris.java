package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OPTIONAL. URL that references a logo for the Client application. If present, the server SHOULD display this
 * image to the End-User during approval. The value of this field MUST point to a valid image file.
 * If desired, representation of this Claim in different languages and scripts is represented as
 * described in Section 2.1.
 *
 * This class implements "The value of this field MUST point to a valid image file."
 */
public class ValidateClientLogoUris extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		Map<String, String> logoUris = getAllLogoUris();
		if(logoUris==null || logoUris.isEmpty()) {
			logSuccess("Client does not contain any logo_uri");
			return env;
		}
		//Note: I would use a map with uris as keys but it caused errors like the following so I just used strings
		//  org.springframework.data.mapping.MappingException: Map key
		//  https://www.example.com/a.png contains dots but no replacement was configured!
		//  Make sure map keys don't contain dots in the first place or configure an appropriate replacement!
		List<String> uriContentTypesMap = new ArrayList<>();
		RestTemplate restTemplate = null;
		try {
			restTemplate = createRestTemplate(env, false);
		} catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException |
			KeyStoreException | InvalidKeySpecException | KeyManagementException e) {
			throw error("Error creating http client", e);
		}

		for(String lang : logoUris.keySet()) {
			String uri = logoUris.get(lang);
			//TODO are data urls also valid logo_uri values? I think they should be
			if(uri.startsWith("data:")) {
				//data:[<mediatype>][;base64],<data>
				//this is a data: url. https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs
				if(uri.startsWith("data:image/")) {
					int mimeTypeEndPos = uri.indexOf(';', 11);
					if(mimeTypeEndPos==-1) {
						mimeTypeEndPos = uri.indexOf(',', 11);
					}
					if(mimeTypeEndPos==-1) {
						appendError("failure_reason", "Invalid data url format",
							"details", args("uri", uri));
					} else {
						//success, it's an image
						uriContentTypesMap.add(uri + " : " + uri.substring(5, mimeTypeEndPos));
					}
				}
			} else {
				try {
					HttpHeaders httpHeaders = restTemplate.headForHeaders(uri);
					if (httpHeaders.getContentType() == null) {
						appendError("failure_reason", "Response does not contain a content-type header",
							"details", args("uri", uri));
						continue;
					}

					if ("image".equals(httpHeaders.getContentType().getType())) {
						uriContentTypesMap.add(uri + " : " + httpHeaders.getContentType());
					} else {
						appendError("failure_reason", "Invalid content type, " +
								"content-type is not 'image'",
							"details", args("uri", uri, "content_type", httpHeaders.getContentType().toString()));
						continue;
					}
				} catch (RestClientException ex) {
					appendError("failure_reason", "Http error",
						"details", args("uri", uri, "exception", ex.getMessage()));
				}
			}
		}
		if(!validationErrors.isEmpty()) {
			throw error("logo_uri validation failed", args("errors", validationErrors));
		}
		logSuccess("Client contains valid logo_uri(s)", args("logo_uri_content_types", uriContentTypesMap));
		return env;
	}
}
