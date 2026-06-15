package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.ArrayBuilder;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.MapBuilder;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;

/**
 * Shared scaffolding for unit tests that exercise conditions on mdoc IssuerSigned structures
 * as stored in 'mdoc_credential_cbor' by ParseMdocCredentialFromVCIIssuance.
 */
final class MdocCredentialTestUtil {

	private MdocCredentialTestUtil() {
		// utility class
	}

	/** Creates an IssuerSigned test credential for the given docType, signed with a fresh key. */
	static byte[] createCredentialBytes(String docType) throws Exception {
		String mdocBase64Url = VciMdocUtils.createMdocCredential(
			new ECKeyGenerator(Curve.P_256).generate().toJSONString(), docType, null);
		return new Base64URL(mdocBase64Url).decode();
	}

	/** Stores the IssuerSigned bytes in the environment the way ParseMdocCredentialFromVCIIssuance does. */
	static void putCredential(Environment env, byte[] issuerSignedBytes) {
		env.putString("mdoc_credential_cbor", Base64.getEncoder().encodeToString(issuerSignedBytes));
	}

	interface ItemListTransformer {
		void transform(CborArray originalItems, ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> newItems);
	}

	/**
	 * Re-encodes the IssuerSigned structure transforming the mDL namespace's item list, without
	 * re-signing — the original issuerAuth (and so the MSO valueDigests) is kept as-is.
	 */
	static byte[] rebuildWithItems(byte[] issuerSignedBytes, ItemListTransformer transformer) {
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		CborMap nameSpaces = (CborMap) issuerSigned.getOrNull("nameSpaces");
		CborArray mdlItems = (CborArray) nameSpaces.getOrNull(DrivingLicense.MDL_NAMESPACE);

		MapBuilder<CborBuilder> rebuilt = CborMap.Companion.builder();
		ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> newItems = rebuilt
			.putMap("nameSpaces")
			.putArray(DrivingLicense.MDL_NAMESPACE);
		transformer.transform(mdlItems, newItems);
		newItems.end().end();
		rebuilt.put("issuerAuth", issuerSigned.getOrNull("issuerAuth"));

		return Cbor.INSTANCE.encode(rebuilt.end().build());
	}

	/** Re-encodes the IssuerSigned structure with the named element removed from the mDL namespace. */
	static byte[] removeElement(byte[] issuerSignedBytes, String elementIdentifier) {
		return rebuildWithItems(issuerSignedBytes, (originalItems, newItems) -> {
			for (DataItem item : originalItems.getItems()) {
				String id = item.getAsTaggedEncodedCbor().getOrNull("elementIdentifier").getAsTstr();
				if (!id.equals(elementIdentifier)) {
					newItems.add(item);
				}
			}
		});
	}
}
