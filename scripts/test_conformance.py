import unittest

from conformance import Conformance, ServerUnavailableError, UnrecoverableHTTPError


# Sentinel: a FakeResponse created without an explicit json_body raises on .json(),
# mimicking httpx raising when a body is not valid JSON.
_RAISE_JSON = object()


class FakeResponse:
    def __init__(self, status_code, json_body=_RAISE_JSON, headers=None, content=b''):
        self.status_code = status_code
        self._json_body = json_body
        self.headers = headers if headers is not None else {}
        self.content = content

    def json(self):
        if self._json_body is _RAISE_JSON:
            raise ValueError("response body is not valid JSON")
        return self._json_body


class FakeClient:
    """Minimal stand-in for httpx.AsyncClient. `handler(url, kwargs)` returns a
    FakeResponse or raises an exception. Every call is recorded for assertions."""

    def __init__(self, handler):
        self._handler = handler
        self.calls = []
        self.headers = {}

    async def get(self, url, **kwargs):
        self.calls.append((url, kwargs))
        return self._handler(url, kwargs)


def make_conformance():
    # Constructs a real httpx client (no network until used); tests replace the
    # client attribute with a FakeClient before exercising any path.
    return Conformance('https://server.example/', 'test-token', False)


class LongPollTest(unittest.IsolatedAsyncioTestCase):

    def setUp(self):
        self.c = make_conformance()

    def _set_responses(self, wait_state_responses, info_response=None):
        ws = list(wait_state_responses)

        def handler(url, kwargs):
            if 'wait-state' in url:
                if not ws:
                    raise AssertionError("unexpected extra wait-state call to " + url)
                r = ws.pop(0)
                if isinstance(r, Exception):
                    raise r
                return r
            if '/info/' in url:
                if info_response is None:
                    raise AssertionError("unexpected /info/ call to " + url)
                if isinstance(info_response, Exception):
                    raise info_response
                return info_response
            raise AssertionError("unexpected url " + url)

        self.c.httpclient = FakeClient(handler)

    async def test_returns_wanted_state(self):
        self._set_responses([FakeResponse(200, json_body={'state': 'FINISHED'})])
        self.assertEqual(await self.c.wait_for_state('m1', ['FINISHED']), 'FINISHED')

    async def test_sends_states_and_timeout_as_params(self):
        self._set_responses([FakeResponse(200, json_body={'state': 'FINISHED'})])
        await self.c.wait_for_state('m1', ['WAITING', 'FINISHED'])
        _, kwargs = self.c.httpclient.calls[0]
        # INTERRUPTED is always appended; httpx URL-encodes params for us.
        self.assertEqual(kwargs['params']['states'], 'WAITING,FINISHED,INTERRUPTED')
        self.assertIn('timeoutMs', kwargs['params'])

    async def test_reissues_on_per_call_timeout(self):
        self._set_responses([
            FakeResponse(200, json_body={'timeout': True}),
            FakeResponse(200, json_body={'state': 'WAITING'}),
        ])
        self.assertEqual(await self.c.wait_for_state('m1', ['WAITING']), 'WAITING')
        self.assertEqual(len(self.c.httpclient.calls), 2)

    async def test_raises_on_interrupted_by_default(self):
        self._set_responses([FakeResponse(200, json_body={'state': 'INTERRUPTED'})])
        with self.assertRaises(Exception) as ctx:
            await self.c.wait_for_state('m1', ['FINISHED'])
        self.assertNotIsInstance(ctx.exception, (ServerUnavailableError, UnrecoverableHTTPError))
        self.assertIn('INTERRUPTED', str(ctx.exception))

    async def test_returns_interrupted_when_explicitly_requested(self):
        # A caller that lists INTERRUPTED gets it returned as a wanted state, not raised.
        self._set_responses([FakeResponse(200, json_body={'state': 'INTERRUPTED'})])
        self.assertEqual(
            await self.c.wait_for_state('m1', ['FINISHED', 'INTERRUPTED']), 'INTERRUPTED')

    async def test_404_with_persisted_nonterminal_is_retryable(self):
        # In-memory runner entry gone, persisted status non-terminal => restart-suspect.
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'RUNNING'}))
        with self.assertRaises(ServerUnavailableError):
            await self.c.wait_for_state('m1', ['FINISHED'])

    async def test_404_with_persisted_nonterminal_wanted_is_still_retryable(self):
        # Even when the persisted non-terminal status is one the caller wants (WAITING),
        # the runner has lost the test so we must NOT return it — the caller could not
        # drive a test that no longer exists. Retry the module instead.
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'WAITING'}))
        with self.assertRaises(ServerUnavailableError):
            await self.c.wait_for_state('m1', ['WAITING', 'FINISHED'])

    async def test_404_with_persisted_finished_returns_finished(self):
        # The test completed before its in-memory entry vanished (e.g. the long-poll
        # response was lost to a restart). Resolve from persisted info, do not rerun.
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'FINISHED'}))
        self.assertEqual(await self.c.wait_for_state('m1', ['FINISHED']), 'FINISHED')

    async def test_404_with_persisted_finished_not_requested_is_permanent(self):
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'FINISHED'}))
        with self.assertRaises(Exception) as ctx:
            await self.c.wait_for_state('m1', ['WAITING'])
        self.assertNotIsInstance(ctx.exception, ServerUnavailableError)
        self.assertIn('finished', str(ctx.exception).lower())

    async def test_404_with_persisted_interrupted_raises_by_default(self):
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'INTERRUPTED'}))
        with self.assertRaises(Exception) as ctx:
            await self.c.wait_for_state('m1', ['FINISHED'])
        self.assertNotIsInstance(ctx.exception, ServerUnavailableError)
        self.assertIn('INTERRUPTED', str(ctx.exception))

    async def test_404_with_persisted_interrupted_returns_when_requested(self):
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(200, json_body={'status': 'INTERRUPTED'}))
        self.assertEqual(
            await self.c.wait_for_state('m1', ['FINISHED', 'INTERRUPTED']), 'INTERRUPTED')

    async def test_404_without_persisted_info_is_permanent(self):
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=FakeResponse(404))
        with self.assertRaises(Exception) as ctx:
            await self.c.wait_for_state('m1', ['FINISHED'])
        self.assertNotIsInstance(ctx.exception, ServerUnavailableError)
        self.assertIn('not found', str(ctx.exception))

    async def test_404_then_status_check_connection_error_is_retryable(self):
        # If the persisted-status check itself can't reach the server, that's retryable.
        self._set_responses(
            [FakeResponse(404, json_body={'error': 'test not found'})],
            info_response=Exception("connection refused"))
        with self.assertRaises(ServerUnavailableError):
            await self.c.wait_for_state('m1', ['FINISHED'])

    async def test_502_is_retryable(self):
        self._set_responses([FakeResponse(502, content=b'bad gateway')])
        with self.assertRaises(ServerUnavailableError):
            await self.c.wait_for_state('m1', ['FINISHED'])

    async def test_401_is_unrecoverable(self):
        self._set_responses([FakeResponse(401)])
        with self.assertRaises(UnrecoverableHTTPError):
            await self.c.wait_for_state('m1', ['FINISHED'])

    async def test_overall_timeout_raises(self):
        self._set_responses([])  # budget exhausted before any call
        with self.assertRaises(Exception) as ctx:
            await self.c.wait_for_state('m1', ['FINISHED'], timeout=0)
        self.assertIn('Timed out', str(ctx.exception))


class CloseClientTest(unittest.IsolatedAsyncioTestCase):

    async def test_closes_client(self):
        c = make_conformance()
        closed = []

        class FakeClosable:
            async def aclose(self):
                closed.append(True)

        c.httpclient = FakeClosable()
        await c.close_client()
        self.assertEqual(closed, [True])


if __name__ == '__main__':
    unittest.main()
