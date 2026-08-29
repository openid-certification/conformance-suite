package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdlDataElements;
import net.openid.conformance.util.MdocValueConstraint;

/**
 * Checks that the data element values of an mdoc mDL match the encoding and value constraints
 * ISO/IEC 18013-5 Table 20 defines. Applies to both an issued and a presented credential.
 */
public class EnsureMdocMdlElementValuesAreValid extends AbstractEnsureMdocElementValuesAreValid {

	@Override
	protected String getExpectedDocType() {
		return MdlDataElements.MDL_DOCTYPE;
	}

	@Override
	protected String getCredentialName() {
		return "mDL";
	}

	@Override
	protected String getSpecificationName() {
		return "ISO/IEC 18013-5 Table 20";
	}

	@Override
	protected boolean isKnownNamespace(String namespace) {
		return MdlDataElements.isKnownNamespace(namespace);
	}

	@Override
	protected MdocValueConstraint getValueConstraint(String namespace, String elementIdentifier) {
		return MdlDataElements.getValueConstraint(elementIdentifier);
	}
}
