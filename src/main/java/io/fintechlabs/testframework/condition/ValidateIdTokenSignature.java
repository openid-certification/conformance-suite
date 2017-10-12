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

import java.security.Key;
import java.text.ParseException;
import java.util.List;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.SignedJWT;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ValidateIdTokenSignature extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public ValidateIdTokenSignature(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-1-5.2.2-24");
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObj("id_token")) {
			return error("Couldn't find parsed ID token");
		}

		if (!env.containsObj("server_jwks")) {
			return error("Couldn't find server's public key");
		}

		String idToken = env.getString("id_token", "value");
		JsonObject serverJwks = env.get("server_jwks"); // to validate the signature

		try {
			// translate stored items into nimbus objects
			SignedJWT jwt = SignedJWT.parse(idToken);
			JWKSet jwkSet = JWKSet.parse(serverJwks.toString());
	
			SecurityContext context = new SimpleSecurityContext();
			
			JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
			
			JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(jwt.getHeader().getAlgorithm(), jwkSource);
			
			List<? extends Key> keys = selector.selectJWSKeys(jwt.getHeader(), context);
			for (Key key : keys) {
				JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
				JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), key);
				
				if (jwt.verify(verifier)) {
					logSuccess();
					return env;
				} else {
					// failed to verify with this key, moving on
				}
			}
			
			// if we got here, it hasn't been verified on any key
			error("Unable to verify ID token signature based on server keys");
			return env;
			
 		} catch (JOSEException | ParseException e) {
			return error("Error validating ID Token signature", e);
		}
		
	}

}
