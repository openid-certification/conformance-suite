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

package io.fintechlabs.testframework.security;

import java.security.PrivateKey;
import java.text.ParseException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * @author jricher
 *
 */
public class KeyManager {


	@Value("${fintechlabs.jwks}")
	private String jwksString;

	@Value("${fintechlabs.signingKey}")
	private String signingKeyId;

	private JWKSet jwkSet;

	@PostConstruct
	public void initializeKeyManager() {
		// parse the string as a JWK Set
		try {
			jwkSet = JWKSet.parse(jwksString);

			// make sure the jwkSet has a key with the indicated ID
			JWK jwk = jwkSet.getKeyByKeyId(signingKeyId);

			if (jwk == null) {
				throw new IllegalStateException("Couldn't find the signing key " + signingKeyId);
			}

		} catch (ParseException e) {
			throw new IllegalStateException("Error trying to build a JWK Set", e);
		}
	}

	public PrivateKey getSigningPrivateKey() {
		JWK signingKey = jwkSet.getKeyByKeyId(signingKeyId);
		KeyType keyType = signingKey.getKeyType();

		try {
			if (keyType.equals(KeyType.RSA)) {
				return ((RSAKey)signingKey).toPrivateKey();
			} else if (keyType.equals(KeyType.EC)) {
				return ((ECKey)signingKey).toPrivateKey();
			} else if (keyType.equals(KeyType.OKP)) {
				return ((OctetKeyPair)signingKey).toPrivateKey();
			} else {
				return null;
			}
		} catch (JOSEException e) {
			return null;
		}
	}

	/**
	 * Get only the public keys in this key set.
	 */
	public JWKSet getPublicKeys() {
		return jwkSet.toPublicJWKSet();
	}

}
