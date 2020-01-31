package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * request_object_encryption_enc
 * OPTIONAL. JWE enc algorithm [JWA] the RP is declaring that it may use for encrypting
 * Request Objects sent to the OP. If request_object_encryption_alg is specified, the default
 * for this value is A128CBC-HS256. When request_object_encryption_enc is included,
 * request_object_encryption_alg MUST also be provided.
 *
 */
public class EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		String enc = getRequestObjectEncryptionEnc();
		String alg = getRequestObjectEncryptionAlg();
		if(enc!=null && alg==null) {
			throw error("When request_object_encryption_enc is included, request_object_encryption_alg MUST " +
						"also be provided.",
						args("request_object_encryption_alg", alg, "request_object_encryption_enc", enc));
		}
		if(enc == null) {
			logSuccess("request_object_encryption_enc is not set");
			return env;
		}
		logSuccess("request_object_encryption_alg is set",
					args("request_object_encryption_alg", alg, "request_object_encryption_enc", enc));
		return env;
	}
}
