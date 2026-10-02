package net.openid.conformance.settings;

/**
 * A reason a settings save was refused.
 *
 * @param field the request field it concerns, e.g. {@code positiveCertificates[0].privateKeyPem};
 *              empty when it concerns the request as a whole
 * @param message shown to the admin, naming fields by their labels on the settings page
 */
public record SettingsError(String field, String message) {
}
