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
import com.nimbusds.jose.JOSEObjectType;
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

public class SignRequestObject extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public SignRequestObject(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "request_object_claims", "client_jwks" })
	@PostEnvironment(strings = "request_object")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.get("request_object_claims");
		JsonObject jwks = env.get("client_jwks");

		if (requestObjectClaims == null) {
			throw error("Couldn't find request object claims");
		}

		if (jwks == null) {
			throw error("Couldn't find jwks");
		}

		if (!requestObjectClaims.has("iss")) {
			String clientId = env.getString("client", "client_id");
			if (clientId != null) {
				requestObjectClaims.addProperty("iss", clientId);
			} else {
				// Only a "should" requirement
				log("Request object contains no issuer and client ID not found");
			}
		}

		if (!requestObjectClaims.has("aud")) {
			String serverIssuerUrl = env.getString("server", "issuer");
			if (serverIssuerUrl != null) {
				requestObjectClaims.addProperty("aud", serverIssuerUrl);
			} else {
				// Only a "should" requirement
				log("Request object contains no audience and server issuer URL not found");
			}
		}

		try {
			JWTClaimsSet claimSet = JWTClaimsSet.parse(requestObjectClaims.toString());

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
					throw error("key should contain an 'alg' entry", args("jwk", jwk.toJSONString()));
				}

				JWSHeader header = new JWSHeader(JWSAlgorithm.parse(alg.getName()), JOSEObjectType.JWT, null, null, null, null, null, null, null, null, jwk.getKeyID(), null, null);

				SignedJWT requestObject = new SignedJWT(header, claimSet);

				requestObject.sign(signer);

				env.putString("request_object", requestObject.serialize());

				logSuccess("Signed the request object", args("request_object", requestObject.serialize(),
					"header", header.toString(),
					"claims", claimSet.toString(),
					"key", jwk.toJSONString()));

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
