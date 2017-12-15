/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class SignIdToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public SignIdToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"id_token_claims", "jwks"})
	@PostEnvironment(strings = "id_token")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.get("id_token_claims");
		JsonObject jwks = env.get("jwks");
		
		if (claims == null) {
			return error("Couldn't find claims");
		}
		
		if (jwks == null) {
			return error("Couldn't find jwks");
		}
		
		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());
			
			JWKSet jwkSet = JWKSet.parse(jwks.toString());
			
			if (jwkSet.getKeys().size() == 1) {
				// figure out which algorithm to use
				JWK jwk = jwkSet.getKeys().iterator().next();
				
				JWSSigner signer = null;
				if (jwk.getKeyType().equals(KeyType.RSA)) {
					signer = new RSASSASigner((RSAKey) jwk);
				} else if (jwk.getKeyType().equals(KeyType.EC)) {
					signer = new ECDSASigner((ECKey) jwk);
				} else if (jwk.getKeyType().equals(KeyType.OCT)) {
					signer = new MACSigner((OctetSequenceKey) jwk);
				}
				
				if (signer == null) {
					return error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
				}
				
				JWSHeader header = new JWSHeader(JWSAlgorithm.parse(jwk.getAlgorithm().getName()), null, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);
				
				SignedJWT idToken = new SignedJWT(header, claimSet);
				
				idToken.sign(signer);
				
				env.putString("id_token", idToken.serialize());
				
				logSuccess("Signed the ID token", args("id_token", idToken.serialize()));
				
				return env;
				
			} else {
				return error("Expected only one JWK in the set", args("found", jwkSet.getKeys().size()));
			}
			
			
			
		} catch (ParseException e) {
			return error(e);
		} catch (JOSEException e) {
			return error(e);
		}
		
	}

}
