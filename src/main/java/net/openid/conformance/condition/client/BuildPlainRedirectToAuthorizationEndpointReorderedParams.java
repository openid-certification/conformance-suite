package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Same as {@link BuildPlainRedirectToAuthorizationEndpoint} but sorts parameters in
 * reverse alphabetical order, to test that implementations handle different parameter orderings.
 */
public class BuildPlainRedirectToAuthorizationEndpointReorderedParams extends BuildPlainRedirectToAuthorizationEndpoint {

	@Override
	protected Collection<String> getParameterOrder(JsonObject authorizationEndpointRequest) {
		List<String> keys = new ArrayList<>(authorizationEndpointRequest.keySet());
		keys.sort(Comparator.reverseOrder());
		return keys;
	}

}
