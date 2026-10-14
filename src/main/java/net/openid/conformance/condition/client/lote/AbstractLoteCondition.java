package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Shared access to the list of trusted entities (ETSI TS 119 602 LoTE) under test, held in the
 * environment object mapped to {@code "lote"} (see {@link LoteSlot}). The object's members are
 * written progressively by the conditions in {@code SetupLoteFromConfiguration}: {@code slot},
 * {@code url}, {@code value} (the compact JWS as fetched), {@code header} and {@code payload}
 * (the parsed JWS parts), {@code anchors}.
 */
public abstract class AbstractLoteCondition extends AbstractValidateX5cCertificateChain {

	/** The environment key the conditions work on; the sequence maps a slot's key over it. */
	public static final String LOTE = "lote";

	protected JsonObject getLote(Environment env) {
		JsonObject lote = env.getObject(LOTE);
		if (lote == null) {
			throw error("No list of trusted entities has been registered");
		}
		return lote;
	}

	protected LoteSlot getSlot(Environment env) {
		String slot = env.getString(LOTE, "slot");
		if (slot == null) {
			throw error("The registered list of trusted entities does not say which slot it belongs to");
		}
		return LoteSlot.valueOf(slot);
	}

	/** The compact JWS the list arrived as. */
	protected String getLoteValue(Environment env) {
		String value = env.getString(LOTE, "value");
		if (value == null || value.isBlank()) {
			throw error("The list of trusted entities has no value (it was not fetched)");
		}
		return value;
	}

	protected JWSObject parseJws(Environment env) {
		try {
			return JWSObject.parse(getLoteValue(env));
		} catch (ParseException e) {
			throw error("The list of trusted entities is not a compact JWS", e);
		}
	}

	/** The parsed {@code LoTE} payload object, as stored by {@code ParseLote}. */
	protected JsonObject getLotePayload(Environment env) {
		JsonElement payload = env.getElementFromObject(LOTE, "payload");
		if (payload == null || !payload.isJsonObject()) {
			throw error("The list of trusted entities has not been parsed");
		}
		return payload.getAsJsonObject();
	}

	/** The {@code ListAndSchemeInformation} object of the parsed payload, or null when absent. */
	protected JsonObject getListAndSchemeInformation(Environment env) {
		JsonElement lote = getLotePayload(env).get("LoTE");
		if (lote == null || !lote.isJsonObject()) {
			return null;
		}
		JsonElement info = lote.getAsJsonObject().get("ListAndSchemeInformation");
		return info != null && info.isJsonObject() ? info.getAsJsonObject() : null;
	}

	/**
	 * Whether the list's JWS verifies under {@code signer}'s key.
	 *
	 * @throws JOSEException when it cannot be verified at all, for an algorithm or key the suite does not support
	 */
	protected static boolean verifies(JWSObject jws, X509Certificate signer) throws JOSEException {
		return jws.verify(new DefaultJWSVerifierFactory().createJWSVerifier(jws.getHeader(), signer.getPublicKey()));
	}

	/** The certificate chain in the JWS header's x5c, leaf first. */
	protected List<X509Certificate> getSignerCertificateChain(JWSObject jws) {
		if (jws.getHeader().getX509CertChain() == null || jws.getHeader().getX509CertChain().isEmpty()) {
			throw error("The JWS protected header of the list of trusted entities has no x5c: the signer's certificate must be in it",
				args("header", jws.getHeader().toString()));
		}
		return parseX5cCertificatesFromNimbusBase64(jws.getHeader().getX509CertChain());
	}

	/** The {@code value} of the first element of a multilingual string array, or null. */
	protected static String firstValue(JsonElement multilingual) {
		if (multilingual == null || !multilingual.isJsonArray() || multilingual.getAsJsonArray().isEmpty()) {
			return null;
		}
		JsonElement first = multilingual.getAsJsonArray().get(0);
		if (!first.isJsonObject()) {
			return null;
		}
		JsonElement value = first.getAsJsonObject().get("value");
		return value != null && value.isJsonPrimitive() ? OIDFJSON.getString(value) : null;
	}
}
