package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static net.openid.conformance.logging.MapCopy.deepCopy;
import static org.junit.Assert.assertEquals;

public class MapSanitiserTests {

	@Test
	public void findLeavesOfJsonObject() throws Exception {

		KeyPair firstKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair secondKeypair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair thirdKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair fourthPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

		String privateKeyString = Base64.getEncoder().encodeToString(firstKeyPair.getPrivate().getEncoded());
		String publicKeyString = Base64.getEncoder().encodeToString(firstKeyPair.getPublic().getEncoded());

		JWK jwk = new RSAKey.Builder((RSAPublicKey) secondKeypair.getPublic())
			.privateKey((RSAPrivateKey) secondKeypair.getPrivate())
			.keyUse(KeyUse.SIGNATURE)
			.keyID(UUID.randomUUID().toString())
			.build();
		JWKSet jwkSet1 = new JWKSet(jwk);

		jwk = new RSAKey.Builder((RSAPublicKey) secondKeypair.getPublic())
			.privateKey((RSAPrivateKey) thirdKeyPair.getPrivate())
			.keyUse(KeyUse.SIGNATURE)
			.keyID(UUID.randomUUID().toString())
			.build();
		JWKSet jwkSet2 = new JWKSet(jwk);

		Map<String, Object> messageMap = deepCopy(Map.of(
			"something", "Not relevant",
			"other", Map.of("key", "value"),
			"foo", Map.of("bar", Map.of("wibble", privateKeyString)),
			"jwks", jwkSet1,
			"batman", Map.of("sharkspray", jwkSet2),
			"actualKey", fourthPair.getPrivate(),
			"pie", Map.of("chips", Map.of("keys", "notajwks"))
		));

		Map<String, Object> interim;

		MapSanitiser mapSanitiser = new MapSanitiser(Set.of(new PrivateKeyLeafVisitor(), new JwksLeafNodeVisitor()));
		Set<MapSanitiser.LeafNode> leafNodes = mapSanitiser.findLeafNodes(messageMap);

		assertEquals(7, leafNodes.size());

		mapSanitiser.sanitise(leafNodes);

		assertEquals("Not relevant", messageMap.get("something"));
		interim = (Map<String, Object>) messageMap.get("other");
		assertEquals("value", interim.get("key"));
		interim = (Map<String, Object>) messageMap.get("foo");
		interim = (Map<String, Object>) interim.get("bar");

		assertEquals(publicKeyString, interim.get("wibble"));
		JWKSet jwkSetFound1 = (JWKSet) messageMap.get("jwks");
		JWK publicFirst = jwkSetFound1.getKeys().get(0);
		JWK privateFirst = jwkSet1.getKeys().get(0);
		assertEquals(privateFirst.toPublicJWK(), publicFirst);

		interim = (Map<String, Object>) messageMap.get("batman");
		jwkSetFound1 = (JWKSet) interim.get("sharkspray");

		publicFirst = jwkSetFound1.getKeys().get(0);
		privateFirst = jwkSet2.getKeys().get(0);
		assertEquals(privateFirst.toPublicJWK(), publicFirst);

		PublicKey sanitisedPrivateKey = (PublicKey) messageMap.get("actualKey");
		assertEquals(fourthPair.getPublic(), sanitisedPrivateKey);


	}

	private JsonObject load(String name) throws IOException {
		String jsonString = IOUtils.resourceToString("jsonEnvironmentObjects/".concat(name), Charset.defaultCharset(), getClass().getClassLoader());
		return new JsonParser().parse(jsonString).getAsJsonObject();
	}



}
