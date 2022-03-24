package net.openid.conformance.logging;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class PrivateKeyLeafVisitor implements JsonLeafNodeVisitor, MapLeafNodeVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(PrivateKeyLeafVisitor.class);

	@Override
	public void accept(JsonObjectSanitiser.LeafNode leafNode) {
		if(leafNode.getType() != LeafType.PRIVATE_KEY) {
			return;
		}
		JsonElement property = leafNode.getProperty();
		JsonPrimitive primitive = (JsonPrimitive) property;
		String value = OIDFJSON.getString(primitive);
		try {
			PrivateKey privateKey = readPemRsaPrivateKey(value);
			JsonObject privateKeyElement = new JsonObject();
			RSAPrivateCrtKey privk = (RSAPrivateCrtKey) privateKey;

			RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey myPublicKey = keyFactory.generatePublic(publicKeySpec);
			String s = Base64.getEncoder().encodeToString(myPublicKey.getEncoded());
			privateKeyElement.addProperty("publicKey", s);
			privateKeyElement.addProperty("privateKey", "<obfuscated for security>");
			leafNode.replace(privateKeyElement);
			LOG.info("Replaced private key in json object: {}", privateKeyElement.toString());
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
		}
	}

	@Override
	public void accept(MapSanitiser.LeafNode leafNode) {
		if(leafNode.getType() != LeafType.PRIVATE_KEY) {
			return;
		}
		Object value = leafNode.getProperty();
		if(value instanceof String) {
			String p = value.toString();
			try {
				PrivateKey privateKey = readPemRsaPrivateKey(p);
				KeyFactory kf = KeyFactory.getInstance("RSA");
				RSAPrivateKeySpec priv = kf.getKeySpec(privateKey, RSAPrivateKeySpec.class);
				RSAPublicKeySpec keySpec = new RSAPublicKeySpec(priv.getModulus(), BigInteger.valueOf(65537));
				PublicKey publicKey = kf.generatePublic(keySpec);
				String publicKeyString = Base64.getEncoder().encodeToString(publicKey.getEncoded());
				leafNode.replace(publicKeyString);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
				// NOOP
			}
		}
		if(value instanceof PrivateKey) {
			try {
				PrivateKey privateKey = (PrivateKey) value;
				KeyFactory kf = null;
				kf = KeyFactory.getInstance("RSA");
				RSAPrivateKeySpec priv = kf.getKeySpec(privateKey, RSAPrivateKeySpec.class);
				RSAPublicKeySpec keySpec = new RSAPublicKeySpec(priv.getModulus(), BigInteger.valueOf(65537));
				PublicKey publicKey = kf.generatePublic(keySpec);
				leafNode.replace(publicKey);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
				throw new RuntimeException("Unable to parse key");
			}
		}
	}

	private PrivateKey readPemRsaPrivateKey(String pemString) throws
		NoSuchAlgorithmException,
		InvalidKeySpecException
	{

		pemString = pemString.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
		pemString = pemString.replace("-----BEGIN PRIVATE KEY-----\n", "");
		pemString = pemString.replace("-----END RSA PRIVATE KEY-----", "");
		pemString = pemString.replace("-----END PRIVATE KEY-----", "");

		byte[] decoded = Base64.getDecoder().decode(pemString.getBytes(StandardCharsets.UTF_8));

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
		KeyFactory kf = KeyFactory.getInstance("RSA");

		return kf.generatePrivate(keySpec);
	}

}
