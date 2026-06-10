package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reverses the order of the issued credentials so the response order deliberately differs
 * from the order of the proofs in the credential request. This is legal issuer behavior:
 * OID4VCI 1.0 Final §8.3 defines no correspondence between the order of the credentials
 * array and the order of the proofs - wallets must identify the binding key from each
 * credential itself.
 */
public class VCIReverseCredentialIssuanceOrder extends AbstractCondition {

	@Override
	@PreEnvironment(required = "credential_issuance")
	public Environment evaluate(Environment env) {

		JsonArray credentials = env.getObject("credential_issuance").getAsJsonArray("credentials");

		if (credentials.size() < 2) {
			log("Only " + credentials.size() + " credential(s) issued; nothing to reorder");
			return env;
		}

		JsonArray reversed = new JsonArray();
		for (int i = credentials.size() - 1; i >= 0; i--) {
			reversed.add(credentials.get(i));
		}
		// replace contents in place so all references to credential_issuance see the new order
		while (!credentials.isEmpty()) {
			credentials.remove(credentials.size() - 1);
		}
		for (JsonElement credential : reversed) {
			credentials.add(credential);
		}

		logSuccess("Reversed the order of the " + credentials.size() + " issued credentials relative to the "
				+ "proofs in the credential request; OID4VCI defines no ordering for the credentials array, so "
				+ "the wallet must not rely on it",
			args("credential_count", credentials.size()));

		return env;
	}
}
