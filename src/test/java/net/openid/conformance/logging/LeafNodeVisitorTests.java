package net.openid.conformance.logging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.Base64;

import static org.junit.Assert.*;

public class LeafNodeVisitorTests {

	@Test
	public void visitJsonNodeWithoutPrivateKeyDoesNotChange() {

		JsonObject owner = new JsonObject();
		JsonElement property = new JsonPrimitive("not a private key");
		owner.add("key", property);

		JsonLeafNodeVisitor visitor = new PrivateKeyLeafVisitor();
		JsonObjectSanitiser.LeafNode leafNode = new JsonObjectSanitiser.LeafNode("test", owner,property, "key", LeafType.PRIVATE_KEY);
		visitor.accept(leafNode);

		assertEquals("not a private key", OIDFJSON.getString(owner.get("key")));

	}

	@Test
	public void visitJsonNodeWithPrivateKeyReplaces() throws NoSuchAlgorithmException {

		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

		JsonObject owner = new JsonObject();
		JsonElement property = new JsonPrimitive(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
		owner.add("key", property);

		JsonLeafNodeVisitor visitor = new PrivateKeyLeafVisitor();
		JsonObjectSanitiser.LeafNode leafNode = new JsonObjectSanitiser.LeafNode("test", owner,property, "key", LeafType.PRIVATE_KEY);
		visitor.accept(leafNode);

		JsonObject sanitisedKey = owner.get("key").getAsJsonObject();
		assertEquals(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()), OIDFJSON.getString(sanitisedKey.get("publicKey")));
		assertEquals("<obfuscated for security>", OIDFJSON.getString(sanitisedKey.get("privateKey")));

	}

	@Test
	public void visitJsonNodeWithoutJwksDoesNotChange() {

		JsonObject owner = new JsonObject();
		JsonObject property = new JsonObject();
		JsonArray keyArray = new JsonArray();
		JsonObject keyObj = new JsonObject();
		keyObj.addProperty("name", "value");
		keyArray.add(keyObj);
		property.add("keys", keyArray);
		owner.add("jwks", property);

		JsonLeafNodeVisitor visitor = new PrivateKeyLeafVisitor();
		JsonObjectSanitiser.LeafNode leafNode = new JsonObjectSanitiser.LeafNode("test", owner,property, "key", LeafType.JWKS);
		visitor.accept(leafNode);

		JsonObject nested = owner.get("jwks").getAsJsonObject();
		JsonArray array = (JsonArray) nested.get("keys");
		JsonObject element = array.get(0).getAsJsonObject();

		assertEquals("value", OIDFJSON.getString(element.get("name")));

	}

	@Test
	public void visitJsonNodeWithJwksConvertsToPublic() throws NoSuchAlgorithmException {

		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		RSAKeyGenerator generator = new RSAKeyGenerator(2048);

		JsonObject owner = new JsonObject();
		JsonObject property = new JsonObject();
		JsonArray keyArray = new JsonArray();
		JsonObject keyObj = new JsonObject();
		keyObj.addProperty("name", "value");
		keyArray.add(keyObj);
		property.add("keys", keyArray);
		owner.add("jwks", property);

		JsonLeafNodeVisitor visitor = new PrivateKeyLeafVisitor();
		JsonObjectSanitiser.LeafNode leafNode = new JsonObjectSanitiser.LeafNode("test", owner,property, "key", LeafType.JWKS);
		visitor.accept(leafNode);

		JsonObject nested = owner.get("jwks").getAsJsonObject();
		JsonArray array = (JsonArray) nested.get("keys");
		JsonObject element = array.get(0).getAsJsonObject();

		assertEquals("value", OIDFJSON.getString(element.get("name")));

	}

