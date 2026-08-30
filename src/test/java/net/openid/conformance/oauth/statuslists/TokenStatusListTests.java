package net.openid.conformance.oauth.statuslists;

import net.openid.conformance.oauth.statuslists.TokenStatusList.Status;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class TokenStatusListTests {

	@Test
	public void encodeStatusListWithOneBitEncoding() {

		// example from spec: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.1
		int bits = 1;
		byte[] input = new byte[16];
		input[0] = 1;
		input[1] = 0;
		input[2] = 0;
		input[3] = 1;
		input[4] = 1;
		input[5] = 1;
		input[6] = 0;
		input[7] = 1;
		input[8] = 1;
		input[9] = 1;
		input[10] = 0;
		input[11] = 0;
		input[12] = 0;
		input[13] = 1;
		input[14] = 0;
		input[15] = 1;

		TokenStatusList statusList = TokenStatusList.create(input, bits);

		String encoded = statusList.encodeStatusList();
		assertEquals("eNrbuRgAAhcBXQ", encoded);

		assertEquals(Status.INVALID, statusList.getStatus(0));
		assertEquals(Status.VALID, statusList.getStatus(1));
		assertEquals(Status.VALID, statusList.getStatus(2));
		assertEquals(Status.INVALID, statusList.getStatus(3));
		assertEquals(Status.INVALID, statusList.getStatus(4));
		assertEquals(Status.INVALID, statusList.getStatus(5));
		assertEquals(Status.VALID, statusList.getStatus(6));
		assertEquals(Status.INVALID, statusList.getStatus(7));
		assertEquals(Status.INVALID, statusList.getStatus(8));
		assertEquals(Status.INVALID, statusList.getStatus(9));
		assertEquals(Status.VALID, statusList.getStatus(10));
		assertEquals(Status.VALID, statusList.getStatus(11));
		assertEquals(Status.VALID, statusList.getStatus(12));
		assertEquals(Status.INVALID, statusList.getStatus(13));
		assertEquals(Status.VALID, statusList.getStatus(14));
		assertEquals(Status.INVALID, statusList.getStatus(15));
	}

	@Test
	public void decodeStatusListWithOneBitEncoding() {

		// https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.2

		// example from spec: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.1
		String lst = "eNrbuRgAAhcBXQ";
		int bits = 1;
		TokenStatusList statusList = TokenStatusList.decode(lst, bits);

		/*
		 * status[0] = 1
		 * status[1] = 0
		 * status[2] = 0
		 * status[3] = 1
		 * status[4] = 1
		 * status[5] = 1
		 * status[6] = 0
		 * status[7] = 1
		 * status[8] = 1
		 * status[9] = 1
		 * status[10] = 0
		 * status[11] = 0
		 * status[12] = 0
		 * status[13] = 1
		 * status[14] = 0
		 * status[15] = 1
		 */
		assertEquals(Status.INVALID, statusList.getStatus(0));
		assertEquals(Status.VALID, statusList.getStatus(1));
		assertEquals(Status.VALID, statusList.getStatus(2));
		assertEquals(Status.INVALID, statusList.getStatus(3));
		assertEquals(Status.INVALID, statusList.getStatus(4));
		assertEquals(Status.INVALID, statusList.getStatus(5));
		assertEquals(Status.VALID, statusList.getStatus(6));
		assertEquals(Status.INVALID, statusList.getStatus(7));
		assertEquals(Status.INVALID, statusList.getStatus(8));
		assertEquals(Status.INVALID, statusList.getStatus(9));
		assertEquals(Status.VALID, statusList.getStatus(10));
		assertEquals(Status.VALID, statusList.getStatus(11));
		assertEquals(Status.VALID, statusList.getStatus(12));
		assertEquals(Status.INVALID, statusList.getStatus(13));
		assertEquals(Status.VALID, statusList.getStatus(14));
		assertEquals(Status.INVALID, statusList.getStatus(15));
	}

	@Test
	public void decodeStatusListWithTwoBitEncoding() {

		// example from spec: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.2
		String lst = "eNo76fITAAPfAgc";
		int bits = 2;
		TokenStatusList statusList = TokenStatusList.decode(lst, bits);

		/*
		 *  status[0] = 1
		 *    status[1] = 2
		 *    status[2] = 0
		 *    status[3] = 3
		 *    status[4] = 0
		 *    status[5] = 1
		 *    status[6] = 0
		 *    status[7] = 1
		 *    status[8] = 1
		 *    status[9] = 2
		 *    status[10] = 3
		 *    status[11] = 3
		 */
		assertEquals(Status.INVALID, statusList.getStatus(0));
		assertEquals(Status.SUSPENDED, statusList.getStatus(1));
		assertEquals(Status.VALID, statusList.getStatus(2));
		assertEquals(Status.STATUS_0X03, statusList.getStatus(3));
		assertEquals(Status.VALID, statusList.getStatus(4));
		assertEquals(Status.INVALID, statusList.getStatus(5));
		assertEquals(Status.VALID, statusList.getStatus(6));
		assertEquals(Status.INVALID, statusList.getStatus(7));
		assertEquals(Status.INVALID, statusList.getStatus(8));
		assertEquals(Status.SUSPENDED, statusList.getStatus(9));
		assertEquals(Status.STATUS_0X03, statusList.getStatus(10));
		assertEquals(Status.STATUS_0X03, statusList.getStatus(11));
	}

	@Test
	public void decodeStatusListWithOneBitEncodingLarge() {

		// 1-bit test vector example from spec:
		// see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#autoid-78
		String lst = "eNrt3AENwCAMAEGogklACtKQPg9LugC9k_ACvreiogE" +
			"AAKkeCQAAAAAAAAAAAAAAAAAAAIBylgQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
			"AAAAAAAAAAAAAAAAAAAXG9IAAAAAAAAAPwsJAAAAAAAAAAAAAAAvhsSAAAAAAAAAAA" +
			"A7KpLAAAAAAAAAAAAAAAAAAAAAJsLCQAAAAAAAAAAADjelAAAAAAAAAAAKjDMAQAAA" +
			"ACAZC8L2AEb";
		int bits = 1;
		TokenStatusList statusList = TokenStatusList.decode(lst, bits);

		/*
		 *    status[0]=1
		 *    status[1993]=1
		 *    status[25460]=1
		 *    status[159495]=1
		 *    status[495669]=1
		 *    status[554353]=1
		 *    status[645645]=1
		 *    status[723232]=1
		 *    status[854545]=1
		 *    status[934534]=1
		 *    status[1000345]=1
		 */
		assertEquals(Status.INVALID, statusList.getStatus(0));
		assertEquals(Status.VALID, statusList.getStatus(1));
		assertEquals(Status.VALID, statusList.getStatus(2));
		assertEquals(Status.VALID, statusList.getStatus(1992));
		assertEquals(Status.INVALID, statusList.getStatus(1993));
		assertEquals(Status.VALID, statusList.getStatus(1994));
		assertEquals(Status.INVALID, statusList.getStatus(25460));
		assertEquals(Status.INVALID, statusList.getStatus(159495));
		assertEquals(Status.VALID, statusList.getStatus(1000344));
		assertEquals(Status.INVALID, statusList.getStatus(1000345));
	}

	@Test
	public void decodeStatusListWithTwoBitEncodingLarge() {

		// 2-bit test vector example from spec
		// See: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#autoid-79
		String lst = "eNrt2zENACEQAEEuoaBABP5VIO01fCjIHTMStt9ovGV" +
			"IAAAAAABAbiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEB5WwIAAAAAA" +
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAID0ugQAAAAAAAAAAAAAAAAAQG12SgAAA" +
			"AAAAAAAAAAAAAAAAAAAAAAAAOCSIQEAAAAAAAAAAAAAAAAAAAAAAAD8ExIAAAAAAAA" +
			"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwJEuAQAAAAAAAAAAAAAAAAAAAAAAAMB9S" +
			"wIAAAAAAAAAAAAAAAAAAACoYUoAAAAAAAAAAAAAAEBqH81gAQw";
		int bits = 2;
		TokenStatusList statusList = TokenStatusList.decode(lst, bits);

		/*
		 *  status[0]=1
		 *    status[1993]=2
		 *    status[25460]=1
		 *    status[159495]=3
		 *    status[495669]=1
		 *    status[554353]=1
		 *    status[645645]=2
		 *    status[723232]=1
		 *    status[854545]=1
		 *    status[934534]=2
		 *    status[1000345]=3
		 */
		assertEquals(Status.INVALID, statusList.getStatus(0));
		assertEquals(Status.VALID, statusList.getStatus(1));
		assertEquals(Status.VALID, statusList.getStatus(2));
		assertEquals(Status.VALID, statusList.getStatus(1992));
		assertEquals(Status.SUSPENDED, statusList.getStatus(1993));
		assertEquals(Status.VALID, statusList.getStatus(1994));
		assertEquals(Status.INVALID, statusList.getStatus(25460));
		assertEquals(Status.STATUS_0X03, statusList.getStatus(159495));
		assertEquals(Status.INVALID, statusList.getStatus(495669));
		assertEquals(Status.INVALID, statusList.getStatus(554353));
		assertEquals(Status.SUSPENDED, statusList.getStatus(645645));
		assertEquals(Status.INVALID, statusList.getStatus(723232));
		assertEquals(Status.INVALID, statusList.getStatus(854545));
		assertEquals(Status.SUSPENDED, statusList.getStatus(934534));
		assertEquals(Status.STATUS_0X03, statusList.getStatus(1000345));
		assertEquals(Status.VALID, statusList.getStatus(1000346));
	}

	@Test
	public void truncatedZlibStreamFailsInsteadOfHanging() {
		// a zlib stream cut off before its end used to spin the inflate loop forever,
		// hanging the test (and its lock) that was checking a received status list
		byte[] complete = deflate(new byte[1024]);
		byte[] truncated = java.util.Arrays.copyOf(complete, complete.length - 5);

		assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
			assertThrows(TokenStatusList.TokenStatusListException.class, () ->
				TokenStatusList.decodeCompressed(truncated, 1)));
	}

	@Test
	public void oversizedDecompressedStatusListIsRejected() {
		// 48MB of zeros deflates to a few kilobytes; inflating it must stop at the size cap
		byte[] bomb = deflate(new byte[48 * 1024 * 1024]);

		assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
			assertThrows(TokenStatusList.TokenStatusListException.class, () ->
				TokenStatusList.decodeCompressed(bomb, 1)));
	}

	private static byte[] deflate(byte[] data) {
		Deflater deflater = new Deflater();
		deflater.setInput(data);
		deflater.finish();
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		while (!deflater.finished()) {
			out.write(buffer, 0, deflater.deflate(buffer));
		}
		deflater.end();
		return out.toByteArray();
	}

}
