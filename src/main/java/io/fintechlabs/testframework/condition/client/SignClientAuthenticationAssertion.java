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

package io.fintechlabs.testframework.condition.client;

import java.text.ParseException;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
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

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class SignClientAuthenticationAssertion extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public SignClientAuthenticationAssertion(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "client_assertion_claims", "jwks" })
	@PostEnvironment(strings = "client_assertion")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.get("client_assertion_claims");
		JsonObject jwks = env.get("jwks");

		if (claims == null) {
			throw error("Couldn't find claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
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
					throw error("Couldn't create signer from key", args("jwk", jwk.toJSONString()));
				}

				Algorithm alg = jwk.getAlgorithm();
				if (alg == null) {
					throw error("No 'alg' field specified in key", args("jwk", jwk.toJSONString()));
				}

				JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), null, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

				SignedJWT assertion = new SignedJWT(header, claimSet);

				assertion.sign(signer);

				env.putString("client_assertion", assertion.serialize());

				logSuccess("Signed the client assertion", args("client_assertion", assertion.serialize()));

				return env;

			} else {
				throw error("Expected only one JWK in the set", args("found", jwkSet.getKeys().size()));
			}

		} catch (ParseException e) {
			throw error(e);
		} catch (JOSEException e) {
			throw error(e);
		}

	}

}
