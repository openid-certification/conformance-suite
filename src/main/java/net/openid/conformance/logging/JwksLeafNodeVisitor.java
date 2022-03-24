package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

public class JwksLeafNodeVisitor implements JsonLeafNodeVisitor, MapLeafNodeVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(JwksLeafNodeVisitor.class);

	@Override
	public void accept(JsonObjectSanitiser.LeafNode leafNode) {
		if(leafNode.getType() != LeafType.JWKS) {
			return;
		}
		String plainText = leafNode.getProperty().toString();
		try {
			JWKSet jwkSet = JWKSet.parse(plainText);
			JWKSet publicJWKSet = jwkSet.toPublicJWKSet();
			String s = publicJWKSet.toString();
			JsonElement element = new JsonParser().parse(s);
			leafNode.replace(element);
			if(jwkSet.equals(publicJWKSet)) {
				LOG.info("On {} - appeared to already be a public set {}", leafNode.getKey(), s);
			} else {
				LOG.info("On {} - Replaced private key jwks with public {}", leafNode.getKey(), s);
			}
		} catch (ParseException e) {
		}
	}

	@Override
	public void accept(MapSanitiser.LeafNode leafNode) {
		if(leafNode.getType() != LeafType.JWKS) {
			return;
		}
		Object value = leafNode.getProperty();
		if(value instanceof String) {
			String plainText = leafNode.getProperty().toString();
			try {
				JWKSet jwkSet = JWKSet.parse(plainText);
				jwkSet = jwkSet.toPublicJWKSet();
				String s = jwkSet.toString();
				JsonElement element = new JsonParser().parse(s);
				leafNode.replace(element);
			} catch (ParseException e) {
				// NOOP
			}
		}
		if(value instanceof JsonObject) {
			String plainText = leafNode.getProperty().toString();
			try {
				JWKSet jwkSet = JWKSet.parse(plainText);
				JWKSet publicJWKSet = jwkSet.toPublicJWKSet();
				String s = publicJWKSet.toString();
				JsonElement element = new JsonParser().parse(s);
				leafNode.replace(element);
				if(jwkSet.equals(publicJWKSet)) {
					LOG.info("On {} - appeared to already be a public set {}", leafNode.getKey(), s);
				} else {
					LOG.info("On {} - Replaced private key jwks with public {}", leafNode.getKey(), s);
				}
			} catch (ParseException e) {
			}
		}
		if(value instanceof JWKSet) {
			JWKSet privateJwks = (JWKSet) value;
			leafNode.replace(privateJwks.toPublicJWKSet());
		}
	}
}
