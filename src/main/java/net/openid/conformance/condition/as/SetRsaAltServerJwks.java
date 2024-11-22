package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Set an RSA key with alg none as the alt signing JWK.
 * The new public key is also added to the server's list of public keys in the jwks_uri
 */
public class SetRsaAltServerJwks extends AbstractCondition {

	private final String altRsaKey =
	"""
		{
			"p": "vTcrfPZ9eZjIXN4LZYyKMXG1lvH5UJukZgQQ1UQYENHPoJWIvi9JD7MPMjjEwpvZcD6YGzaczxboEiihlHPiqM6Dw9yTMbX4Bw-GgyFhgkYrOd8fPgMAFZXI4zl5G6aSTsRHolAMVXvuKGBD7QV1LnU7ZZ9hv0XZOeLDCaVzpm8",
			"kty": "RSA",
			"q": "6VHXO7lyTJcniJGAPJ_5H2UHrknODXun-bKTelxWZcEvw8t2gJDvia5mbmor7RLbdg4c4BojdflvhJJVXlxOsgxHMJiFP2cjVoNktHkEmgHCqCplJUD98ZCK7hfC5LwJLQwszMCpDbYAlVSUAsWFyWczo0hC4Kv0QCwluqm9uEc",
			"d": "Cldd48WZrS2FWFm1KpmKp73z_1HNwp2Y0UnLBhuTekuQ72UR5KsYZ0w3YtD5Kj2a8TukNvJTOhkqvT2i_y7ZN6FK5o5CBL6z5LNzuzNhIYQNqypBkVxTdfTD3ghqFCbpVHPwPgl_M1HheipjLCbsP8UuwJEWEHKPtI_0ZqrAQjdwk32LvF9kahhdaGpMeNoOcRM1BQxURrsLUjJm8fOZcz6gfjItd-8m6-VXoXDPDjCKnePi6Wh6QLbpDnuefPWrKLyo_p4EaZK_hCWWZFk0XeoEWgsVcG7piMae2mpesimiWeA1ncqkbfQNdI4bYIUt31iFDz-XBG0osSH8GoGHCQ",
			"e": "AQAB",
			"use": "sig",
			"kid": "rsa-none-alt-arg",
			"qi": "VzPxjxy_hoMSgrGMWYQLmPg0OT0gLbvyRDwFKLIPU_-pt4CKZBzzhk5NtQ9SpnPiWQ-z0D1TjHAV53CO5sJZEfevKXoTwsnBLmJwnFBq4BInXcl8PGt_5p_nx9HYxc3Lo0NU0oDSID-B3rh2fjXt3NvHJbbAHZZuaRdsw5PM1ZU",
			"dp": "H_X3tI32N9nkzjr7ddW9agipAawxzrnblRfOuBdecUjfZ2KazHU0RCCcyoDoS28D1X_dNYuOBTT7UkXmtSq1-ImZnDXf7x-rm5W1xOSYkebEWmwj3Neo5fx9CFSm7lK-l-tzpikbTD04xz0rfBfV6VkIBWxcmHB19t8kzrZRyKU",
			"alg": "none",
			"dq": "DejOHwZgNQax2adq8LJMxL1eJtrJiO49RlqKBjppACnzMgX4K5P4Y8nc22pC8iA0qyYOPKHyST80kb-zjSuNmXm36MK-9tesOKUepM-uIYxHUYUtgHoOaY9HaQhLmx1GosPeC9rUeTfHcx-Wr0-dOTOI1YwiSIiXyBeZrDYgVFM",
			"n": "rHO0Hvkwg8p9MMsTK9H2cWIVcuTXgqD9MQZnb48FM-tdmoMlPjU5WUg4N8h_4jdQ1ADgQACppuoJiXhDidN4aOiVjSso-SRjwWxKbq4TVvlNdzOCH-ligo4ftcJBLUWJJLD3I64BGG7KMoovEPFN89jR3VXD6RyKj4vaY706CaCkxZlGS9Mp8sGdT7zv5UjGKhh4-KRolRq9mId2mnleIvZBYyANxd0Rb861RY7g3O4DxWEA3xtxkSJrcBkDyY8IrKl0mxKldaR9rll4FmYoNZWzZqFoDfLzt7rYSiYOfXdt-QIlMlZthqVa3RJMM8L5MrRevoRVKjmulH1_KaXwyQ"
		}
	""";

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "server_public_jwks", "server_jwks", "server_alt_jwks" })
	public Environment evaluate(Environment env) {

		JsonElement configured = env.getElementFromObject("config", "server.jwks");

		if (configured == null) {
			throw error("Couldn't find a JWK set in configuration");
		}

		// parse the JWKS to make sure it's valid
		try {
			JWKSet jwks = JWKUtil.parseJWKSet(configured.toString());

			JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(jwks);

			// Alt RSA signing key with alg NONE.
			JWK altPrivateKey = JWK.parse(altRsaKey);

			List<JWK> newJwksList = new ArrayList<>(jwks.getKeys());
			newJwksList.add(altPrivateKey);
			JWKSet newJwkSet = new JWKSet(newJwksList);
			JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(newJwkSet);

			env.putObject("server_public_jwks", publicJwks);
			log("generated new alt RSA key configuration");

			setAltPrivateKeyJwk(env, altPrivateKey);

			logSuccess("Set alt server key",
				args("server_public_jwks", publicJwks, "server_jwks", privateJwks, "server_alt_jwks", JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(altPrivateKey))));

			return env;

		} catch (ParseException e) {
			throw error("Failure parsing JWK Set", e, args("jwk_string", configured));
		}
	}

	private void setAltPrivateKeyJwk(Environment env, JWK jwk) {
		JWKSet keySet =  new JWKSet(jwk);
		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(keySet);
		env.putObject("server_alt_jwks", privateJwks);
	}
}
