package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdlDataElements;

import java.util.Set;

/**
 * Checks that an issued mdoc credential with docType org.iso.18013.5.1.mDL contains all the
 * data elements that ISO/IEC 18013-5 13.4.2 Table 20 marks as mandatory in the
 * org.iso.18013.5.1 namespace. The check passes without doing anything for other docTypes.
 */
public class EnsureMdocMdlMandatoryDataElementsPresent
		extends AbstractEnsureMdocDataElementsPresent {

	@Override
	protected String getDocType() {
		return MdlDataElements.MDL_DOCTYPE;
	}

	@Override
	protected String getNamespace() {
		return MdlDataElements.MDL_NAMESPACE;
	}

	@Override
	protected Set<String> getRequiredElements() {
		return MdlDataElements.MANDATORY_ELEMENTS;
	}

	@Override
	protected String getRequirementDescription() {
		return "mandatory";
	}

	@Override
	protected String getCredentialName() {
		return "mDL";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC 18013-5";
	}
}
