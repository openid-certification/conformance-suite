package net.openid.conformance.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the active partner theme (FEASIBILITY SPIKE).
 *
 * <p>A theme is a single canonical JSON document (see docs/theming-spike/) holding
 * partner branding (accent color, logo) and optional pre-baked test-configuration
 * presets. Two providers feed the same schema:</p>
 *
 * <ul>
 *   <li><b>File (configuration as code, self-hosting journey)</b> — when the
 *   {@code fintechlabs.theme.dir} property points at a directory containing
 *   {@code theme.json} (and optionally a logo file referenced by
 *   {@code brand.logo.file}), that theme is authoritative. The file is re-read on
 *   every request so a self-hosting partner can edit-and-refresh; production would
 *   want caching keyed on mtime.</li>
 *   <li><b>Database (self-serve UI journey, OIDF-hosted)</b> — a single document in
 *   the {@code THEME} collection, written by the admin-only POST /api/theme.</li>
 * </ul>
 *
 * <p>Precedence: file wins and locks the UI (the admin page renders read-only when
 * {@code source == FILE}) — deployment configuration should never silently lose to a
 * click in a web form.</p>
 */
@Service
public class ThemeService {

	public static final String COLLECTION = "THEME";
	public static final String CURRENT_ID = "current";

	private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
	private static final Pattern PRESET_ID = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");
	// the data-URL pattern is also the content-type allowlist (png / jpeg / svg)
	private static final Pattern DATA_URL = Pattern.compile("^data:(image/(?:png|jpeg|svg\\+xml));base64,([A-Za-z0-9+/=\\s]+)$");
	private static final int MAX_LOGO_BYTES = 512 * 1024;

	public enum Source { FILE, DATABASE, NONE }

	public record ResolvedTheme(Source source, JsonObject theme) {}

	/** Decoded logo payload. A plain class (not a record) because Error Prone
	 * forbids array record components. */
	public static final class Logo {
		private final byte[] bytes;
		private final String contentType;

		public Logo(byte[] bytes, String contentType) {
			this.bytes = bytes;
			this.contentType = contentType;
		}

		public byte[] bytes() {
			return bytes;
		}

		public String contentType() {
			return contentType;
		}
	}

	@Value("${fintechlabs.theme.dir:}")
	private String themeDir;

	@Autowired
	private MongoTemplate mongoTemplate;

	public ResolvedTheme getActiveTheme() {
		JsonObject fileTheme = readFileTheme();
		if (fileTheme != null) {
			return new ResolvedTheme(Source.FILE, fileTheme);
		}
		Document doc = mongoTemplate.findById(CURRENT_ID, Document.class, COLLECTION);
		if (doc != null && doc.getString("themeJson") != null) {
			try {
				return new ResolvedTheme(Source.DATABASE, JsonParser.parseString(doc.getString("themeJson")).getAsJsonObject());
			} catch (JsonSyntaxException | IllegalStateException e) {
				// fall through to NONE; a corrupt stored theme must not take the UI down
			}
		}
		return new ResolvedTheme(Source.NONE, null);
	}

	public boolean isFileManaged() {
		return readFileTheme() != null;
	}

	public void saveTheme(JsonObject theme, Map<String, String> updatedBy) {
		Document doc = new Document()
			.append("_id", CURRENT_ID)
			.append("themeJson", theme.toString())
			.append("updatedAt", new Date().getTime())
			.append("updatedBy", updatedBy);
		mongoTemplate.remove(new Query(Criteria.where("_id").is(CURRENT_ID)), COLLECTION);
		mongoTemplate.insert(doc, COLLECTION);
	}

	public void deleteTheme() {
		mongoTemplate.remove(new Query(Criteria.where("_id").is(CURRENT_ID)), COLLECTION);
	}

