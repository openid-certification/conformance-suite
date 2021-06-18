package net.openid.conformance.models.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

public class OpenBankingBrasilConsentRequest {

	private JsonObject payload;

	public OpenBankingBrasilConsentRequest(String cpf, String...permissions) {
		JsonObject data = new JsonObject();
		JsonArray permissionsArray = new JsonArray();
		Arrays.stream(permissions).forEach(p -> permissionsArray.add(p));
		data.add("permissions", permissionsArray);
		JsonObject loggedUser = new JsonObject();
		JsonObject document = new JsonObject();
		document.addProperty("identification", cpf);
		document.addProperty("rel", "CPF");
		loggedUser.add("document", document);
		data.add("loggedUser", loggedUser);
		payload = new JsonObject();
		payload.add("data", data);
	}

	public JsonObject toJson() {
		return payload;
	}

}
