#!/usr/bin/env python
#
# python wrapper for conformance suite API

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import json
import re
import os
import shutil
import time
import aiohttp
import asyncio
import aiofiles
import string
import re
from types import SimpleNamespace

from aiohttp import ClientSession, TraceConfig
from aiohttp_retry import RetryClient, ExponentialRetry

# Retrying on the ClientPayloadError appears to be needed because of a bug in aiohttp
# possibly https://github.com/aio-libs/aiohttp/issues/4581
retry_options = ExponentialRetry(attempts=5, start_timeout=1, exceptions=[aiohttp.ClientPayloadError, aiohttp.ServerDisconnectedError])
http_debug = False

replchars = re.compile('([^' + re.escape(string.printable) + '])')
def nonprintable_to_hex_inner(match):
    return r'\x{0:02x}'.format(ord(match.group()))

def nonprintable_to_hex(s):
    return replchars.sub(nonprintable_to_hex_inner, s)

async def on_request_start(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceRequestStartParams,
) -> None:
    current_attempt = trace_config_ctx.trace_request_ctx['current_attempt']
    trace_config_ctx.what = "{} {}".format(params.method, params.url)
    print('{} {} attempt {} of {}'.format(params.method, params.url, current_attempt, retry_options.attempts))

async def on_request_end(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceRequestEndParams,
) -> None:
    current_attempt = trace_config_ctx.trace_request_ctx['current_attempt']
    trace_config_ctx.what = "{} {}".format(params.method, params.url)
    if params.response.status in [200,201]:
        body = await params.response.text('ISO-8859-1')
        print('{} {} {} {} headers {} body starts "{}"'.format(params.method, params.url, params.response.status, params.response.reason, params.response.headers, nonprintable_to_hex(body[:100])))
    else:
        print('{} {} {} {} headers {} body {}'.format(params.method, params.url, params.response.status, params.response.reason, params.response.headers, await params.response.text()))


async def on_request_exception(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceRequestExceptionParams,
) -> None:
    print('{} {} exception {}'.format(params.method, params.url, params.exception))

async def on_connection_reuseconn(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceConnectionReuseconnParams,
) -> None:
    print('{} on_connection_reuseconn'.format(trace_config_ctx.what))

async def on_connection_queued_start(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceConnectionQueuedStartParams,
) -> None:
    print('{} on_connection_queued_start'.format(trace_config_ctx.what))

async def on_connection_queued_end(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceConnectionQueuedEndParams,
) -> None:
    print('{} on_connection_queued_end'.format(trace_config_ctx.what))

async def on_connection_create_start(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceConnectionCreateStartParams,
) -> None:
    print('{} on_connection_create_start'.format(trace_config_ctx.what))

async def on_connection_create_end(
    session: ClientSession,
    trace_config_ctx: SimpleNamespace,
    params: aiohttp.TraceConnectionCreateEndParams,
) -> None:
    print('{} on_connection_create_end'.format(trace_config_ctx.what))

class Conformance(object):
    def __init__(self, api_url_base, api_token, verify_ssl):
        if not api_url_base.endswith('/'):
            api_url_base += "/"
        self.api_url_base = api_url_base

        headers = {'Content-Type': 'application/json'}
        if api_token is not None:
            headers['Authorization'] = 'Bearer {0}'.format(api_token)
        conn = aiohttp.TCPConnector(verify_ssl=verify_ssl)
        trace_config = TraceConfig()
        if http_debug:
            trace_config.on_request_start.append(on_request_start)
            trace_config.on_request_end.append(on_request_end)
            trace_config.on_request_exception.append(on_request_exception)
            trace_config.on_connection_reuseconn.append(on_connection_reuseconn)
            trace_config.on_connection_queued_start.append(on_connection_queued_start)
            trace_config.on_connection_queued_end.append(on_connection_queued_end)
            trace_config.on_connection_create_start.append(on_connection_create_start)
            trace_config.on_connection_create_end.append(on_connection_create_end)
        self.requests_session = RetryClient(raise_for_status=False, retry_options=retry_options, headers=headers, connector=conn, trace_configs=[trace_config])

    async def get_all_test_modules(self):
        """ Returns an array containing a dictionary per test module """
        api_url = '{0}api/runner/available'.format(self.api_url_base)
        async with self.requests_session.get(api_url) as response:
            if response.status != 200:
                raise Exception("get_all_test_modules failed - HTTP {:d} {}".format(response.status, await response.text()))
            return await response.json()

    async def exporthtml(self, plan_id, path):
        api_url = '{0}api/plan/exporthtml/{1}'.format(self.api_url_base, plan_id)
        async with self.requests_session.get(api_url) as response:
            if response.status != 200:
                raise Exception("exporthtml failed - HTTP {:d} {}".format(response.status, await response.text()))
            d = response.headers['content-disposition']
            local_filename = re.findall("filename=\"(.+)\"", d)[0]
            full_path = os.path.join(path, local_filename)
            f = await aiofiles.open(full_path, mode='wb')
            await f.write(await response.read())
            await f.close()
        return full_path

    async def create_test_plan(self, name, configuration, variant=None):
        api_url = '{0}api/plan'.format(self.api_url_base)
        payload = {'planName': name}
        if variant != None:
            payload['variant'] = json.dumps(variant)
        response = await self.requests_session.post(api_url, params=payload, data=configuration)

        if response.status != 201:
            raise Exception("create_test_plan failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def create_test(self, test_name, configuration):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name}
        response = await self.requests_session.post(api_url, params=payload, data=configuration)

        if response.status != 201:
            raise Exception("create_test failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def create_test_from_plan(self, plan_id, test_name):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        response = await self.requests_session.post(api_url, params=payload)

        if response.status != 201:
            raise Exception("create_test_from_plan failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def create_test_from_plan_with_variant(self, plan_id, test_name, variant):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        if variant != None:
            payload['variant'] = json.dumps(variant)
        response = await self.requests_session.post(api_url, params=payload)

        if response.status != 201:
            raise Exception("create_test_from_plan failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def get_module_info(self, module_id):
        api_url = '{0}api/info/{1}'.format(self.api_url_base, module_id)
        response = await self.requests_session.get(api_url)

        if response.status != 200:
            raise Exception("get_module_info failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def get_test_log(self, module_id):
        api_url = '{0}api/log/{1}'.format(self.api_url_base, module_id)
        response = await self.requests_session.get(api_url)

        if response.status != 200:
            raise Exception("get_test_log failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def start_test(self, module_id):
        api_url = '{0}api/runner/{1}'.format(self.api_url_base, module_id)
        response = await self.requests_session.post(api_url)

        if response.status != 200:
            raise Exception("start_test failed - HTTP {:d} {}".format(response.status, await response.text()))
        return await response.json()

    async def wait_for_state(self, module_id, required_states, timeout=240):
        timeout_at = time.time() + timeout
        while True:
            if time.time() > timeout_at:
                raise Exception("Timed out waiting for test module {} to be in one of states: {}".
                                format(module_id, required_states))

            info = await self.get_module_info(module_id)

            status = info['status']
            print("module id {} status is {}".format(module_id, status))
            if status in required_states:
                return status
            if status == 'INTERRUPTED':
                raise Exception("Test module {} has moved to INTERRUPTED".format(module_id))

            await asyncio.sleep(1)