	/**
	 * The accent override block served as /api/theme/css and linked from every page
	 * head right after oidf-tokens.css.
	 *
	 * <p>The token system is brand-coupled (components consume the --orange-* ramp
	 * directly), so the override re-points the ramp at the partner accent. Shades are
	 * derived in the browser with color-mix(in oklab, ...) so a partner only has to
	 * supply ONE color; an optional {@code brand.ramp} object allows explicit
	 * per-step overrides when the derived shades don't suit a very light/dark brand.
	 * Status colors are semantic, not brand: --status-warning aliases --orange-500 in
	 * oidf-tokens.css, so it is re-pinned to the OIDF literals here. The brand-pure
	 * certification orange (--oidf-orange-pure, logo + Certified mark) is deliberately
	 * NOT themed — it identifies the OIDF certification program, not the partner.</p>
	 */
	public String generateCss(JsonObject theme) {
		if (theme == null) {
			return "/* no partner theme configured */\n";
		}
		JsonObject brand = theme.getAsJsonObject("brand");
		if (brand == null || !brand.has("accent")) {
			return "/* partner theme has no brand.accent */\n";
		}
		String accent = OIDFJSON.getString(brand.get("accent"));
		if (!HEX_COLOR.matcher(accent).matches()) {
			return "/* invalid brand.accent */\n";
		}

		StringBuilder ramp = new StringBuilder();
		JsonObject explicit = brand.has("ramp") && brand.get("ramp").isJsonObject() ? brand.getAsJsonObject("ramp") : new JsonObject();
		String[][] derived = {
			{"50", "color-mix(in oklab, var(--theme-accent), white 88%)"},
			{"100", "color-mix(in oklab, var(--theme-accent), white 72%)"},
			{"200", "color-mix(in oklab, var(--theme-accent), white 50%)"},
			{"300", "color-mix(in oklab, var(--theme-accent), white 25%)"},
			{"400", "var(--theme-accent)"},
			{"500", "color-mix(in oklab, var(--theme-accent), black 14%)"},
			{"600", "color-mix(in oklab, var(--theme-accent), black 30%)"},
			{"700", "color-mix(in oklab, var(--theme-accent), black 46%)"},
		};
		for (String[] step : derived) {
			String value = step[1];
			if (explicit.has(step[0])) {
				String explicitValue = OIDFJSON.getString(explicit.get(step[0]));
				if (HEX_COLOR.matcher(explicitValue).matches()) {
					value = explicitValue;
				}
			}
			ramp.append("  --orange-").append(step[0]).append(": ").append(value).append(";\n");
		}

		return """
			/* Generated by ThemeService - partner accent override (spike).
			 * Loaded after oidf-tokens.css on every page; later :root wins. */
			:root {
			  --theme-accent: %s;
			%s  --oidf-orange: var(--theme-accent);
			  --focus-ring: 0 0 0 3px color-mix(in srgb, var(--theme-accent) 35%%, transparent);

			  /* Status colors are semantic (warn = amber), not brand. oidf-tokens.css
			   * aliases --status-warning to --orange-500, so re-pin the OIDF literals
			   * or the partner accent would leak into warning badges. */
			  --status-warning: #D27420;
			  --status-warning-bg: #FDF1E5;
			  --status-warning-border: #FADCBE;
			}
			""".formatted(accent, ramp);
	}

