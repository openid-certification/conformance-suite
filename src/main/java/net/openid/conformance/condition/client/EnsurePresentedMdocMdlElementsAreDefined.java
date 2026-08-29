package net.openid.conformance.condition.client;

import net.openid.conformance.util.MdlDataElements;

/**
 * Checks an mDL as presented to a verifier contains no data element ISO/IEC 18013-5 Table 20 does
 * not define.
 *
 * Unlike the presence checks this one does apply to presentations: selective disclosure
 * explains why an element is absent, but never why an undefined one is present.
 */
public class EnsurePresentedMdocMdlElementsAreDefined extends AbstractEnsurePresentedMdocElementsAreDefined {

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
	protected boolean isDefined(String namespace, String elementIdentifier) {
		return MdlDataElements.isDefined(namespace, elementIdentifier);
	}
}
