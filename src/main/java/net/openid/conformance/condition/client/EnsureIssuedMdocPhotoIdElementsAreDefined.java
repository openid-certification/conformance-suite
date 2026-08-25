package net.openid.conformance.condition.client;

import net.openid.conformance.util.PhotoIdDataElements;

/**
 * Checks an mdoc photo ID as issued over VCI contains no data element ISO/IEC TS 23220-4 Annex C
 * does not define.
 * */
public class EnsureIssuedMdocPhotoIdElementsAreDefined extends AbstractEnsureIssuedMdocElementsAreDefined {

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
	protected boolean isDefined(String namespace, String elementIdentifier) {
		return PhotoIdDataElements.isDefined(namespace, elementIdentifier);
	}
}
