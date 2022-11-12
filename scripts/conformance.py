#!/usr/bin/env python3
#
# python wrapper for conformance suite API

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import asyncio
import json
import re
import httpx
import os
import shutil
import time
import traceback
import zipfile


class RetryTransport(httpx.HTTPTransport):
    def handle_request(
        self,
        request: httpx.Request,
    ) -> httpx.Response:
        retry = 0
        resp = None
        while retry < 5:
            retry += 1
            if retry > 2:
                time.sleep(1)
            try:
                if resp is not None:
                    resp.close()
                resp = super().handle_request(request)
            except Exception as e:
                print("httpx {} exception {} caught - retrying".format(request.url, e))
                continue
            if resp.status_code >= 500 and resp.status_code < 600:
                print("httpx {} 5xx response - retrying".format(request.url))
                continue
            content_type = resp.headers.get("Content-Type")
            if content_type is not None:
                mime_type, _, _ = content_type.partition(";")
                if mime_type == 'application/json':
                    try:
                        resp.read()
                        resp.json()
                    except Exception as e:
                        traceback.print_exc()
                        print("httpx {} response not decodable as json '{}' - retrying".format(request.url, e))
                        continue
            break
        return resp

class Conformance(object):
    def __init__(self, api_url_base, api_token, verify_ssl):
        if not api_url_base.endswith('/'):
            api_url_base += "/"
        self.api_url_base = api_url_base
        transport = RetryTransport(verify=verify_ssl)
        self.httpclient = httpx.Client(verify=verify_ssl, transport=transport, timeout=20)
        headers = {'Content-Type': 'application/json'}
        if api_token is not None:
            headers['Authorization'] = 'Bearer {0}'.format(api_token)
        self.httpclient.headers = headers

    async def get_all_test_modules(self):
        """ Returns an array containing a dictionary per test module """
        api_url = '{0}api/runner/available'.format(self.api_url_base)
        response = self.httpclient.get(api_url)

        if response.status_code != 200:
            raise Exception("get_all_test_modules failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def exporthtml(self, plan_id, path):
        for i in range(5):
            api_url = '{0}api/plan/exporthtml/{1}'.format(self.api_url_base, plan_id)
            try:
                with self.httpclient.stream("GET", api_url) as response:
                    if response.status_code != 200:
                        raise Exception("exporthtml failed - HTTP {:d} {}".format(response.status_code, response.content))
                    d = response.headers['content-disposition']
                    local_filename = re.findall("filename=\"(.+)\"", d)[0]
                    full_path = os.path.join(path, local_filename)
                    with open(full_path, 'wb') as f:
                        for chunk in response.iter_bytes():
                            f.write(chunk)
                zip_file = zipfile.ZipFile(full_path)
                ret = zip_file.testzip()
                if ret is not None:
                    raise Exception("exporthtml for {} downloaded corrupt zip file {} - {}".format(plan_id, full_path, str(ret)))
                return full_path
            except Exception as e:
                print("httpx {} exception {} caught - retrying".format(api_url, e))
                await asyncio.sleep(1)
        raise Exception("exporthtml for {} failed even after retries".format(plan_id))

    async def create_certification_package(self, plan_id, conformance_pdf_path, rp_logs_zip_path = None, output_zip_directory = "./"):
        """
        Create a complete certification package zip file which is written
        to the directory specified by the 'output_zip_directory' parameter.
        Calling this function will additionally publish and mark the test plan as immutable.

        :param plan_id:         The plan id for which to create the package.
        :conformance_pdf_path:  The path to the signed Certification of Conformance PDF document.
        :rp_logs_zip_path:      Required for RP tests and is the path to the client logs zip file.
        :output_zip_directory:  The (already existing) directory to which the certification package zip file is written.
        """
        certificationOfConformancePdf = open(conformance_pdf_path, 'rb')
        clientSideData = open(rp_logs_zip_path, 'rb') if rp_logs_zip_path is not None else open(os.devnull, 'rb')
        files = { 'certificationOfConformancePdf': certificationOfConformancePdf, 'clientSideData': clientSideData}
        try:
            with httpx.Client() as multipartClient:
                multipartClient.headers = self.httpclient.headers.copy()
                multipartClient.headers.pop('content-type')
                api_url = '{0}api/plan/{1}/certificationpackage'.format(self.api_url_base, plan_id)

                response = multipartClient.post(api_url, files = files)
                if response.status_code != 200:
                    raise Exception("certificationpackage failed - HTTP {:d} {}".format(response.status_code, response.content))

                d = response.headers['content-disposition']
                local_filename = re.findall('filename="(.+)"', d)[0]
                full_path = os.path.join(output_zip_directory, local_filename)
                with open(full_path, 'wb') as f:
                    for chunk in response.iter_bytes():
                        f.write(chunk)
                print("Certification package zip for plan id {} written to {}".format(plan_id, full_path))
        finally:
            certificationOfConformancePdf.close();
            clientSideData.close();

    async def create_test_plan(self, name, configuration, variant=None):
        api_url = '{0}api/plan'.format(self.api_url_base)
        payload = {'planName': name}
        if variant != None:
            payload['variant'] = json.dumps(variant)
        response = self.httpclient.post(api_url, params=payload, data=configuration)

        if response.status_code != 201:
            raise Exception("create_test_plan failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def create_test(self, test_name, configuration):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name}
        response = self.httpclient.post(api_url, params=payload, data=configuration)

        if response.status_code != 201:
            raise Exception("create_test failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def create_test_from_plan(self, plan_id, test_name):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        response = self.httpclient.post(api_url, params=payload)

        if response.status_code != 201:
            raise Exception("create_test_from_plan failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def create_test_from_plan_with_variant(self, plan_id, test_name, variant):
        api_url = '{0}api/runner'.format(self.api_url_base)
        payload = {'test': test_name, 'plan': plan_id}
        if variant != None:
            payload['variant'] = json.dumps(variant)
        response = self.httpclient.post(api_url, params=payload)

        if response.status_code != 201:
            raise Exception("create_test_from_plan failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def get_module_info(self, module_id):
        api_url = '{0}api/info/{1}'.format(self.api_url_base, module_id)
        response = self.httpclient.get(api_url)

        if response.status_code != 200:
            raise Exception("get_module_info failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def get_test_log(self, module_id):
        api_url = '{0}api/log/{1}'.format(self.api_url_base, module_id)
        response = self.httpclient.get(api_url)

        if response.status_code != 200:
            raise Exception("get_test_log failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

    async def start_test(self, module_id):
        api_url = '{0}api/runner/{1}'.format(self.api_url_base, module_id)
        response = self.httpclient.post(api_url)

        if response.status_code != 200:
            raise Exception("start_test failed - HTTP {:d} {}".format(response.status_code, response.content))
        return response.json()

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

    async def close_client(self):
        self.httpclient.close()
