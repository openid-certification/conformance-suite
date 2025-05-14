package net.openid.conformance.vciid2issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIParseCredentialOfferRequest extends AbstractCondition {

	private final JsonObject requestParts;

	public VCIParseCredentialOfferRequest(JsonObject requestParts) {
		this.requestParts = requestParts;
	}

	@Override
	public Environment evaluate(Environment env) {



		return env;
	}
}
