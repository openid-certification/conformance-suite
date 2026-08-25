package net.openid.conformance.condition.client;

import net.openid.conformance.util.PhotoIdDataElements;

import java.util.Set;

/**
 * Checks that an issued photo ID contains the data elements ISO/IEC TS 23220-4 Annex C Table 1
 * marks with presence "R" (recommended). Callers should raise this as a WARNING — the elements
 * are recommended, not required.
 *
 * See {@link EnsureMdocPhotoIdMandatoryDataElementsPresent} for the naming and normativity notes.
 */
public class EnsureMdocPhotoIdRecommendedDataElementsPresent
		extends AbstractEnsureMdocDataElementsPresent {

	@Override
	protected String getDocType() {
		return PhotoIdDataElements.PHOTO_ID_DOCTYPE;
	}

	@Override
	protected String getNamespace() {
		return PhotoIdDataElements.ISO_23220_2_NAMESPACE;
	}

	@Override
	protected Set<String> getRequiredElements() {
		return PhotoIdDataElements.RECOMMENDED_ELEMENTS;
	}

	@Override
	protected String getRequirementDescription() {
		return "recommended";
	}

	@Override
	protected String getCredentialName() {
		return "photo ID";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC TS 23220-4 Annex C";
	}
}
