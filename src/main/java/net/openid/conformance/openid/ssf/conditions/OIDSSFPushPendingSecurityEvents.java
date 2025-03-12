package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class OIDSSFPushPendingSecurityEvents extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// read current event from env

		String pushToken = env.getString("ssf","stream.config.delivery.authorization_header");
		String pushUrl = env.getString("ssf", "stream.config.delivery.endpoint_url");

		JsonElement sets = env.getElementFromObject("ssf", "stream.sets");
		JsonObject setsObj = sets.getAsJsonObject();

		Map.Entry<String, JsonElement> setEntry = setsObj.entrySet().iterator().next();
		String setJti = setEntry.getKey();
		String set = OIDFJSON.getString(setEntry.getValue());

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/secevent+jwt"));
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(pushToken);

		ResponseEntity<?> responseEntity = restTemplate.exchange(pushUrl, HttpMethod.POST, new HttpEntity<>(set, headers), Map.class);

		log("Delivered verification event to push endpoint.", args("status_code", responseEntity.getStatusCode()));

		Set<String> setJtisToAck = Set.of(setJti);
		if (responseEntity.getStatusCode().value() == HttpStatus.ACCEPTED.value()) {
			// TODO mark event as consumed
			JsonArray acks = OIDFJSON.convertSetToJsonArray(setJtisToAck);
			env.putArray("ssf", "stream.feedback.acks", acks);
		}

		logSuccess("Pushed and acknowledged pending SSF security events", args("status_code", responseEntity.getStatusCode(), "acks", setJtisToAck));

		return env;
	}
}
