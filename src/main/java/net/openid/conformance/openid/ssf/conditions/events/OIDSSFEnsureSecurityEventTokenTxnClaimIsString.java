package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Checks the value of a present {@code txn} claim. RFC 8417 section 2.2 defines {@code txn} as
 * "An OPTIONAL string value that represents a unique transaction identifier", and SSF 1.0
 * section 4.1.9 requires a present value to be "unique to the underlying event", which an
 * empty string cannot be. An absent claim passes; its absence is graded separately by
 * {@link OIDSSFWarnSecurityEventTokenTxnClaimMissing}.
 * Reads the parsed SET from {@code set_token.claims}.
 */
public class OIDSSFEnsureSecurityEventTokenTxnClaimIsString extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement txnEl = env.getElementFromObject("set_token", "claims.txn");
		if (txnEl == null) {
			log("The SET contains no 'txn' claim; nothing to check");
			return env;
		}

		if (!OIDFJSON.isString(txnEl)) {
			throw error("The 'txn' claim must be a JSON string", args("txn", txnEl));
		}

		if (OIDFJSON.getString(txnEl).isEmpty()) {
			throw error("The 'txn' claim must not be an empty string, as its value must identify the underlying event", args("txn", txnEl));
		}

		logSuccess("The 'txn' claim is a non-empty string", args("txn", txnEl));

		return env;
	}
}
