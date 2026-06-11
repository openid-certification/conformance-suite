/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.conformance.support.networknt;

import com.networknt.schema.SchemaException;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Detects the JSON Schema specification version from a schema document's {@code $schema} tag.
 *
 * <p>This is a local copy of {@code com.networknt.schema.SpecificationVersionDetector}, which
 * networknt moved into their <em>test</em> module (see
 * <a href="https://github.com/networknt/json-schema-validator/issues/1206">networknt/json-schema-validator#1206</a>)
 * and is therefore not available on the runtime classpath of json-schema-validator 3.x.
 *
 * <p>Adapted from the upstream source: the test-only {@code Path}-based detection helpers are
 * omitted, and the package-private {@code SchemaRegistry.normalizeDialectId(String)} call is
 * replaced with the equivalent local {@link #normalizeDialectId(String)} (stripping the trailing
 * {@code '#'} fragment that draft-04/06/07 append to their {@code $schema} URIs).
 */
public final class SpecificationVersionDetector {

	private static final String SCHEMA_TAG = "$schema";

	private SpecificationVersionDetector() {
		// Prevent instantiation of this utility class
	}

	/**
	 * Detects schema version based on the schema tag: if the schema tag is not present, throws
	 * {@link SchemaException} with the corresponding message, otherwise - returns the detected spec version.
	 *
	 * @param jsonNode JSON Node to read from
	 * @return Spec version if present, otherwise throws an exception
	 */
	public static SpecificationVersion detect(JsonNode jsonNode) {
		return detectOptionalVersion(jsonNode, true).orElseThrow(
				() -> new SchemaException("'" + SCHEMA_TAG + "' tag is not present")
		);
	}

	/**
	 * Detects schema version based on the schema tag: if the schema tag is not present, returns an empty {@link
	 * Optional} value, otherwise - returns the detected spec version wrapped into {@link Optional}.
	 *
	 * @param jsonNode JSON Node to read from
	 * @param throwIfUnsupported whether to throw an exception if the version is not supported
	 * @return Spec version if present, otherwise empty
	 */
	public static Optional<SpecificationVersion> detectOptionalVersion(JsonNode jsonNode, boolean throwIfUnsupported) {
		return Optional.ofNullable(jsonNode.get(SCHEMA_TAG)).map(schemaTag -> {

			String schemaTagValue = schemaTag.asString();
			String schemaUri = normalizeDialectId(schemaTagValue);

			if (throwIfUnsupported) {
				return SpecificationVersion.fromDialectId(schemaUri)
						.orElseThrow(() -> new SchemaException("'" + schemaTagValue + "' is unrecognizable schema"));
			} else {
				return SpecificationVersion.fromDialectId(schemaUri).orElse(null);
			}
		});
	}

	/**
	 * Local equivalent of the package-private {@code SchemaRegistry.normalizeDialectId(String)}: drafts 4/6/7
	 * declare {@code $schema} with a trailing {@code '#'} fragment which must be removed to match the canonical
	 * dialect ids known to {@link SpecificationVersion#fromDialectId(String)}.
	 */
	private static String normalizeDialectId(String dialectId) {
		if (dialectId != null && dialectId.endsWith("#")) {
			return dialectId.substring(0, dialectId.length() - 1);
		}
		return dialectId;
	}
}
