package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class FetchPoppyJson extends AbstractPoppyFetch {

	static final String WELL_KNOWN_PATH = "/.well-known/poppy.json";

	static final int MAX_HOPS = 5;

	static URI startUrl(String domain) {
		return URI.create("https://" + domain + WELL_KNOWN_PATH);
	}

	/**
	 * Checks the chain of URLs a fetch of poppy.json went through, the last one being the one that served it.
	 */
	static void checkHops(List<URI> hops) {
		for (URI hop : hops) {
			if (!"https".equals(hop.getScheme())) {
				throw new IllegalArgumentException("poppy.json request or redirect uses a non-HTTPS URL: " + hop);
			}
		}
		URI last = hops.get(hops.size() - 1);
		if (!WELL_KNOWN_PATH.equals(last.getRawPath()) || last.getRawQuery() != null) {
			throw new IllegalArgumentException("poppy.json was served from " + last + " but must be served from exactly " + WELL_KNOWN_PATH + " with no query");
		}
		if (last.getPort() != -1) {
			throw new IllegalArgumentException("poppy.json was served from " + last + ", which has an explicit port; Poppy requires the default HTTPS port");
		}
	}

	@Override
	@PreEnvironment(strings = "poppy_domain")
	@PostEnvironment(required = "poppy_json", strings = "poppy_json_final_url")
	public Environment evaluate(Environment env) {
		URI current = startUrl(env.getString("poppy_domain"));
		List<URI> hops = new ArrayList<>();
		hops.add(current);
		ResponseEntity<String> response = get(env, current);
		while (response.getStatusCode().is3xxRedirection() && response.getHeaders().getLocation() != null) {
			if (hops.size() > MAX_HOPS) {
				throw error("poppy.json redirected more than " + MAX_HOPS + " times", args("hops", hopStrings(hops)));
			}
			current = current.resolve(response.getHeaders().getLocation());
			hops.add(current);
			if (!"https".equals(current.getScheme())) {
				break;
			}
			response = get(env, current);
		}
		try {
			checkHops(hops);
		} catch (IllegalArgumentException e) {
			throw error(e.getMessage(), args("hops", hopStrings(hops)));
		}
		JsonObject json = requireJsonObject(response, current, "poppy.json");
		env.putObject("poppy_json", json);
		env.putString("poppy_json_final_url", current.toString());
		logSuccess("Fetched poppy.json", args("final_url", current.toString(), "hops", hopStrings(hops), "poppy_json", json));
		return env;
	}

	private static List<String> hopStrings(List<URI> hops) {
		return hops.stream().map(URI::toString).toList();
	}
}
