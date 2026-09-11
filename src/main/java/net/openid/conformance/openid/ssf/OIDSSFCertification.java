package net.openid.conformance.openid.ssf;

import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

/**
 * Computes the certification profile names of the Shared Signals plans, one method per plan
 * that grants a profile. A profile name records what the run exercised: the role, the delivery
 * method, the OAuth grant the suite used to obtain or issue the access token, and the client
 * authentication method at the token endpoint.
 * <p>
 * Today only the two CAEP Interop Profile plans grant a profile. The plain SSF plans are not
 * part of the certification program and return none; a method for them belongs here once that
 * changes.
 */
public final class OIDSSFCertification {

	/** The certification target of the CAEP Interop Profile plans. */
	public static final String CAEP_INTEROP_PROFILE = "OIDSSF-1.0-FINAL+CAEPIOP-1.0-DRAFT01";

	/**
	 * CAEP Interop Profile 2.7.1 lets the authorization server offer the client credentials
	 * grant or the authorization code grant; the suite implements only the client credentials
	 * grant on both sides, so this part of the name is fixed until the authorization code grant
	 * is implemented and becomes a variant.
	 */
	public static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

	private static final String ROLE_TRANSMITTER = "Transmitter";

	private static final String ROLE_RECEIVER = "Receiver";

	private OIDSSFCertification() {
	}

	/** Profile name of {@link OIDSSFTransmitterTestPlanCaepInterop}. */
	public static List<String> caepInteropTransmitterProfileName(VariantSelection variantSelection) {
		return caepInteropProfileName(variantSelection, ROLE_TRANSMITTER);
	}

	/** Profile name of {@link OIDSSFReceiverTestPlanCaepInterop}. */
	public static List<String> caepInteropReceiverProfileName(VariantSelection variantSelection) {
		return caepInteropProfileName(variantSelection, ROLE_RECEIVER);
	}

	/**
	 * The CAEP Interop Profile (2.4.3, 2.7) requires OAuth 2.0, so only {@link SsfAuthMode#DYNAMIC}
	 * is certifiable; a pre-shared bearer string yields no profile name. RFC 6749 4.4 reserves the
	 * client credentials grant for confidential clients, so a run without client authentication,
	 * which stays available for testing a transmitter whose token endpoint needs none, yields no
	 * profile name either.
	 *
	 * @return the single profile name, or an empty list when the variant selection is not
	 *         certifiable
	 */
	private static List<String> caepInteropProfileName(VariantSelection variantSelection, String role) {
		String authMode = variantSelection.getVariantParameterValue(SsfAuthMode.class);
		if (!SsfAuthMode.DYNAMIC.name().equalsIgnoreCase(authMode)) {
			return List.of();
		}
		String deliveryMethod = variantSelection.getVariantParameterValue(SsfDeliveryMode.class);
		String clientAuthType = variantSelection.getVariantParameterValue(ClientAuthType.class);
		if (deliveryMethod == null || clientAuthType == null
			|| ClientAuthType.NONE.toString().equalsIgnoreCase(clientAuthType)) {
			return List.of();
		}
		return List.of(String.join(" ", CAEP_INTEROP_PROFILE, role, deliveryMethod, GRANT_CLIENT_CREDENTIALS, clientAuthType));
	}
}
