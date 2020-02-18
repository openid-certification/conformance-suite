package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * contacts
 * OPTIONAL. Array of e-mail addresses of people responsible for this Client.
 * This might be used by some providers to enable a Web user interface to modify the Client information.
 *
 * Although contacts is optional, the python suite requires at least one contact in registration requests.
 * Python suite also checks if the first entry in contacts contains a @ character
 * See Provider.registration_endpoint in oidctest/src/oidctest/rp/provider.py
 */
public class EnsureRegistrationRequestContainsAtLeastOneContact extends AbstractCondition {
	private static final String CONTACTS = "contacts";
	@Override
	@PreEnvironment(required = { "dynamic_registration_request"})
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("dynamic_registration_request");
		if(!request.has(CONTACTS)) {
			throw error("This application requires that registration requests contain at least one contact.");
		}
		if(!request.get(CONTACTS).isJsonArray()) {
			throw error("This application requires that registration requests contain at least one contact. " +
						"Provided contacts is not encoded as a json array");
		}
		JsonArray contactsArray = request.get(CONTACTS).getAsJsonArray();
		if(contactsArray.size()<1) {
			throw error("This application requires that registration requests contain at least one contact. " +
				"Provided contacts array is empty");
		}
		for(JsonElement element : contactsArray) {
			String contactFromElement = OIDFJSON.getString(element);
			if(contactFromElement!=null && !contactFromElement.contains("@")) {
				throw error("Invalid contact. Only email addresses are expected in contacts",
							args("contact", element));
			}
		}
		logSuccess("Registration request contains valid contacts", args("contacts", contactsArray));
		return env;
	}
}
