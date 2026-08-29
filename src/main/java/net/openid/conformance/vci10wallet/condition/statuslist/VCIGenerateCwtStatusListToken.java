package net.openid.conformance.vci10wallet.condition.statuslist;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import kotlinx.io.bytestring.ByteString;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.cose.Cose;
import org.multipaz.cose.CoseLabel;
import org.multipaz.cose.CoseNumberLabel;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;
import org.multipaz.crypto.EcCurve;
import org.multipaz.crypto.EcPrivateKey;
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Generates the emulated credential issuer's Token Status List as a Status List Token in CWT
 * format, the representation ISO/IEC 18013-5 12.3.6.3 requires for an MSO revocation list.
 *
 * <p>The list contents are the same as {@link VCIGenerateJwtStatusListToken} produces; only the
 * representation differs: a COSE_Sign1 (tagged 18) with the signature algorithm, the
 * {@code application/statuslist+cwt} type and the x5chain in the protected header, over a CWT
 * claims set using the claim keys of draft-ietf-oauth-status-list section 5.2.
 *
 * <p>Stores the token base64 encoded in {@code current_status_list_cwt}.
 */
public class VCIGenerateCwtStatusListToken extends AbstractCondition {

	// CWT claim keys, draft-ietf-oauth-status-list section 5.2
	private static final long CWT_CLAIM_SUB = 2;
	private static final long CWT_CLAIM_EXP = 4;
	private static final long CWT_CLAIM_IAT = 6;
	private static final long CWT_CLAIM_STATUS_LIST = 65533;
	private static final long CWT_CLAIM_TTL = 65534;

	public static final String STATUS_LIST_CWT_CONTENT_TYPE = "application/statuslist+cwt";