	/**
	 * Decode the active theme's logo. Accepts either an embedded data URL
	 * ({@code brand.logo.data}, the storage form used by the admin UI / database) or
	 * a file reference ({@code brand.logo.file}, the configuration-as-code form,
	 * resolved against the theme directory).
	 */
	public Logo getLogo(ResolvedTheme resolved) {
		if (resolved.theme() == null) {
			return null;
		}
		JsonObject logo = getLogoObject(resolved.theme());
		if (logo == null) {
			return null;
		}
		if (logo.has("data")) {
			var matcher = DATA_URL.matcher(OIDFJSON.getString(logo.get("data")));
			if (matcher.matches()) {
				return new Logo(Base64.getMimeDecoder().decode(matcher.group(2)), matcher.group(1));
			}
			return null;
		}
		if (resolved.source() == Source.FILE && logo.has("file")) {
			String fileName = OIDFJSON.getString(logo.get("file"));
			Path base = Path.of(themeDir).toAbsolutePath().normalize();
			Path logoPath = base.resolve(fileName).normalize();
			if (!logoPath.startsWith(base)) {
				return null; // path traversal attempt
			}
			try {
				byte[] bytes = Files.readAllBytes(logoPath);
				return new Logo(bytes, contentTypeForFileName(fileName));
			} catch (IOException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Spike-level validation. Production needs proper SVG sanitization (an allowlist
	 * parser, not substring checks) and contrast guidance; see the findings doc.
	 */
	public List<String> validate(JsonObject theme) {
		List<String> errors = new ArrayList<>();
		JsonObject partner = theme.has("partner") && theme.get("partner").isJsonObject() ? theme.getAsJsonObject("partner") : null;
		if (partner == null || !partner.has("name") || OIDFJSON.getString(partner.get("name")).isBlank()) {
			errors.add("'partner.name' is required");
		} else if (OIDFJSON.getString(partner.get("name")).length() > 60) {
			errors.add("'partner.name' must be 60 characters or fewer");
		}

		JsonObject brand = theme.has("brand") && theme.get("brand").isJsonObject() ? theme.getAsJsonObject("brand") : null;
		if (brand == null || !brand.has("accent")) {
			errors.add("'brand.accent' is required");
		} else if (!HEX_COLOR.matcher(OIDFJSON.getString(brand.get("accent"))).matches()) {
			errors.add("'brand.accent' must be a #RRGGBB hex color");
		}

		JsonObject logo = brand == null ? null : getLogoObject(theme);
		if (logo != null && logo.has("data")) {
			var matcher = DATA_URL.matcher(OIDFJSON.getString(logo.get("data")));
			if (!matcher.matches()) {
				errors.add("'brand.logo.data' must be a base64 data URL of type image/png, image/jpeg or image/svg+xml");
			} else {
				byte[] bytes = Base64.getMimeDecoder().decode(matcher.group(2));
				if (bytes.length > MAX_LOGO_BYTES) {
					errors.add("Logo must be 512KB or smaller");
				}
				if ("image/svg+xml".equals(matcher.group(1))) {
					String svg = new String(bytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
					if (svg.contains("<script") || svg.contains("javascript:") || svg.contains("onload=")) {
						errors.add("SVG logo must not contain scripts");
					}
				}
			}
		}

		if (theme.has("presets")) {
			if (!theme.get("presets").isJsonArray()) {
				errors.add("'presets' must be an array");
			} else {
				JsonArray presets = theme.getAsJsonArray("presets");
				Set<String> seen = new HashSet<>();
				for (JsonElement element : presets) {
					if (!element.isJsonObject()) {
						errors.add("each preset must be an object");
						continue;
					}
					JsonObject preset = element.getAsJsonObject();
					String id = preset.has("id") ? OIDFJSON.getString(preset.get("id")) : "";
					if (!PRESET_ID.matcher(id).matches()) {
						errors.add("preset id '" + id + "' must match [a-z0-9-]{1,64}");
					}
					if (!seen.add(id)) {
						errors.add("duplicate preset id '" + id + "'");
					}
					if (!preset.has("label") || OIDFJSON.getString(preset.get("label")).isBlank()) {
						errors.add("preset '" + id + "' needs a label");
					}
					if (!preset.has("planName") || OIDFJSON.getString(preset.get("planName")).isBlank()) {
						errors.add("preset '" + id + "' needs a planName");
					}
				}
			}
		}
		return errors;
	}

	private JsonObject getLogoObject(JsonObject theme) {
		JsonObject brand = theme.has("brand") && theme.get("brand").isJsonObject() ? theme.getAsJsonObject("brand") : null;
		if (brand == null || !brand.has("logo") || !brand.get("logo").isJsonObject()) {
			return null;
		}
		return brand.getAsJsonObject("logo");
	}

	private JsonObject readFileTheme() {
		if (themeDir == null || themeDir.isBlank()) {
			return null;
		}
		Path themeJson = Path.of(themeDir, "theme.json");
		if (!Files.isReadable(themeJson)) {
			return null;
		}
		try {
			return JsonParser.parseString(Files.readString(themeJson, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException | JsonSyntaxException | IllegalStateException e) {
			return null;
		}
	}

	private String contentTypeForFileName(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".svg")) {
			return "image/svg+xml";
		}
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		return "image/png";
	}
}
