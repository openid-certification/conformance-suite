package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.common.AbstractInvalidateJwsSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.client.CreateDpopProofSteps;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;

public class Fapi2DPoPNegativeConditions {

	//=====================================
	// Conditions related to DPoP header
	//=====================================

	abstract static class ChangeDpopHeader extends AbstractCondition {

		@Override
		@PreEnvironment(required = "dpop_proof_header")
		public Environment evaluate(Environment env) {
			JsonObject header = env.getObject("dpop_proof_header");
			changeClaim(header, env);
			logSuccess("DPoP proof header", header);
			return env;
		}

		public abstract void changeClaim(JsonObject header, Environment env);
	}

	public static class RemoveTypFromDpopProof extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			header.remove("typ");
		}
	}

	public static class RemoveJwkFromDpopProof extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			header.remove("jwk");
		}
	}

	public static class SetDpopHeaderJwkToPrivateKey extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			JsonObject jwk = env.getElementFromObject("client", "dpop_private_jwk").getAsJsonObject();
			header.add("jwk", jwk);
		}
	}

	public static class SetDpopHeaderTypToInvalidValue extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			header.addProperty("typ", "dpop+jwt+wrongyousee");
		}
	}

	public static class AddExtraClaimsToHeader extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			Instant exp = Instant.now().plusSeconds(60 * 60);
			header.addProperty("tx_id", exp.getEpochSecond());
		}
	}


	//=====================================
	// Conditions related to DPoP claims
	//=====================================
	abstract static class ChangeDpopClaims extends AbstractCondition {

		@Override
		@PreEnvironment(required = "dpop_proof_claims")
		public Environment evaluate(Environment env) {
			JsonObject claims = env.getObject("dpop_proof_claims");
			changeClaim(claims, env);
			logSuccess("DPoP proof claims", claims);
			return env;
		}

		public abstract void changeClaim(JsonObject claims, Environment env);
	}

	public static class RemoveJtiFromDpopProof extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			claims.remove("jti");
		}
	}

	public static class RemoveHtmFromDpopProof extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			claims.remove("htm");
		}
	}

	public static class RemoveHtuFromDpopProof extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			claims.remove("htu");
		}
	}

	public static class RemoveIatFromDpopProof extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			claims.remove("iat");
		}
	}

	public static class DpopHtuUpperCase extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			String resourceEndpoint = env.getString("protected_resource_url");
			String resourceMethod = "GET";
			String configuredMethod = env.getString("resource", "resourceMethod");
			if (!Strings.isNullOrEmpty(configuredMethod)) {
				resourceMethod = configuredMethod;
			}
			claims.addProperty("htm", resourceMethod);
			URI resourceURI = URI.create(resourceEndpoint);
			String hostUpper = resourceURI.getHost().toUpperCase();
			String schemeUpper = resourceURI.getScheme().toUpperCase();
			String changedResourceEndoint = resourceEndpoint.replace(resourceURI.getHost(),hostUpper);
			changedResourceEndoint = changedResourceEndoint.replace(resourceURI.getScheme(),schemeUpper);

			claims.addProperty("htu", changedResourceEndoint);
			logSuccess("Added htm/htu to DPoP proof claims", claims);
		}
	}

	public static class AddExtraClaimsToClaims extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			Instant exp = Instant.now().plusSeconds(60 * 60);
			claims.addProperty("tx_id_key", exp.getEpochSecond());
		}
	}

	public static class DpopHtuWithPort extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			String resourceEndpoint = env.getString("protected_resource_url");

			URI uri = null;
			try {
				uri = new URI(resourceEndpoint);
			} catch (URISyntaxException e) {
				error(e);
				return;
			}
			if (uri.getPort() == -1) {
				resourceEndpoint = uri.getScheme() + "://" + uri.getHost()
					+ (uri.getScheme().equals("https") ? ":443" : ":80")
					+ uri.getPath();
			}
			String resourceMethod = "GET";
			String configuredMethod = env.getString("resource", "resourceMethod");
			if (!Strings.isNullOrEmpty(configuredMethod)) {
				resourceMethod = configuredMethod;
			}
			claims.addProperty("htm", resourceMethod);
			claims.addProperty("htu", resourceEndpoint);
		}
	}

	public static class SetDpopHtmToPut extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			claims.addProperty("htm", "PUT");
		}
	}

	public static class FixedJtiClaim extends ChangeDpopClaims {
		@Override
		public void changeClaim(JsonObject claims, Environment env) {
			String existingJTI = env.getString("jti");
			if(existingJTI == null || existingJTI.equals("")){
				existingJTI = RandomStringUtils.secure().nextAlphanumeric(15);
				env.putString("jti", existingJTI);
			}
			claims.addProperty("jti", existingJTI);
		}
	}

	//=====================================
	// Conditions specific to DPoP
	//=====================================

	public static class AddDpopHeaderAllCapital extends AbstractCondition {

		@Override
		@PreEnvironment(required = "resource_endpoint_request_headers", strings = "dpop_proof")
		public Environment evaluate(Environment env) {

			String dpopProof = env.getString("dpop_proof");

			JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

			requestHeaders.addProperty("DPOP", dpopProof);

			logSuccess("Set DPoP header", args("DPOP", dpopProof));

			return env;
		}

	}

	public static class RemoveDpopFromResourceRequest extends AbstractCondition {
		@Override
		@PreEnvironment(required = "resource_endpoint_request_headers")
		public Environment evaluate(Environment env) {

			JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

			requestHeaders.remove("DPoP");

			logSuccess("Removed DPoP from resource header", requestHeaders);

			return env;
		}
	}


	//=====================================
	// Conditions specific to DPoP signing
	//=====================================

	public static class SignDpopProofWithNone extends AbstractCondition {
		private static final String ALG_NONE_HEADER = "{\"alg\": \"none\", \"typ\": \"dpop+jwt\"}";

		@Override
		@PreEnvironment(required = {"dpop_proof_claims"})
		@PostEnvironment(strings = "dpop_proof")
		public Environment evaluate(Environment env) {
			JsonObject claims = env.getObject("dpop_proof_claims");
			String jwt = Base64URL.encode(ALG_NONE_HEADER) + "." + Base64URL.encode(claims.toString()) + ".";
			env.putString("dpop_proof", jwt);
			logSuccess("Signed the DPoP proof with none alg", args("dpop_proof", jwt));
			return env;
		}
	}

	public static class ChangeSignAlgorithm extends ChangeDpopHeader {
		@Override
		public void changeClaim(JsonObject header, Environment env) {
			header.remove("alg");
			header.addProperty("alg", "RS256");
		}
	}

	public static class InvalidateDpopProofSignature extends AbstractInvalidateJwsSignature {

		@Override
		@PreEnvironment(strings = "dpop_proof")
		@PostEnvironment(strings = "dpop_proof")
		public Environment evaluate(Environment env) {
			return invalidateSignature(env, "dpop_proof");
		}

	}

	public static class NotWellformedDPoP extends AbstractCondition {

		@Override
		@PreEnvironment(strings = "dpop_proof")
		@PostEnvironment(strings = "dpop_proof")
		public Environment evaluate(Environment env) {

			String jws = env.getString("dpop_proof");
			String[] parts = jws.split("\\.");
			jws = String.join(".", parts[0], parts[1]);
			env.putString("dpop_proof", jws);
			logSuccess("Changed the DPoP proof", args("dpop_proof", jws));
			return env;
		}
	}

	public static class SignDpopAndRemoveAlg extends AbstractCondition {

		@Override
		@PreEnvironment(strings = "dpop_proof")
		@PostEnvironment(strings = "dpop_proof")
		public Environment evaluate(Environment env) {


			String jws = env.getString("dpop_proof");

			String[] parts = jws.split("\\.");
			parts[0] = Base64URL.encode(Base64URL.from(parts[0]).decodeToString().replace("alg", "alg2")).toString();

			jws = String.join(".", parts);

			env.putString("dpop_proof", jws);

			logSuccess("Changed the DPoP proof", args("dpop_proof", jws));

			return env;

		}
	}


	public static class GenerateNewSignKey extends AbstractCondition {

		@Override
		@PreEnvironment(required = {"client"})
		public Environment evaluate(Environment env) {
			JsonObject jwk = (JsonObject) env.getElementFromObject("client", "dpop_private_jwk");
			JWK signingJwk = null;
			try {
				signingJwk = JWK.parse(jwk.toString());
			} catch (ParseException e) {
				throw error(e);
			}
			String dpopSigningAlg = signingJwk.getAlgorithm().getName();
			JWKGenerator<? extends JWK> generator;
			switch (dpopSigningAlg) {
				case "ES256":
					generator = new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256);
					break;
				case "EdDSA":
					generator = new OctetKeyPairGenerator(Curve.Ed25519).algorithm(JWSAlgorithm.EdDSA);
					break;
				case "PS256":
					generator = new RSAKeyGenerator(AbstractGenerateClientJWKs.DEFAULT_KEY_SIZE).algorithm(JWSAlgorithm.PS256);
					break;
				default:
					throw error("Failed to generate key for alg", args("alg", dpopSigningAlg));
			}

			JWK key;
			try {
				key = generator.keyUse(KeyUse.SIGNATURE).generate();
			} catch (JOSEException e) {
				throw error("Failed to generate DPoP key", e);
			}
			JsonObject keyJson = JsonParser.parseString(key.toJSONString()).getAsJsonObject();
			env.putObject("client", "dpop_private_jwk", keyJson);
			env.putObject("client", "dpop_private_jwk_old", jwk);

			return env;
		}
	}
	public static class RecoverSignKey extends AbstractCondition {

		@Override
		@PreEnvironment(required = {"client"})
		public Environment evaluate(Environment env) {
			JsonObject jwk = (JsonObject) env.getElementFromObject("client", "dpop_private_jwk_old");

			env.putObject("client", "dpop_private_jwk", jwk);
			return env;
		}
	}

	public static class RenameDPoPProof extends AbstractCondition {

		@Override
		@PreEnvironment(required = "resource_endpoint_request_headers", strings = "dpop_proof")
		@PostEnvironment(strings = "dpop_proof2")
		public Environment evaluate(Environment env) {

			String dpopProof = env.getString("dpop_proof");
			env.putString("dpop_proof2",dpopProof);
			return env;
		}

	}
	public static class AddMultipleDpopHeaderForResourceEndpointRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "resource_endpoint_request_headers", strings = {"dpop_proof", "dpop_proof2"})
		public Environment evaluate(Environment env) {

			String dpopProof = env.getString("dpop_proof");
			String dpopProof2 = env.getString("dpop_proof2");

			JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

			requestHeaders.remove("DPOP");

			JsonArray element = new JsonArray(2);
			element.add(dpopProof);
			element.add(dpopProof2);
			requestHeaders.add("DPoP", element);

			logSuccess("Set DPoP header", args("DPoP", element));

			return env;
		}

	}

	public static class MultipleProofs extends AbstractConditionSequence {

		@Override
		public void evaluate() {
			callAndStopOnFailure(RenameDPoPProof.class);
			call(CreateDpopProofSteps.createResourceEndpointDpopSteps().replace(AddDpopHeaderForResourceEndpointRequest.class, condition(AddMultipleDpopHeaderForResourceEndpointRequest.class)));
		}
	}


	public static class RemoveQueryAndFragmentFromDpopHtu extends AbstractCondition {

		@Override
		@PreEnvironment(required = "dpop_proof_claims")
		public Environment evaluate(Environment env) {

			JsonObject claims = env.getObject("dpop_proof_claims");
			String htu = OIDFJSON.getString(claims.get("htu"));
			int lastIndexOf = htu.lastIndexOf("?");
			if(lastIndexOf> 0){
				htu = htu.substring(0,lastIndexOf);
			}
			claims.addProperty("htu", htu);
			logSuccess("Remove query/fragment (which must be ignored as per 4.3-9 in DPoP spec) to htu in DPoP proof claims", claims);

			return env;

		}
	}

	public static class AddQueryAndFragmentToDpopHtu extends AbstractCondition {

		@Override
		@PreEnvironment(required = "dpop_proof_claims")
		public Environment evaluate(Environment env) {

			JsonObject claims = env.getObject("dpop_proof_claims");

			String htu = OIDFJSON.getString(claims.get("htu"));
			int lastIndexOf = htu.lastIndexOf("?");
			if(lastIndexOf == -1) {
				htu = htu + "?allthedoorsonthisspaceshiphavebeen#programmedtohaveacheeryandsunnydisposition";
			}
			claims.addProperty("htu", htu);
			logSuccess("Added query/fragment (which must be ignored as per 4.3-9 in DPoP spec) to htu in DPoP proof claims", claims);
			return env;
		}
	}

	public static class SetDpopHtuToDifferentUrl extends AbstractCondition {

		@Override
		@PreEnvironment(required = "dpop_proof_claims")
		public Environment evaluate(Environment env) {

			JsonObject claims = env.getObject("dpop_proof_claims");

			String htu = OIDFJSON.getString(claims.get("htu"));

			int lastIndexOf = htu.lastIndexOf("?");
			if(lastIndexOf> 0){
				htu = htu.substring(0,lastIndexOf) + "ohnonotagain";
			} else {
				htu = htu + "ohnonotagain";
			}


			claims.addProperty("htu", htu);

			logSuccess("Made htu in DPoP proof claims a different url", claims);

			return env;

		}
	}
}
