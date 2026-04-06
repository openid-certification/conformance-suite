package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObjectJSON;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class CreateMultiSignedRequestObject extends AbstractGetSigningKey {

	@Override
	@PreEnvironment(required = {"request_object_claims", "client_jwks", "client2_jwks"}, strings = {"client_id", "client2_id"})
	@PostEnvironment(required = "request_object_json")
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("request_object_claims").deepCopy();
		String client1Id = env.getString("client_id");
		String client2Id = env.getString("client2_id");

		// Defensive: remove client_id from the shared payload if present, since for multi-signed
		// it belongs in each signature's protected header instead
		claims.remove("client_id");

		try {
			Payload payload = new Payload(claims.toString());
			JWSObjectJSON jwsObjectJSON = new JWSObjectJSON(payload);

			JWSSignerFactory jwsSignerFactory = MultiJWSSignerFactory.getInstance();

			// First signature: primary client key
			addSignature(jwsObjectJSON, jwsSignerFactory, env.getObject("client_jwks"), client1Id);

			// Second signature: secondary client key
			addSignature(jwsObjectJSON, jwsSignerFactory, env.getObject("client2_jwks"), client2Id);

			String serialized = jwsObjectJSON.serializeGeneral();
			JsonObject requestObjectJson = JsonParser.parseString(serialized).getAsJsonObject();

			env.putObject("request_object_json", requestObjectJson);

			logSuccess("Created multi-signed request object with JWS JSON Serialization",
				args("request_object_json", requestObjectJson));

			return env;

		} catch (JOSEException e) {
			throw error("Failed to sign multi-signed request object", e);
		} catch (ParseException e) {
			throw error("Failed to parse signing key", e);
		}
	}

	private void addSignature(JWSObjectJSON jwsObjectJSON, JWSSignerFactory jwsSignerFactory,
							  JsonObject jwks, String clientId) throws JOSEException, ParseException {
		JWK signingJwk = getSigningKey("signing", jwks);
		JWSAlgorithm alg = JWSAlgorithm.parse(signingJwk.getAlgorithm().getName());

		JWSHeader.Builder builder = new JWSHeader.Builder(alg)
			.type(new JOSEObjectType("oauth-authz-req+jwt"))
			.keyID(signingJwk.getKeyID())
			.customParam("client_id", clientId);

		if (signingJwk.getX509CertChain() != null) {
			builder.x509CertChain(signingJwk.getX509CertChain());
		}

		JWSHeader header = builder.build();
		JWSSigner signer = jwsSignerFactory.createJWSSigner(signingJwk, alg);
		jwsObjectJSON.sign(header, signer);
	}

}
