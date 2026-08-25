package net.openid.conformance.condition.client;

import org.multipaz.documenttype.knowntypes.PhotoID;

import java.util.Set;

/**
 * Checks that an issued mdoc credential with docType org.iso.23220.photoid.1 contains all the
 * data elements that ISO/IEC TS 23220-4 Annex C Table 1 marks as mandatory in the
 * org.iso.23220.1 namespace.
 *
 * Annex C is nominally an informative annex, but it is the only definition of the photo ID
 * profile that implementers have, so this suite treats it as normative. Its note on the presence
 * column says mandatory "solely means that the photo ID issuer shall ensure that this element is
 * present" — it does not require a reader to request the element, nor prevent a holder from
 * refusing to release it, which is why this check only applies at issuance.
 *
 * The element identifiers are those of ISO/IEC TS 23220-2 ed.2; that edition's Table 2 footnote a
 * records that the "_unicode" suffixed identifiers of the previous edition (family_name_unicode,
 * given_name_unicode, issuing_authority_unicode) have been replaced by the plain identifiers used
 * here. Older ISO/IEC TS 23220-4 drafts (e.g. WG4 N 4583) still list the "_unicode" names.
 */
public class EnsureMdocPhotoIdMandatoryDataElementsPresent
		extends AbstractEnsureMdocMandatoryDataElementsPresent {

	/** ISO/IEC TS 23220-4 Annex C Table 1, elements with presence "M". */
	public static final Set<String> MANDATORY_ELEMENTS = Set.of(
		"family_name",
		"given_name",
		"birth_date",
		"portrait",
		"issue_date",
		"expiry_date",
		"issuing_authority",
		"issuing_country",
		"age_over_18");

	@Override
	protected String getDocType() {
		return PhotoID.PHOTO_ID_DOCTYPE;
	}

	@Override
	protected String getNamespace() {
		return PhotoID.ISO_23220_2_NAMESPACE;
	}

	@Override
	protected Set<String> getMandatoryElements() {
		return MANDATORY_ELEMENTS;
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
