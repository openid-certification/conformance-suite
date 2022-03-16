package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static net.openid.conformance.logging.MapCopy.deepCopy;

@Component
@Primary
public class SanitisingEventLog implements EventLog {

	private static final Logger LOG = LoggerFactory.getLogger(SanitisingEventLog.class);
	private final JsonObjectSanitiser jsonObjectSanitiser;
	private final MapSanitiser mapSanitiser;

	private EventLog delegate;

	@Autowired(required = true)
	public SanitisingEventLog(EventLog delegate, JsonObjectSanitiser jsonObjectSanitiser, MapSanitiser mapSanitiser) {
		this.delegate = delegate;
		this.jsonObjectSanitiser = jsonObjectSanitiser;
		this.mapSanitiser = mapSanitiser;
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, String msg) {
		delegate.log(testId, source, owner, msg);
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, JsonObject obj) {
		JsonObject ret = new JsonParser().parse(obj.toString()).getAsJsonObject();
		LOG.info("Sanitising JsonObject in condtion {} of test {}", source, testId);
		jsonObjectSanitiser.sanitise(ret);
		delegate.log(testId, source, owner, ret);
	}

	@Override
	public void log(String testId, String source, Map<String, String> owner, Map<String, Object> map) {
		map = deepCopy(map);
		LOG.info("Sanitising Map in condtion {} of test {}", source, testId);
		mapSanitiser.sanitise(map);
		delegate.log(testId, source, owner, map);
	}

	@Override
	public void createIndexes() {
		delegate.createIndexes();
	}

	private Map<String, Object> sanitise(Map<String, Object> map) {
		Map<String, Object> ret = new HashMap<>(map);
		for(String k: ret.keySet()) {
			if(k.endsWith("jwks")) {
				JsonObject jwks = (JsonObject) ret.get(k);
				jwks = new JsonParser().parse(jwks.toString()).getAsJsonObject();
				ret.put(k, jwks);
				JsonArray keys = jwks.getAsJsonArray("keys");
				JsonArray sanitisedKeys = new JsonArray();
				jwks.add("keys", sanitisedKeys);
				keys.forEach(jwk -> {
					JsonObject jwkObj = jwk.getAsJsonObject();
					JsonObject sanitisedJwk = new JsonObject();
					sanitisedJwk.add("kid", jwkObj.get("kid"));
					sanitisedKeys.add(sanitisedJwk);
				});
			}
		}
		return ret;
	}

}
