package net.openid.conformance.condition.client;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.ArrayBuilder;
import org.multipaz.cbor.Bstr;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborArray;
import org.multipaz.cbor.CborBuilder;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.DataItemExtensionsKt;
import org.multipaz.cbor.MapBuilder;
import org.multipaz.documenttype.knowntypes.DrivingLicense;
import org.multipaz.testapp.VciMdocUtils;

import java.util.Base64;
import java.util.Map;

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
		return rebuildWithItems(issuerSignedBytes, DrivingLicense.MDL_NAMESPACE, transformer);
	}

	/** As {@link #rebuildWithItems(byte[], ItemListTransformer)}, for an arbitrary namespace. */
	static byte[] rebuildWithItems(byte[] issuerSignedBytes, String namespace,
			ItemListTransformer transformer) {
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		CborMap nameSpaces = (CborMap) issuerSigned.getOrNull("nameSpaces");
		CborArray items = (CborArray) nameSpaces.getOrNull(namespace);

		MapBuilder<CborBuilder> rebuilt = CborMap.Companion.builder();
		ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> newItems = rebuilt
			.putMap("nameSpaces")
			.putArray(namespace);
		transformer.transform(items, newItems);
		newItems.end().end();
		rebuilt.put("issuerAuth", issuerSigned.getOrNull("issuerAuth"));

		return Cbor.INSTANCE.encode(rebuilt.end().build());
	}

	/**
	 * Re-encodes the IssuerSigned structure with the named element's value replaced. The MSO
	 * valueDigests are not recalculated, which does not matter for checks that do not verify them.
	 */
	static byte[] replaceElementValue(byte[] issuerSignedBytes, String namespace,
			String elementIdentifier, DataItem newValue) {
		return rebuildWithItems(issuerSignedBytes, namespace, (originalItems, newItems) -> {
			for (DataItem item : originalItems.getItems()) {
				DataItem inner = item.getAsTaggedEncodedCbor();
				String id = inner.getOrNull("elementIdentifier").getAsTstr();
				if (!id.equals(elementIdentifier)) {
					newItems.add(item);
					continue;
				}
				newItems.addTaggedEncodedCbor(Cbor.INSTANCE.encode(issuerSignedItem(
					inner.getOrNull("digestID"), inner.getOrNull("random"), elementIdentifier, newValue)));
			}
		});
	}

	/** Re-encodes the IssuerSigned structure with an extra element added to the given namespace. */
	static byte[] addElement(byte[] issuerSignedBytes, String namespace, String elementIdentifier,
			DataItem value) {
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		CborMap nameSpaces = (CborMap) issuerSigned.getOrNull("nameSpaces");

		MapBuilder<CborBuilder> rebuilt = CborMap.Companion.builder();
		MapBuilder<MapBuilder<CborBuilder>> namespacesBuilder = rebuilt.putMap("nameSpaces");
		boolean added = false;
		for (Map.Entry<DataItem, DataItem> entry : nameSpaces.getItems().entrySet()) {
			String existing = entry.getKey().getAsTstr();
			ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> items = namespacesBuilder.putArray(existing);
			for (DataItem item : ((CborArray) entry.getValue()).getItems()) {
				items.add(item);
			}
			if (existing.equals(namespace)) {
				appendItem(items, elementIdentifier, value);
				added = true;
			}
			items.end();
		}
		if (!added) {
			ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> items = namespacesBuilder.putArray(namespace);
			appendItem(items, elementIdentifier, value);
			items.end();
		}
		namespacesBuilder.end();
		rebuilt.put("issuerAuth", issuerSigned.getOrNull("issuerAuth"));
		return Cbor.INSTANCE.encode(rebuilt.end().build());
	}


	/** Re-encodes the IssuerSigned structure with an element-less namespace added. */
	static byte[] addEmptyNamespace(byte[] issuerSignedBytes, String namespace) {
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		CborMap nameSpaces = (CborMap) issuerSigned.getOrNull("nameSpaces");

		MapBuilder<CborBuilder> rebuilt = CborMap.Companion.builder();
		MapBuilder<MapBuilder<CborBuilder>> namespacesBuilder = rebuilt.putMap("nameSpaces");
		for (Map.Entry<DataItem, DataItem> entry : nameSpaces.getItems().entrySet()) {
			ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> items =
				namespacesBuilder.putArray(entry.getKey().getAsTstr());
			for (DataItem item : ((CborArray) entry.getValue()).getItems()) {
				items.add(item);
			}
			items.end();
		}
		namespacesBuilder.putArray(namespace).end();
		namespacesBuilder.end();
		rebuilt.put("issuerAuth", issuerSigned.getOrNull("issuerAuth"));
		return Cbor.INSTANCE.encode(rebuilt.end().build());
	}


	/**
	 * Re-encodes the IssuerSigned structure with one timestamp in the MSO's validityInfo replaced,
	 * or removed when newValue is null. The COSE signature is not recalculated, which does not
	 * matter for checks that do not verify it.
	 */
	static byte[] replaceMsoValidityTimestamp(byte[] issuerSignedBytes, String name, DataItem newValue) {
		DataItem issuerSigned = Cbor.INSTANCE.decode(issuerSignedBytes);
		DataItem issuerAuthItem = issuerSigned.getOrNull("issuerAuth");
		DataItem coseArray = issuerAuthItem instanceof Tagged tagged ? tagged.getTaggedItem() : issuerAuthItem;
		java.util.List<DataItem> parts = ((CborArray) coseArray).getItems();
		DataItem mso = Cbor.INSTANCE.decode(parts.get(2).getAsBstr()).getAsTaggedEncodedCbor();

		MapBuilder<CborBuilder> newValidity = CborMap.Companion.builder();
		CborMap validityInfo = (CborMap) mso.getOrNull("validityInfo");
		for (Map.Entry<DataItem, DataItem> entry : validityInfo.getItems().entrySet()) {
			String key = entry.getKey().getAsTstr();
			if (key.equals(name)) {
				if (newValue != null) {
					newValidity.put(key, newValue);
				}
			} else {
				newValidity.put(key, entry.getValue());
			}
		}

		MapBuilder<CborBuilder> newMso = CborMap.Companion.builder();
		for (Map.Entry<DataItem, DataItem> entry : ((CborMap) mso).getItems().entrySet()) {
			String key = entry.getKey().getAsTstr();
			newMso.put(key, key.equals("validityInfo") ? newValidity.end().build() : entry.getValue());
		}
		byte[] newPayload = Cbor.INSTANCE.encode(
			new Tagged(Tagged.ENCODED_CBOR, new Bstr(Cbor.INSTANCE.encode(newMso.end().build()))));

		ArrayBuilder<CborBuilder> newCose = CborArray.Companion.builder();
		newCose.add(parts.get(0));
		newCose.add(parts.get(1));
		newCose.add(new Bstr(newPayload));
		newCose.add(parts.get(3));
		DataItem newIssuerAuth = issuerAuthItem instanceof Tagged tagged
			? new Tagged(tagged.getTagNumber(), newCose.end().build())
			: newCose.end().build();

		MapBuilder<CborBuilder> rebuilt = CborMap.Companion.builder();
		rebuilt.put("nameSpaces", issuerSigned.getOrNull("nameSpaces"));
		rebuilt.put("issuerAuth", newIssuerAuth);
		return Cbor.INSTANCE.encode(rebuilt.end().build());
	}

	private static void appendItem(ArrayBuilder<MapBuilder<MapBuilder<CborBuilder>>> items,
			String elementIdentifier, DataItem value) {
		items.addTaggedEncodedCbor(Cbor.INSTANCE.encode(issuerSignedItem(
			DataItemExtensionsKt.toDataItem(9999),
			DataItemExtensionsKt.toDataItem(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }),
			elementIdentifier, value)));
	}

	private static DataItem issuerSignedItem(DataItem digestId, DataItem random,
			String elementIdentifier, DataItem value) {
		MapBuilder<CborBuilder> item = CborMap.Companion.builder();
		item.put("digestID", digestId);
		item.put("random", random);
		item.put("elementIdentifier", DataItemExtensionsKt.toDataItem(elementIdentifier));
		item.put("elementValue", value);
		return item.end().build();
	}

	/** Re-encodes the IssuerSigned structure with the named element removed from the mDL namespace. */
	static byte[] removeElement(byte[] issuerSignedBytes, String elementIdentifier) {
		return removeElement(issuerSignedBytes, DrivingLicense.MDL_NAMESPACE, elementIdentifier);
	}

	/** Re-encodes the IssuerSigned structure with the named element removed from the given namespace. */
	static byte[] removeElement(byte[] issuerSignedBytes, String namespace, String elementIdentifier) {
		return rebuildWithItems(issuerSignedBytes, namespace, (originalItems, newItems) -> {
			for (DataItem item : originalItems.getItems()) {
				String id = item.getAsTaggedEncodedCbor().getOrNull("elementIdentifier").getAsTstr();
				if (!id.equals(elementIdentifier)) {
					newItems.add(item);
				}
			}
		});
	}
}
