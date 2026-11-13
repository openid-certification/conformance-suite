#!/usr/bin/env python3
"""Offline tests for branch_status.py: python3 .claude/skills/git-branch-status/test_branch_status.py"""

import io
import os
import subprocess
import sys
import tempfile
import unittest
import urllib.error
import urllib.request

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import branch_status  # noqa: E402
from branch_status import ApiError, Git, GitError, GitLab  # noqa: E402

SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'branch_status.py')


class Repo:
    """A scratch repository with an origin/master ref and no network remote."""

    def __init__(self, path):
        self.path = path
        self.git('init', '-q', '-b', 'master')
        self.git('config', 'user.email', 'test@example.com')
        self.git('config', 'user.name', 'Test')
        self.git('config', 'commit.gpgsign', 'false')
        self.commit('base.txt', 'base\n', 'Initial commit')
        self.publish()

    def git(self, *args, env=None):
        result = subprocess.run(['git', '-C', self.path, *args], capture_output=True, text=True,
                                env={**os.environ, **(env or {})})
        if result.returncode != 0:
            raise AssertionError(f'git {args}: {result.stderr}')
        return result.stdout.strip()

    def commit(self, name, content, subject, date=None):
        full = os.path.join(self.path, name)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, 'w') as f:
            f.write(content)
        self.git('add', '-A')
        self.git('commit', '-q', '-m', subject,
                 env={'GIT_COMMITTER_DATE': date, 'GIT_AUTHOR_DATE': date} if date else None)

    def branch(self, name, filename, content, subject, date=None):
        self.git('checkout', '-q', '-b', name, 'master')
        self.commit(filename, content, subject, date)
        self.git('checkout', '-q', 'master')

    def publish(self):
        self.git('update-ref', 'refs/remotes/origin/master', 'master')

    def classify(self, base='origin/master', since_days=183):
        merged, opened, old = branch_status.classify_branches(Git(self.path), base, since_days)
        return {row[0]: row[1] for row in merged}, {b['branch']: b for b in opened}, old


