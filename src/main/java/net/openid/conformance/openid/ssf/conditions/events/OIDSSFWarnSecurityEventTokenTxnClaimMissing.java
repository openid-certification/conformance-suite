package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that a SET carries a {@code txn} claim. SSF 1.0 section 4.1.9: "Transmitters SHOULD
 * set the "txn" claim value in Security Event Tokens (SETs)", so callers invoke this at
 * WARNING severity. The value itself is checked by
 * {@link OIDSSFEnsureSecurityEventTokenTxnClaimIsString}.
 * <p>
 * No uniqueness check across SETs is performed: section 4.1.9 says the value "MUST be unique
 * to the underlying event", but "the Transmitter ... may use the same value in the "txn" claim
 * across different Security Event Tokens (SETs) ... to indicate that the SETs originated from
 * the same underlying cause or reason", so a repeated value is not evidence of a violation.
 * Reads the parsed SET from {@code set_token.claims}.
 */
public class OIDSSFWarnSecurityEventTokenTxnClaimMissing extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement txnEl = env.getElementFromObject("set_token", "claims.txn");
		if (txnEl == null) {
			throw error("The SET does not contain a 'txn' claim; Transmitters SHOULD set the 'txn' claim",
				args("claims", env.getElementFromObject("set_token", "claims")));
		}

		logSuccess("The SET contains a 'txn' claim", args("txn", txnEl));

		return env;
	}
}
