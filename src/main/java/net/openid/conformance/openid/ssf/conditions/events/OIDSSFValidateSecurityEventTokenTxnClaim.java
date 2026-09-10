package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks the {@code txn} claim of a SET per
 * <a href="https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html#section-4.1.9">SSF 1.0 Section 4.1.9</a>:
 * "Transmitters SHOULD set the "txn" claim value in Security Event Tokens (SETs)."
 * <p>
 * Throws when {@code txn} is absent (callers should invoke this at WARNING severity since it is a SHOULD) and
 * when it is present but not a non-empty JSON string (RFC 8417 Section 2.2 defines {@code txn} as a string).
 * <p>
 * No uniqueness check across SETs is performed: Section 4.1.9 says the value "MUST be unique to the underlying
 * event", but "the Transmitter ... may use the same value in the "txn" claim across different Security Event
 * Tokens (SETs) ... to indicate that the SETs originated from the same underlying cause or reason", so a
 * repeated value is not evidence of a violation.
 * Reads the parsed SET from {@code set_token.claims}.
 */
public class OIDSSFValidateSecurityEventTokenTxnClaim extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement txnEl = env.getElementFromObject("set_token", "claims.txn");
		if (txnEl == null) {
			throw error("The SET does not contain a 'txn' claim; Transmitters SHOULD set the 'txn' claim",
				args("claims", env.getElementFromObject("set_token", "claims")));
		}

		if (!txnEl.isJsonPrimitive() || !txnEl.getAsJsonPrimitive().isString()) {
			throw error("The 'txn' claim MUST be a JSON string", args("txn", txnEl));
		}

		if (OIDFJSON.getString(txnEl).isEmpty()) {
			throw error("The 'txn' claim MUST NOT be an empty string", args("txn", txnEl));
		}

		logSuccess("The SET contains a 'txn' claim", args("txn", txnEl));

		return env;
	}
}
