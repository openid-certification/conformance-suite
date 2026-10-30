package net.openid.conformance.condition.common.sockets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.function.Function;

public class Payloads {

	public static Function<Environment, MultiValueMap<String,String>> formBody(String envObjectName) {
		return  env -> {
			JsonObject formJson = env.getObject(envObjectName);
			MultiValueMap<String, String> form = new LinkedMultiValueMap <>();
			for (String key : formJson.keySet()) {
				JsonElement el = formJson.get(key);
				if (el.isJsonObject()) {
					// e.g. claims parameter
					form.add(key, el.toString());
				} else if (el.isJsonArray()) {
					form.add(key, el.getAsJsonArray().toString());
				} else {
					form.add(key, OIDFJSON.getString(el));
				}
			}
			return form;
		};
	}
}
