package net.openid.conformance.oauth.statuslists;

import java.io.ByteArrayOutputStream;
import java.io.Serial;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A wrapper around a compressed status list from the Token Status List (TSL).
 * See: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12
 */
public class TokenStatusList {

	private final byte[] bytes;
	private final int bits;

	public TokenStatusList(byte[] bytes, int bits) {
		this.bytes = bytes;
		this.bits = bits;
	}

	public static TokenStatusList decode(String encodedStatusList, int bits) {
		try {
			return new TokenStatusList(decodeStatusList(encodedStatusList), bits);
		} catch (Exception e) {
			throw new TokenStatusListException("Could not decode compressed status list representation", e);
		}
	}

	public static byte[] decodeStatusList(String encodedStatusList) throws Exception {

		byte[] compressed = Base64.getUrlDecoder().decode(encodedStatusList);

		Inflater inflater = new Inflater(); // ZLIB format
		inflater.setInput(compressed);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				output.write(buffer, 0, count);
			}
		} finally {
			inflater.end();
		}
		return output.toByteArray();
	}

	public Status getStatus(int idx) {
		return getStatus(idx, bits);
	}

	protected Status getStatus(int index, int bitsPerEntry) {
		int v = getPackedValue(index, bitsPerEntry);
		return switch (v) {
			case 0 -> Status.VALID;
			case 1 -> Status.INVALID;
			case 2 -> Status.SUSPENDED;
			case 3 -> Status.STATUS_0X03; // used in tests
			default -> throw new TokenStatusListException("Unknown status code: " + v);
		};
	}

	/**
	 * LSB-first, entries packed back-to-back.
	 */
	private int getPackedValue(int index, int bitsPerEntry) {
		if (bitsPerEntry <= 0 || bitsPerEntry > 32) {
			throw new TokenStatusListException("bitsPerEntry must be 1..32");
		}
		long mask = (bitsPerEntry == 32) ? 0xFFFF_FFFFL : ((1L << bitsPerEntry) - 1);

		int bitOffset = index * bitsPerEntry;
		int byteIndex = bitOffset >>> 3;     // / 8
		int bitInByte = bitOffset & 7;       // % 8

		// Build up to 8 bytes into a little-endian 64-bit chunk
		long chunk = 0;
		for (int i = 0; i < 8; i++) {
			int pos = byteIndex + i;
			if (pos >= bytes.length) {
				break;
			}
			chunk |= ((long) (bytes[pos] & 0xFF)) << (8 * i);
		}
		return (int) ((chunk >>> bitInByte) & mask);
	}

	public static TokenStatusList create(byte[] rawEntries, int bitsPerEntry) {
		if (bitsPerEntry <= 0 || bitsPerEntry > 32) {
			throw new IllegalArgumentException("bitsPerEntry must be 1..32");
		}

		byte[] bytes = packEntries(rawEntries, bitsPerEntry);
		return new TokenStatusList(bytes, bitsPerEntry);
	}

	public String encodeStatusList() {
		byte[] z = compressZlib(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(z);
	}

	private static byte[] packEntries(byte[] entries, int bitsPerEntry) {
		int totalBits = entries.length * bitsPerEntry;
		byte[] out = new byte[(totalBits + 7) >>> 3];

		int maxVal = (bitsPerEntry == 32) ? -1 : (1 << bitsPerEntry);
		for (int i = 0; i < entries.length; i++) {
			int v = entries[i] & 0xFF;
			if (bitsPerEntry < 32 && v >= maxVal) {
				throw new IllegalArgumentException("entry " + i + " out of range for " + bitsPerEntry + " bits");
			}
			int base = i * bitsPerEntry;
			for (int b = 0; b < bitsPerEntry; b++) {
				if (((v >>> b) & 1) == 1) {
					int bitIndex = base + b;        // LSB-first within entry
					out[bitIndex >>> 3] |= (byte) (1 << (bitIndex & 7));
				}
			}
		}
		return out;
	}

	private static byte[] compressZlib(byte[] data) {
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, false);   // zlib (nowrap=false)
		deflater.setInput(data);
		deflater.finish();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[512];
		try {
			while (!deflater.finished()) {
				int n = deflater.deflate(buf);
				if (n == 0 && deflater.needsInput()) {
					break;
				}
				baos.write(buf, 0, n);
			}
		} finally {
			deflater.end();
		}
		return baos.toByteArray();
	}

	/**
	 * See: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-7.1
	 */
	public enum Status {

		VALID(0x00),

		INVALID(0x01),

		SUSPENDED(0x02),

		// made up from example in https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.1
		STATUS_0X03(0x03);

		private final int typeValue;

		Status(int typeValue) {
			this.typeValue = typeValue;
		}

		public int getTypeValue() {
			return typeValue;
		}

		public static Status valueOf(byte codetypeValue) {
			for (Status status : Status.values()) {
				if (status.typeValue == codetypeValue) {
					return status;
				}
			}
			throw new TokenStatusListException("invalid status type value: " + codetypeValue);
		}
	}

	public static class TokenStatusListException extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		public TokenStatusListException(String message) {
			super(message);
		}

		public TokenStatusListException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