class ClassifyBranches(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Repo(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_same_subject_is_not_proof_of_merge(self):
        self.repo.commit('a.txt', 'a\n', 'Fix validation')
        self.repo.publish()
        self.repo.git('checkout', '-q', '-b', 'other', 'master~1')
        self.repo.commit('b.txt', 'b\n', 'Fix validation')
        self.repo.git('checkout', '-q', 'master')
        merged, opened, _ = self.repo.classify()
        self.assertNotIn('other', merged)
        self.assertEqual(opened['other']['matched'], 1)

    def test_subject_substring_is_not_a_match(self):
        self.repo.commit('a.txt', 'a\n', 'Add documentation for the frobnicator')
        self.repo.publish()
        self.repo.branch('short', 'c.txt', 'c\n', 'Add doc')
        merged, opened, _ = self.repo.classify()
        self.assertNotIn('short', merged)
        self.assertEqual(opened['short']['matched'], 0)

    def test_cherry_picked_branch_is_merged_even_when_old(self):
        self.repo.branch('rebased', 'z.txt', 'z\n', 'An old change', date='2020-01-01T00:00:00Z')
        self.repo.git('cherry-pick', 'rebased')
        self.repo.publish()
        merged, opened, old = self.repo.classify()
        self.assertIn('changes already on origin/master', merged['rebased'])
        self.assertEqual((opened, old), ({}, 0))

    def test_squash_merged_branch_is_merged(self):
        self.repo.git('checkout', '-q', '-b', 'feature')
        self.repo.commit('one.txt', '1\n', 'First half')
        self.repo.commit('two.txt', '2\n', 'Second half')
        self.repo.git('checkout', '-q', 'master')
        self.repo.git('merge', '-q', '--squash', 'feature')
        self.repo.git('commit', '-q', '-m', 'Feature (squashed)')
        self.repo.publish()
        merged, _, _ = self.repo.classify()
        self.assertIn('2 commits', merged['feature'])

    def test_old_unmerged_branch_is_hidden_not_merged(self):
        self.repo.branch('stale', 's.txt', 's\n', 'Stale work', date='2020-01-01T00:00:00Z')
        merged, opened, old = self.repo.classify()
        self.assertEqual((merged, opened, old), ({}, {}, 1))

    def test_ancestor_and_conflicting_branches(self):
        self.repo.git('branch', 'done')
        self.repo.branch('clash', 'base.txt', 'theirs\n', 'Change base one way')
        self.repo.commit('base.txt', 'ours\n', 'Change base another way')
        self.repo.publish()
        merged, opened, _ = self.repo.classify()
        self.assertEqual(merged['done'], 'ancestor of origin/master')
        self.assertEqual(opened['clash']['conflict'], 'yes')

    def test_branch_named_like_a_path(self):
        self.repo.commit('docs/readme.txt', 'r\n', 'Add docs')
        self.repo.publish()
        self.repo.branch('docs', 'docs/more.txt', 'm\n', 'More docs')
        _, opened, _ = self.repo.classify()
        self.assertEqual(opened['docs']['total'], 1)

    def test_base_with_a_slash_in_its_name(self):
        self.repo.git('branch', 'release/v1')
        self.repo.git('update-ref', 'refs/remotes/origin/release/v1', 'master')
        self.repo.git('remote', 'add', 'origin', 'https://gitlab.example/group/project.git')
        self.repo.branch('v1', 'v.txt', 'v\n', 'Unrelated work called v1')
        for base in ('release/v1', 'origin/release/v1'):
            merged, opened, _ = self.repo.classify(base=base)
            self.assertNotIn('release/v1', merged, base)
            self.assertIn('v1', opened, base)
            self.assertEqual(branch_status.base_branch_name(Git(self.repo.path), base), 'release/v1')
        self.assertIsNone(branch_status.base_branch_name(Git(self.repo.path), self.repo.git('rev-parse', 'master')))

    def test_invalid_base_is_an_error_not_an_empty_result(self):
        self.repo.branch('work', 'w.txt', 'w\n', 'Unmerged work')
        with self.assertRaises(GitError):
            self.repo.classify(base='origin/typo')
        run = subprocess.run([sys.executable, SCRIPT, '--no-fetch', '--base', 'origin/typo'], cwd=self.repo.path,
                             capture_output=True, text=True)
        self.assertNotEqual(run.returncode, 0)
        self.assertIn('origin/typo is not a commit', run.stderr)
        self.assertNotIn('safe to delete', run.stdout)


class StubOpener:
    def __init__(self, *outcomes):
        self.outcomes = list(outcomes)

    def open(self, req, timeout=None):
        outcome = self.outcomes.pop(0)
        if isinstance(outcome, Exception):
            raise outcome
        return io.BytesIO(outcome)


def http_error(code, body=b'{"message":"nope"}'):
    return urllib.error.HTTPError('https://gitlab.example/x', code, 'error', {}, io.BytesIO(body))


class GitLabClient(unittest.TestCase):
    def client(self, *outcomes):
        gitlab = GitLab('gitlab.example', 'group/project', 'secret')
        gitlab.opener = StubOpener(*outcomes)
        return gitlab

    def test_collection_404_is_an_error(self):
        with self.assertRaisesRegex(ApiError, 'HTTP 404.*group/project'):
            self.client(http_error(404)).get_all('/merge_requests?state=opened')

    def test_single_resource_404_can_be_expected(self):
        self.assertIsNone(self.client(http_error(404)).try_get('/repository/branches/gone'))

    def test_refused_single_resource_is_still_an_error(self):
        with self.assertRaisesRegex(ApiError, 'HTTP 403'):
            self.client(http_error(403)).try_get('/repository/branches/x')

    def test_403_reports_gitlab_message(self):
        with self.assertRaisesRegex(ApiError, 'HTTP 403.*nope'):
            self.client(http_error(403)).get('/environments')

    def test_pagination(self):
        page = ('[' + ','.join(['1'] * 100) + ']').encode()
        self.assertEqual(len(self.client(page, b'[1,2]').get_all('/x')), 102)

    def test_redirect_to_another_origin_is_refused(self):
        handler = branch_status.SameOriginRedirects()
        req = urllib.request.Request('https://gitlab.example/api/v4/user', headers={'PRIVATE-TOKEN': 'secret'})
        for target in ('https://evil.example/api/v4/user', 'http://gitlab.example/api/v4/user'):
            with self.assertRaisesRegex(ApiError, 'refused a redirect'):
                handler.redirect_request(req, None, 302, 'Found', {}, target)
        self.assertIsNotNone(handler.redirect_request(req, None, 302, 'Found', {}, 'https://gitlab.example/other'))


class JobsStub:
    def __init__(self, statuses):
        # A trailing '?' marks an allow_failure job, e.g. 'manual?' for an optional manual job.
        self.jobs = [dict(status=s.rstrip('?'), allow_failure=s.endswith('?')) for s in statuses]

    def get_all(self, path):
        return self.jobs


class PipelineLabel(unittest.TestCase):
    def label(self, *statuses):
        return branch_status.pipeline_label(JobsStub(statuses), dict(id=1, status='running'))

    def test_unfinished_jobs_keep_it_running(self):
        for state in ('created', 'pending', 'preparing', 'waiting_for_resource', 'scheduled', 'running'):
            self.assertEqual(self.label('success', state), 'running', state)

    def test_only_manual_jobs_left(self):
        self.assertEqual(self.label('success', 'manual?', 'skipped'), 'passed*')
        self.assertEqual(self.label('success', 'manual?', 'failed?'), 'warnings*')
        self.assertEqual(self.label('success', 'manual?', 'failed'), 'failed*')

    def test_canceled_or_blocking_manual_jobs_are_not_a_pass(self):
        self.assertEqual(self.label('success', 'canceled'), 'canceled*')
        self.assertEqual(self.label('success', 'manual'), 'manual*')
        self.assertEqual(self.label('failed', 'canceled', 'manual'), 'failed*')

    def test_latest_pipeline_is_for_the_given_commit(self):
        asked = []

        class Stub:
            def get(self, path):
                asked.append(path)
                return []

        self.assertEqual(branch_status.latest_pipeline(Stub(), 'fix/x', 'abc123'), ('none', '-'))
        self.assertIn('sha=abc123', asked[0])
        self.assertIn('ref=fix%2Fx', asked[0])

    def test_skipped_pipeline_does_not_hide_one_that_ran(self):
        class Stub:
            def get(self, path):
                if path.startswith('/pipelines?'):
                    return [dict(id=2, status='skipped'), dict(id=1, status='success')]
                return dict(id=int(path.rsplit('/', 1)[1]), status='success' if path.endswith('/1') else 'skipped',
                            user=dict(username='me'))

        self.assertEqual(branch_status.latest_pipeline(Stub(), 'b', 'abc'), ('passed', 'me'))


def note(author, created, **extra):
    return dict(author=dict(username=author), created_at=created, **extra)


class WaitingThreads(unittest.TestCase):
    def test_reminder_does_not_reset_the_wait(self):
        thread = dict(notes=[note('me', '2026-01-01T00:00:00Z'), note('rev', '2026-02-01T00:00:00Z'),
                             note('rev', '2026-03-01T00:00:00Z')])
        self.assertEqual(branch_status.waiting_threads([thread], 'me'), [(['rev'], '2026-02-01T00:00:00Z')])

    def test_answered_and_resolved_threads_do_not_wait(self):
        answered = dict(notes=[note('rev', '2026-01-01T00:00:00Z'), note('me', '2026-01-02T00:00:00Z')])
        resolved = dict(notes=[note('rev', '2026-01-03T00:00:00Z', resolvable=True, resolved=True)])
        system = dict(notes=[note('rev', '2026-01-04T00:00:00Z', system=True)])
        self.assertEqual(branch_status.waiting_threads([answered, resolved, system], 'me'), [])

    def test_plain_comment_is_answered_by_a_later_note_of_mine(self):
        asked = dict(individual_note=True, notes=[note('rev', '2026-01-01T00:00:00Z')])
        reply = dict(individual_note=True, notes=[note('me', '2026-01-02T00:00:00Z')])
        later = dict(individual_note=True, notes=[note('rev', '2026-01-03T00:00:00Z')])
        self.assertEqual(branch_status.waiting_threads([asked, reply], 'me'), [])
        self.assertEqual(branch_status.waiting_threads([asked, reply, later], 'me'), [(['rev'], '2026-01-03T00:00:00Z')])
        self.assertEqual(len(branch_status.waiting_threads([asked], 'me')), 1)


class Helpers(unittest.TestCase):
    def test_parse_remote(self):
        expected = ('gitlab.com', 'openid/conformance-suite')
        for url in ('git@gitlab.com:openid/conformance-suite.git', 'https://gitlab.com/openid/conformance-suite',
                    'https://user:pass@gitlab.com/openid/conformance-suite.git/',
                    'ssh://git@gitlab.com:2222/openid/conformance-suite.git'):
            self.assertEqual(branch_status.parse_remote(url), expected, url)

    def test_fork_mrs_are_not_matched_by_branch_name(self):
        own = dict(source_branch='collision', source_project_id=1, target_project_id=1, iid=1)
        fork = dict(source_branch='collision', source_project_id=2, target_project_id=1, iid=2)
        self.assertEqual(branch_status.same_project_mrs([fork]), {})
        self.assertEqual(branch_status.same_project_mrs([fork, own])['collision']['iid'], 1)

    def test_clean(self):
        self.assertEqual(branch_status.clean('a`b[c]\nd'), "a'b(c) d")
        self.assertEqual(len(branch_status.clean('x' * 500)), 100)


if __name__ == '__main__':
    unittest.main()
