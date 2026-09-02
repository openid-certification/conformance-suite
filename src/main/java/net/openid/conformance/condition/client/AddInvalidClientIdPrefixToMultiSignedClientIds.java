package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Multi-signed counterpart of {@link AddInvalidClientIdPrefixToRequestObject}.
 *
 * For multi-signed requests (OID4VP Appendix A.3.2.2) client_id is not in the shared payload; each
 * signature carries its own client_id in its protected header, taken from the {@code client_id} and
 * {@code client2_id} environment strings when the request is signed. Both are rewritten here: a wallet
 * may fall back to any signature whose client_id it recognises (Section 5.9.2), so leaving either one
 * intact would let a conformant wallet complete the flow.
 */
public class AddInvalidClientIdPrefixToMultiSignedClientIds extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"client_id", "client2_id"})
	@PostEnvironment(strings = {"client_id", "client2_id"})
	public Environment evaluate(Environment env) {

		String originalClientId = env.getString("client_id");
		String originalClient2Id = env.getString("client2_id");

		String invalidClientId = AddInvalidClientIdPrefixToRequestObject.withInvalidPrefix(originalClientId);
		String invalidClient2Id = AddInvalidClientIdPrefixToRequestObject.withInvalidPrefix(originalClient2Id);

		env.putString("client_id", invalidClientId);
		env.putString("client2_id", invalidClient2Id);

		log("Replaced both multi-signed client_ids with invalid prefix scheme",
			args("original_client_id", originalClientId, "invalid_client_id", invalidClientId,
				"original_client2_id", originalClient2Id, "invalid_client2_id", invalidClient2Id));

		return env;
	}
}
