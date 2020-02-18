package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;

/**
 * https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
 * jwks
 * ...
 *     When both signing and
 *     encryption keys are made available, a use (Key Use) parameter value is REQUIRED
 *     for all keys in the referenced JWK Set to indicate each key's intended usage.
 * ...
 */
public class EnsureClientJwksContainsKeyUseForAllKeysIfJwksContainsBothSigAndEncKeys extends AbstractCondition
{

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		JsonElement jwks = env.getElementFromObject("client", "jwks");

		try
		{
			JWKSet jwkSet = JWKUtil.parseJWKSet(jwks.toString());
			int totalKeyCount = 0;
			int sigKeyCount = 0;
			int encKeyCount = 0;
			int keysWithUseCount = 0;
			for(JWK key : jwkSet.getKeys()) {
				totalKeyCount++;
				if(key.getKeyUse() == null || key.getKeyUse().equals(KeyUse.SIGNATURE)) {
					sigKeyCount++;
				}
				if(key.getKeyUse() == null || key.getKeyUse().equals(KeyUse.ENCRYPTION)) {
					encKeyCount++;
				}
				if(key.getKeyUse()!=null) {
					keysWithUseCount++;
				}
			}
			if(totalKeyCount == keysWithUseCount) {
				logSuccess("All keys have use (Key Use) parameter values");
				return env;
			}
			if(sigKeyCount == 0 && encKeyCount == 0) {
				logSuccess("No keys found");
				return env;
			}
			if(sigKeyCount > 0 && encKeyCount > 0)
			{
				throw error("When both signing and " +
					"encryption keys are made available, a use (Key Use) parameter value is REQUIRED " +
					"for all keys in the referenced JWK Set to indicate each key's intended usage");
			} else if(sigKeyCount>0) {
				logSuccess("Jwks is not required to have key use parameters because it only contains signing keys");
			} else {
				logSuccess("Jwks is not required to have key use parameters because it only contains encryption keys");
			}
		}
		catch (ParseException e)
		{
			throw error("Failed to parse client jwks", e, args("jwks", jwks));
		}

		return env;
	}
}