	@Override
	@PreEnvironment(required = { "server_jwks" }, strings = { "current_status_list_id" })
	@PostEnvironment(strings = { "current_status_list_cwt" })
	public Environment evaluate(Environment env) {

		String currentStatusListId = env.getString("current_status_list_id");
		String issuerUrl = env.getString("server", "issuer");
		String currentStatusListUri = issuerUrl + "statuslists/" + currentStatusListId;

		TokenStatusList statusList = VCIStatusListContents.create();
		byte[] compressedStatusList = Base64.getUrlDecoder().decode(statusList.encodeStatusList());

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(10 * 60);

		byte[] payload = buildClaimsSet(currentStatusListUri, iat, exp, compressedStatusList);

		ECKey signingKey = selectSigningKey(env);
		Algorithm algorithm = algorithmFor(signingKey);
		X509CertChain certChain = certChainOf(signingKey);

		Map<CoseLabel, DataItem> protectedHeaders = new LinkedHashMap<>();
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_ALG),
			DataItemExtensionsKt.toDataItem(algorithm.getCoseAlgorithmIdentifier().intValue()));
		protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_TYP),
			new Tstr(STATUS_LIST_CWT_CONTENT_TYPE));
		if (certChain != null) {
			// ISO/IEC 18013-5 12.3.6.3: the x5chain goes in the protected header
			protectedHeaders.put(new CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN), certChain.toDataItem());
		} else {
			log("The server JWK used to sign the status list token has no x5c certificate chain,"
				+ " so the generated CWT cannot carry the x5chain that ISO/IEC 18013-5 12.3.6.3 requires");
		}

		AsymmetricKey key = signingKeyFor(signingKey, algorithm, certChain);

		CoseSign1 coseSign1;
		try {
			coseSign1 = (CoseSign1) kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				(scope, continuation) -> Cose.INSTANCE.coseSign1Sign(key, payload, true,
					protectedHeaders, Map.of(), continuation));
		} catch (Exception e) {
			throw error("Failed to sign the status list token in CWT format", e);
		}

		// draft-ietf-oauth-status-list section 5.2: the COSE message is the tagged COSE_Sign1
		byte[] token = Cbor.INSTANCE.encode(new Tagged(Tagged.COSE_SIGN1, coseSign1.toDataItem()));

		env.putString("current_status_list_cwt", Base64.getEncoder().encodeToString(token));

		logSuccess("Generated the Status List Token in CWT format",
			args("sub", currentStatusListUri,
				"algorithm", algorithm.name(),
				"exp", exp.getEpochSecond(),
				"length", token.length));
		return env;
	}

	private byte[] buildClaimsSet(String uri, Instant iat, Instant exp, byte[] compressedStatusList) {
		Map<DataItem, DataItem> statusListClaim = new LinkedHashMap<>();
		statusListClaim.put(new Tstr("bits"), DataItemExtensionsKt.toDataItem(VCIStatusListContents.BITS));
		statusListClaim.put(new Tstr("lst"), new Bstr(compressedStatusList));

		Map<DataItem, DataItem> claims = new LinkedHashMap<>();
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_SUB), new Tstr(uri));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_EXP),
			DataItemExtensionsKt.toDataItem(exp.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_IAT),
			DataItemExtensionsKt.toDataItem(iat.getEpochSecond()));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_STATUS_LIST),
			new CborMap(statusListClaim, false));
		claims.put(DataItemExtensionsKt.toDataItem(CWT_CLAIM_TTL),
			DataItemExtensionsKt.toDataItem(TimeUnit.MINUTES.toSeconds(12)));

		return Cbor.INSTANCE.encode(new CborMap(claims, false));
	}

	/**
	 * The first EC key with a private part in the server JWKS. ISO/IEC 18013-5 12.3.6.3 only
	 * permits the EC based signature algorithms for an MSO revocation list.
	 */
	private ECKey selectSigningKey(Environment env) {
		JsonObject jwks = env.getObject("server_jwks");
		JWKSet jwkSet;
		try {
			jwkSet = JWKSet.parse(jwks.toString());
		} catch (Exception e) {
			throw error("Failed to parse the server JWKS", e);
		}
		for (JWK jwk : jwkSet.getKeys()) {
			if (KeyType.EC.equals(jwk.getKeyType()) && jwk.isPrivate()) {
				return jwk.toECKey();
			}
		}
		throw error("The server JWKS does not contain an EC private key; ISO/IEC 18013-5 12.3.6.3"
			+ " only permits ES256, ES384, ES512 or EdDSA for an MSO revocation list");
	}

	private Algorithm algorithmFor(ECKey key) {
		Curve curve = key.getCurve();
		if (Curve.P_256.equals(curve)) {
			return Algorithm.ES256;
		}
		if (Curve.P_384.equals(curve)) {
			return Algorithm.ES384;
		}
		if (Curve.P_521.equals(curve)) {
			return Algorithm.ES512;
		}
		throw error("The server JWKS EC signing key uses curve " + curve
			+ ", which is not one of the curves ISO/IEC 18013-5 12.3.6.3 permits for an MSO revocation list");
	}

	private EcCurve curveFor(ECKey key) {
		Curve curve = key.getCurve();
		if (Curve.P_256.equals(curve)) {
			return EcCurve.P256;
		}
		if (Curve.P_384.equals(curve)) {
			return EcCurve.P384;
		}
		return EcCurve.P521;
	}

	private X509CertChain certChainOf(ECKey key) {
		List<com.nimbusds.jose.util.Base64> x5c = key.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			return null;
		}
		List<X509Cert> certs = new ArrayList<>();
		for (com.nimbusds.jose.util.Base64 cert : x5c) {
			byte[] der = cert.decode();
			certs.add(new X509Cert(new ByteString(der, 0, der.length)));
		}
		return new X509CertChain(certs);
	}

	private AsymmetricKey signingKeyFor(ECKey key, Algorithm algorithm, X509CertChain certChain) {
		EcPrivateKey privateKey = new EcPrivateKeyDoubleCoordinate(curveFor(key),
			key.getD().decode(), key.getX().decode(), key.getY().decode());
		if (certChain == null) {
			return new AsymmetricKey.AnonymousExplicit(privateKey, algorithm);
		}
		return new AsymmetricKey.X509CertifiedExplicit(certChain, privateKey, algorithm);
	}
}
