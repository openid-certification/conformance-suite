package net.openid.conformance.condition.client;

import net.openid.conformance.util.PhotoIdDataElements;
import net.openid.conformance.util.MdocValueConstraint;

/**
 * Checks that the data element values of an mdoc photo ID match the encoding and value constraints
 * ISO/IEC TS 23220-4 Annex C defines. Applies to both an issued and a presented credential.
 */
public class EnsureMdocPhotoIdElementValuesAreValid extends AbstractEnsureMdocElementValuesAreValid {

	@Override
	protected String getExpectedDocType() {
		return PhotoIdDataElements.PHOTO_ID_DOCTYPE;
	}

	@Override
	protected String getCredentialName() {
		return "photo ID";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC TS 23220-4 Annex C";
	}

	@Override
	protected boolean isKnownNamespace(String namespace) {
		return PhotoIdDataElements.isKnownNamespace(namespace);
	}

	@Override
	protected MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier) {
		return PhotoIdDataElements.getValueConstraint(namespace, elementIdentifier);
	}
}
