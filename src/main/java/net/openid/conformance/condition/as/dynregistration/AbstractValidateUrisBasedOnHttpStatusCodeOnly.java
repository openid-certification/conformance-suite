package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.HttpUtil;
import org.apache.http.HttpResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public abstract class AbstractValidateUrisBasedOnHttpStatusCodeOnly extends AbstractClientValidationCondition
{

	protected abstract Map<String, String> getUrisToTest();
	protected abstract String getMetadataName();
	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		Map<String, String> clientUris = getUrisToTest();
		if(clientUris==null || clientUris.isEmpty()) {
			logSuccess("Client does not contain any " + getMetadataName());
			return env;
		}
		List<String> clientUriStatusCodes = new LinkedList<>();
		for(String lang : clientUris.keySet()) {
			String uri = clientUris.get(lang);
			try
			{
				HttpResponse response = HttpUtil.headRequest(uri);
				if (response == null) {
					appendError("failure_reason", "Failed to fetch " + getMetadataName(),
						"details", args("uri", uri));
					continue;
				}
				if (response.getStatusLine().getStatusCode() > 399) {
					appendError("failure_reason", "Server returned an error for the " + getMetadataName(),
						"details", args("uri", uri, "http_status_code", response.getStatusLine().getStatusCode()));
					continue;
				} else {
					clientUriStatusCodes.add(uri + " : " + response.getStatusLine());
				}
			} catch (HttpUtil.HttpUtilException ex) {
				appendError("failure_reason", "Http error",
					"details", args("uri", uri, "exception", ex.getCause().getMessage()));
			}
		}
		if(!validationErrors.isEmpty()) {
			throw error(getMetadataName() + " validation failed", args("errors", validationErrors));
		}
		logSuccess("Client contains valid "+getMetadataName()+" value(s)", args("uri_status_codes", clientUriStatusCodes));
		return env;
	}
}
