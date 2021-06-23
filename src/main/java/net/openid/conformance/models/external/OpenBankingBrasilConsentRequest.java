package net.openid.conformance.models.external;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

public class OpenBankingBrasilConsentRequest {

	private JsonObject payload;

	public OpenBankingBrasilConsentRequest(String cpf, String cnpj, String...permissions) {
		// see https://openbanking-brasil.github.io/areadesenvolvedor/#tocS_CreateConsent

		JsonObject data = new JsonObject();
		JsonArray permissionsArray = new JsonArray();
		Arrays.stream(permissions).forEach(p -> permissionsArray.add(p));
		data.add("permissions", permissionsArray);
		if (cpf != null) {
			JsonObject loggedUser = new JsonObject();
			JsonObject document = new JsonObject();
			document.addProperty("identification", cpf);
			document.addProperty("rel", "CPF");
			loggedUser.add("document", document);
			data.add("loggedUser", loggedUser);
		}
		if (cnpj != null) {
			JsonObject businessEntity = new JsonObject();
			JsonObject document = new JsonObject();
			document.addProperty("identification", cnpj);
			document.addProperty("rel", "CNPJ");
			businessEntity.add("document", document);
			data.add("businessEntity", businessEntity);
		}
		payload = new JsonObject();
		payload.add("data", data);
	}

	public JsonObject toJson() {
		return payload;
	}

}
