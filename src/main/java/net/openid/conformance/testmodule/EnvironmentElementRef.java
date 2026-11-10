package net.openid.conformance.testmodule;

/**
 * A reference to an element inside an Environment object, as resolved by
 * {@link Environment#getElementFromObject(String, String)}.
 *
 * @param objectId the object in the environment to search
 * @param path the path within the object, in dot-separated format such as "foo.bar"
 */
public record EnvironmentElementRef(String objectId, String path) {
}
