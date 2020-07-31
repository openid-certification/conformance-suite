package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateCHash extends AbstractValidateHash {

	@Override
	@PreEnvironment(required = { "c_hash" , "authorization_endpoint_response" })
	public Environment evaluate(Environment env) {
		return super.validateHash(env,"c_hash","c_hash");
	}


}
