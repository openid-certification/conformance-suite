package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.EcPublicKey;
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.util.Base64;

/**
 * Checks that the device key in the MSO of the issued mdoc credential (raw CBOR stored as
 * standard base64 in 'mdoc_credential_cbor' by ParseMdocCredentialFromVCIIssuance) is one
 * of the keys the proofs in the credential request demonstrated possession of.
 */
public class VCIEnsureMdocDeviceKeyMatchesProofKey extends AbstractVCIEnsureBindingKeyMatchesProofKey {

	@Override
	@PreEnvironment(strings = "mdoc_credential_cbor", required = "credential_request_proofs")
	public Environment evaluate(Environment env) {

		EcPublicKey deviceKey;
		try {
			byte[] bytes = Base64.getDecoder().decode(env.getString("mdoc_credential_cbor"));
			DataItem issuerSigned = Cbor.INSTANCE.decode(bytes);
			DataItem issuerAuth = issuerSigned.getOrNull("issuerAuth");
			if (issuerAuth == null) {
				throw error("mdoc credential is missing the required 'issuerAuth' field");
			}
			CoseSign1 coseSign1 = issuerAuth.getAsCoseSign1();
			DataItem msoDataItem = Cbor.INSTANCE.decode(coseSign1.getPayload()).getAsTaggedEncodedCbor();
			MobileSecurityObject mso = MobileSecurityObject.Companion.fromDataItem(msoDataItem);
			deviceKey = mso.getDeviceKey();
		} catch (ConditionError e) {
			throw e;
		} catch (Exception e) {
			throw error("Failed to extract the device key from the mdoc credential", e);
		}

		if (!(deviceKey instanceof EcPublicKeyDoubleCoordinate ecKey)) {
			throw error("The mdoc credential's device key is not an EC key with x/y coordinates",
				args("curve", deviceKey.getCurve().name()));
		}

		JsonObject jwk = new JsonObject();
		jwk.addProperty("kty", "EC");
		jwk.addProperty("crv", ecKey.getCurve().getJwkName());
		jwk.addProperty("x", Base64URL.encode(ecKey.getX()).toString());
		jwk.addProperty("y", Base64URL.encode(ecKey.getY()).toString());

		checkBindingKeyWasSent(env, jwk, "MSO device key");

		return env;
	}
}