	@Test
	public void jwksthings() throws ParseException {
		String jwks = "{\n" +
			"            \"keys\": [\n" +
			"                {\n" +
			"                    \"p\": \"_QaFFuyZriEWtbiKexy7pIYicgU0ePMepvyNuiiFqlsUFQ24q9gp_iWECDhi8qqss__i1LW_eHQATKWfqUc6HdDY-ickJXvVktrQiT-buvJ24iTtK_NooPMKQW976NIX39_sEPJ6ceo9ZDFxTTCkmJus3eXDJmRZT2YzSZJcvcc\",\n" +
			"                    \"kty\": \"RSA\",\n" +
			"                    \"q\": \"3x1_dyovnWXSr7y7ufe6AHUV0kHBYVrGW2vdNZxKrrgBW5r2mm93NnHsrG-mJlzW7kG_jR41bWf74sucGdwXTx96riRVy8bov9SzPCDl9_QCHzPpqZOxjngfzQYq1L1qJA2BAie0Sq6YZhgLJ1fWPLutEO5soIYAkLXSJ9IVb00\",\n" +
			"                    \"d\": \"ZvivfZxJMbbdTQmxyE0lmE6ZuH8cMQOLyZxdaA5pNwm7ZEnPBEftZs8aR9ijhCDWMieui3h--rXwlXqbEm3g1sVgQ-WKTFV-NLKaJC1h-EU5HKdlOflstr7x57zhKp60ZIK69GyEXyJccUfzcD32u8raec9NplQ2MqS5MA1lnQFlocFoX1RNU4tSpEdJQq2UzqtX5WPhc88A6fTc1xu2fA5wyxzZu7fUjIETzLimcu-dDaEvvgm7c_A1ulm8EQuCN10k3FrIIe9RfuXHyxh9Rcd0aiIP9qwitxd5Cl0io7zby8MBIAaSei2co7y4tciBt4AfnzpBlGbtjgfr2gxD0Q\",\n" +
			"                    \"e\": \"AQAB\",\n" +
			"                    \"alg\": \"PS256\",\n" +
			"                    \"use\": \"sig\",\n" +
			"                    \"kid\": \"X5d4vFYfLxaG1gg8_l3bFYFhUaUVmE6PaEsRWX2EYqM\",\n" +
			"                    \"qi\": \"eHhOb5NUDfMGXwNscjEcMrMFS425qsoJAXfGJ10hm8svZOkNYgVpb7Hs7oT8XytanRl0Gk8gKH0yhvya65B5ipyby17uBMkikNN-EeYoY2AkvEfM_nO0dvHbDdF11rkM5Q_SEDlGmojxnx4_Euj8WeuSgcwiCQkR23aZlQGx1Mg\",\n" +
			"                    \"dp\": \"bQ9aXj8tHnj0qO8aAWapGokWX78OlvNzytYg4JSGyJ7pUQnRB4Ds2Lai6kgjniUiu5MX2kdceDbHykG5R-WDj0Ztv6UPV3jA3cOjDwVzwmiwBVmVQNRxzK31Ra8f4YJs9_o0bjmVvXQRchY9l9_Xkk_Hev2F2A540Fhk0tlbUBE\",\n" +
			"                    \"dq\": \"XPYDZ_kxwZjtQb-XUBLBcvNV1jcDhba2stysXGv0SfvsxOg6G3qZ5xtsiyQxzAYen0LRttCBXkZXEtXXAodLRvJMwUXuYWtNCrBqxYDHkJogUDPnBXq-Hig6x8fsDJunH8JooCc-3WcFpHQcIZZdcwyXPVi59eAfWCwJlgHYYHk\",\n" +
			"                    \"n\": \"3IXVqA9zCrmaz30WWjzZexdSuhagP_oVfgB-Y5S7XXeHhU4tho7c3wcNhsrGCKLYSf_47rV78-2OfhPY9WrSJVyqIXoaniwoTnD9splF90J7yog9LgP4kqkWl6wm8gdR5C-UC4Gnl0D2cZKnE6MZ7k_b5nM0ZfC-7H_bO13B4aYpu069th7hFWGK7ps65uiDxcQGYP3oSeqahE5qwfUef2QMkhyVtM_nWP9OVAzOGtJ8km4TdCrq9aF78No9u2YQuaUOJgeOnwKMBjmgEwcuaQe9DEQFXhzBtRmPlml2pTy2QDvxe1HgOTOsnh9igFvX70AGroY3PFY-lJAxC4Fd2w\"\n" +
			"                }\n" +
			"            ]\n" +
			"        }";
		JWKSet jwkSet = JWKSet.parse(jwks);
		assertTrue(jwkSet.getKeys().get(0).isPrivate());
		jwkSet = jwkSet.toPublicJWKSet();
		assertFalse(jwkSet.getKeys().get(0).isPrivate());
	}

}
