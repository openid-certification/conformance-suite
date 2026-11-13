#!/usr/bin/env python3
"""Local branch, merge request and review-environment status for a GitLab project.

Run from inside a checkout. Prints three sections as markdown with fixed-width
tables: branch analysis, my open MRs with comments awaiting my reply, and
review environments. `--mr-stats SINCE` prints merged-MR statistics instead.

Standard library only. See SKILL.md next to this file for setup.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone

PROTECTED = {'master', 'main', 'demo', 'production'}
SETTLED_JOB_STATES = {'success', 'failed', 'canceled', 'skipped', 'manual'}
NOW = datetime.now(timezone.utc)


class ApiError(Exception):
    """A GitLab call that failed for a reason the user has to fix (e.g. a missing permission)."""


class GitError(Exception):
    """A git command that failed. Never read a failed command's empty output as 'nothing found'."""


class Git:
    def __init__(self, repo):
        self.repo = repo

    def run(self, *args):
        return subprocess.run(['git', '-C', self.repo, *args], capture_output=True, text=True)

    def out(self, *args, check=True):
        result = self.run(*args)
        if check and result.returncode != 0:
            raise GitError(f"`git {' '.join(args)}` failed: {result.stderr.strip()[:300]}")
        return result.stdout.strip()

    def lines(self, *args):
        return [line for line in self.out(*args).splitlines() if line]


class SameOriginRedirects(urllib.request.HTTPRedirectHandler):
    """urllib re-sends every header, PRIVATE-TOKEN included, to wherever a redirect points."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        old, new = urllib.parse.urlsplit(req.full_url), urllib.parse.urlsplit(newurl)
        if new.scheme != 'https' or new.netloc.lower() != old.netloc.lower():
            raise ApiError(f"refused a redirect from {old.netloc} to {clean(newurl)}: "
                           f"the token is only sent to https://{old.netloc}")
        return super().redirect_request(req, fp, code, msg, headers, newurl)


class GitLab:
    def __init__(self, host, project, token):
        self.project = project
        self.token = token
        self.root = f"https://{host}/api/v4"
        self.base = f"{self.root}/projects/{urllib.parse.quote(project, safe='')}"
        self.opener = urllib.request.build_opener(SameOriginRedirects)

    def get(self, path, project_scoped=True, missing_ok=False):
        """Returns the decoded JSON. A 404 raises ApiError like any other failure, unless
        missing_ok says the one resource asked for may legitimately not exist (then None)."""
        url = (self.base if project_scoped else self.root) + path
        req = urllib.request.Request(url, headers={'PRIVATE-TOKEN': self.token})
        error = None
        for attempt in range(3):
            if attempt:
                time.sleep(attempt)
            try:
                with self.opener.open(req, timeout=30) as response:
                    return json.load(response)
            except urllib.error.HTTPError as e:
                if e.code == 404 and missing_ok:
                    return None
                body = e.read().decode('utf-8', 'replace')
                try:
                    parsed = json.loads(body)
                    detail = parsed.get('error_description') or parsed.get('message') or body
                except (ValueError, AttributeError):
                    detail = body
                error = f"HTTP {e.code} on {path.split('?')[0] or 'the project'}: {str(detail)[:300]}"
                if e.code == 404:
                    error += f" (no project {self.project} on this host, or the token cannot see it)"
                if e.code in (401, 403, 404):
                    break
            except ApiError:
                raise
            except Exception as e:  # network errors: retry
                error = f"{path.split('?')[0]}: {e}"
        raise ApiError(error)

    def get_all(self, path):
        out, page = [], 1
        sep = '&' if '?' in path else '?'
        while True:
            data = self.get(f"{path}{sep}per_page=100&page={page}")
            out.extend(data)
            if len(data) < 100:
                return out
            page += 1

    def try_get(self, path, default=None):
        """For one resource whose absence is expected, e.g. the branch of an old environment.
        Only a 404 gives the default; a refused call is still an error."""
        found = self.get(path, missing_ok=True)
        return default if found is None else found


def parse_time(value):
    return datetime.fromisoformat(value.replace('Z', '+00:00'))


def iso_date(value):
    try:
        return datetime.fromisoformat(value).replace(tzinfo=timezone.utc)
    except ValueError:
        raise argparse.ArgumentTypeError(f'{value!r} is not a YYYY-MM-DD date')


def quote(ref):
    return urllib.parse.quote(ref, safe='')


def clean(value, limit=100):
    """Text from GitLab (titles, branch and user names) is written by other people and is
    read by an agent: keep it on one line, out of code fences and link syntax, and short."""
    text = re.sub(r'[\x00-\x1f\x7f]+', ' ', str(value)).replace('`', "'")
    text = text.replace('[', '(').replace(']', ')')
    return text if len(text) <= limit else text[:limit - 3] + '...'


def table(headers, rows):
    """A fixed-width table in a code fence; markdown tables wrap mid-cell in terminals."""
    if not rows:
        return '(none)'
    rows = [[clean(cell) for cell in row] for row in rows]
    widths = [max(len(str(h)), *(len(str(r[i])) for r in rows)) for i, h in enumerate(headers)]
    rule = '+' + '+'.join('-' * (w + 2) for w in widths) + '+'

    def fmt(cells):
        return '| ' + ' | '.join(str(c).ljust(widths[i]) for i, c in enumerate(cells)) + ' |'

    return '\n'.join(['```', rule, fmt(headers), rule, *(fmt(r) for r in rows), rule, '```'])


def links(mrs):
    seen, parts = set(), []
    for mr in mrs:
        if mr and mr['iid'] not in seen and re.fullmatch(r'https://[^\s()<>]+', mr['web_url']):
            seen.add(mr['iid'])
            parts.append(f"[!{int(mr['iid'])} {clean(mr['title'], 150)}]({mr['web_url']})")
    return 'Links: ' + ' · '.join(parts) if parts else ''


def parse_remote(url):
    """(host, project path) from an ssh or https remote URL, or None for anything else."""
    m = re.match(r'(?:\w+://)?(?:[^@/]+@)?([^:/]+)(?::\d+(?=/))?[:/](.+?)(?:\.git)?/?$', url)
    return (m.group(1), m.group(2)) if m else None


def read_token():
    token = os.environ.get('GITLAB_TOKEN')
    if token:
        return token.strip()
    try:
        with open(os.path.expanduser('~/.gitlab-token')) as f:
            return f.read().strip()
    except OSError:
        return None


def same_project_mrs(mrs):
    """MRs by source branch, leaving out MRs from forks: a fork's branch only shares a
    name with a branch of this project."""
    by_branch = {}
    for mr in mrs:
        if mr.get('source_project_id') == mr.get('target_project_id'):
            by_branch.setdefault(mr['source_branch'], mr)
    return by_branch


def pipeline_label(gitlab, pipeline):
    """passed / warnings / failed / running / pending / ... from a pipeline DETAIL response.

    Only the detail endpoint carries detailed_status, which is the one way to tell
    'passed' from 'passed with warnings'. A trailing * marks a status derived from the
    jobs: a pipeline whose only unfinished jobs are manual stays 'running' for ever.
    """
    if not pipeline:
        return 'none'
    group = (pipeline.get('detailed_status') or {}).get('group') or ''
    status = pipeline.get('status') or ''
    if group == 'success-with-warnings':
        return 'warnings'
    if status == 'success':
        return 'passed'
    if status == 'running':
        try:
            jobs = gitlab.get_all(f"/pipelines/{pipeline['id']}/jobs")
        except ApiError:
            return 'running'
        # created, pending, preparing, waiting_for_resource, scheduled and running are all unfinished.
        if jobs and all(j['status'] in SETTLED_JOB_STATES for j in jobs):
            def has(state, required):
                return any(j['status'] == state and bool(j.get('allow_failure')) != required for j in jobs)

            if has('failed', required=True):
                return 'failed*'
            if any(j['status'] == 'canceled' for j in jobs):
                return 'canceled*'
            if has('manual', required=True):  # a manual job the pipeline cannot pass without
                return 'manual*'
            return 'warnings*' if has('failed', required=False) else 'passed*'
        return 'running'
    if status in ('pending', 'created', 'scheduled', 'waiting_for_resource', 'preparing'):
        return 'pending'
    if status in ('canceled', 'canceling'):
        return 'canceled'
    return status or '?'


def base_branch_name(git, base):
    """The branch a base ref names ('release/v1' for origin/release/v1 and for a local
    release/v1), or None when it is a tag or a commit id."""
    full = git.out('rev-parse', '--symbolic-full-name', base, check=False)
    if full.startswith('refs/heads/'):
        return full[len('refs/heads/'):]
    for remote in sorted(git.lines('remote'), key=len, reverse=True):
        prefix = f'refs/remotes/{remote}/'
        if full.startswith(prefix):
            return full[len(prefix):]
    return None


def latest_pipeline(gitlab, ref, sha=None):
    """(status label, username that triggered it) for the newest pipeline on ref, for the
    commit sha when given: a retried or manually continued pipeline of an older commit
    must not stand in for the commit the branch is at. A skipped pipeline (a ci.skip
    push) says nothing, so an earlier pipeline that ran for the same commit wins."""
    query = f"/pipelines?ref={quote(ref)}&per_page=20&order_by=id&sort=desc"
    if sha:
        query += f"&sha={quote(sha)}"
    try:
        pipelines = gitlab.get(query)
        if not pipelines:
            return 'none', '-'
        chosen = next((p for p in pipelines if p.get('status') != 'skipped'), pipelines[0])
        detail = gitlab.get(f"/pipelines/{chosen['id']}")
    except ApiError:
        return '?', '-'
    return pipeline_label(gitlab, detail), ((detail or {}).get('user') or {}).get('username', '-')


def find_username(args, git, gitlab, open_mrs):
    """The user's GitLab username and how it was found, or (None, reason)."""
    if args.user:
        return args.user, '--user'
    if os.environ.get('GITLAB_USERNAME'):
        return os.environ['GITLAB_USERNAME'], 'GITLAB_USERNAME'
    configured = git.out('config', 'gitlab.user', check=False)
    if configured:
        return configured, 'git config gitlab.user'
    try:
        user = gitlab.get('/user', project_scoped=False)
        if user and user.get('username'):
            return user['username'], 'the token'
    except ApiError:
        pass
    # Fine-grained tokens often lack "User: Read"; fall back to the display name on MRs.
    name = git.out('config', 'user.name', check=False)
    if name:
        candidates = list(open_mrs)
        try:
            candidates += gitlab.get('/merge_requests?state=merged&per_page=100&order_by=updated_at')
        except ApiError:
            pass
        usernames = {m['author']['username'] for m in candidates if m['author'].get('name') == name}
        if len(usernames) == 1:
            return usernames.pop(), f"MR author named '{name}'"
    return None, ("could not determine your GitLab username: set GITLAB_USERNAME, run "
                  "`git config --global gitlab.user <username>`, or give the token 'User: Read'")


def classify_branches(git, base, since_days):
    """(merged rows, open branches, number hidden as old).

    A branch is merged only on evidence from the commits themselves: its tip is an
    ancestor of the base, or merging it into the base leaves the base's tree unchanged
    (which covers squash and rebase merges). Commit subjects prove nothing: unrelated
    commits share them, and an amended commit keeps its subject.
    """
    base_sha = git.out('rev-parse', '--verify', f'{base}^{{commit}}')
    base_tree = git.out('rev-parse', f'{base_sha}^{{tree}}')
    base_name = base_branch_name(git, base)
    base_subjects = set(git.lines('log', base_sha, '--format=%s'))
    ancestors = set(git.lines('for-each-ref', '--merged', base_sha, '--format=%(refname:lstrip=2)', 'refs/heads'))
    checked_out = {line[len('branch refs/heads/'):] for line in git.lines('worktree', 'list', '--porcelain')
                   if line.startswith('branch refs/heads/')}
    cutoff = NOW - timedelta(days=since_days)
    branches = [line.split('\t') for line in git.lines(
        'for-each-ref', '--format=%(refname:lstrip=2)%09%(committerdate:iso-strict)', 'refs/heads')]

    def examine(entry):
        branch, tip = entry
        # Full ref names throughout: a short name can also be a tag or a path in the worktree.
        head = f'refs/heads/{branch}'
        day = tip[:10]
        in_use = 'yes' if branch in checked_out else ''
        if branch in ancestors:
            return 'merged', [branch, f'ancestor of {base}', day, in_use]
        # merge-tree is a dry run: it touches neither the index nor any worktree.
        merge = git.run('merge-tree', '--write-tree', base_sha, head)
        total = int(git.out('rev-list', '--count', f'{base_sha}..{head}'))
        if merge.returncode == 0 and merge.stdout.split('\n', 1)[0].strip() == base_tree:
            return 'merged', [branch, f'changes already on {base} ({total} commits)', day, in_use]
        if parse_time(tip) < cutoff:
            return 'old', None
        subjects = git.lines('log', f'{base_sha}..{head}', '--format=%s', '--no-merges')
        return 'open', dict(branch=branch, total=total, day=day,
                            matched=sum(1 for subject in subjects if subject in base_subjects),
                            conflict={0: 'no', 1: 'yes'}.get(merge.returncode, 'error'))

    wanted = [b for b in branches if b[0] not in PROTECTED and b[0] != base_name]
    with ThreadPoolExecutor(8) as pool:
        results = list(pool.map(examine, wanted))
    merged_rows = sorted((r for kind, r in results if kind == 'merged'), key=lambda r: r[2], reverse=True)
    return merged_rows, [r for kind, r in results if kind == 'open'], sum(kind == 'old' for kind, _ in results)


def branch_analysis(args, git, gitlab, base, open_mrs, api_error):
    print('## 1. Branch analysis\n')
    if api_error:
        print(f"GitLab unavailable ({api_error}); MR and pipeline columns are empty.\n")
    mr_by_branch = same_project_mrs(open_mrs)
    merged_rows, candidates, skipped_old = classify_branches(git, base, args.since_days)
    base_sha = git.out('rev-parse', '--verify', f'{base}^{{commit}}')
    remotes = set(git.lines('for-each-ref', '--format=%(refname:lstrip=3)', 'refs/remotes/origin'))

    def enrich(candidate):
        branch, total, matched = candidate['branch'], candidate['total'], candidate['matched']
        conflict, behind = candidate['conflict'], '0'
        if branch in remotes:
            remote = f'refs/remotes/origin/{branch}'
            ahead, behind = git.out('rev-list', '--left-right', '--count', f'refs/heads/{branch}...{remote}').split()
            vs_origin = 'in sync' if (ahead, behind) == ('0', '0') else f'+{ahead}/-{behind}'
            pipeline = '?'
            if gitlab and not api_error:
                pipeline = latest_pipeline(gitlab, branch, git.out('rev-parse', remote))[0]
        else:
            vs_origin, pipeline = 'no remote', 'local'
        if behind != '0':
            code = git.run('merge-tree', '--write-tree', base_sha, f'refs/remotes/origin/{branch}').returncode
            remote_conflict = {0: 'no', 1: 'yes'}.get(code, 'error')
            if remote_conflict != conflict:
                conflict = f'{conflict} (origin: {remote_conflict})'
        mr = mr_by_branch.get(branch)
        commits = f"{total} ({matched} subjects on {base})" if matched else str(total)
        return dict(mr=mr, matched=matched, day=candidate['day'], row=[
            branch, commits, candidate['day'], vs_origin, pipeline, conflict,
            f"!{mr['iid']}" if mr else '-', mr['author']['username'] if mr else '-',
            'draft' if mr and mr.get('draft') else ''])

    with ThreadPoolExecutor(8) as pool:
        rows = sorted(pool.map(enrich, candidates), key=lambda r: r['day'], reverse=True)

    print(f'**Merged, safe to delete ({len(merged_rows)})**')
    print(table(['branch', 'how', 'last commit', 'in a worktree'], merged_rows))
    headers = ['branch', 'commits', 'last commit', 'vs origin', 'pipeline', 'conflicts', 'MR', 'MR author', 'draft']
    groups = [
        ('Unmerged with open MR', [r for r in rows if r['mr']]),
        ('Possibly part-merged (some commit subjects also on the base), no MR',
         [r for r in rows if not r['mr'] and r['matched']]),
        ('Unmerged, no MR', [r for r in rows if not r['mr'] and not r['matched']]),
    ]
    for title, group in groups:
        print(f'\n**{title} ({len(group)})**')
        print(table(headers, [r['row'] for r in group]))
        link_line = links(r['mr'] for r in group)
        if link_line:
            print(link_line)
    cutoff = (NOW - timedelta(days=args.since_days)).date()
    print(f'\nNot listed: {skipped_old} unmerged branches whose tip is older than {cutoff} '
          f'(--since-days {args.since_days}). `*` on a pipeline = derived from its finished jobs.')


def waiting_threads(discussions, username):
    """[(authors awaiting a reply, created_at of the oldest unanswered note)], one per thread.

    In a thread the unanswered notes are those after the user's last note. A plain MR
    comment cannot be replied to in place (each is a discussion of its own), so it counts
    as answered once the user has written anything on the MR after it.
    """
    def human(discussion):
        return [n for n in discussion['notes'] if not n.get('system')]

    mine = [parse_time(n['created_at']) for d in discussions for n in human(d) if n['author']['username'] == username]
    my_latest = max(mine, default=None)
    waiting = []
    for discussion in discussions:
        notes = human(discussion)
        if not notes or (notes[0].get('resolvable') and notes[0].get('resolved')):
            continue
        if discussion.get('individual_note'):
            unanswered = [n for n in notes if n['author']['username'] != username
                          and (my_latest is None or parse_time(n['created_at']) > my_latest)]
        else:
            last_mine = max((i for i, n in enumerate(notes) if n['author']['username'] == username), default=-1)
            unanswered = notes[last_mine + 1:]
        if unanswered:
            waiting.append((sorted({n['author']['username'] for n in unanswered}),
                            min(n['created_at'] for n in unanswered)))
    return waiting


def unanswered_comments(gitlab, username, username_note, open_mrs):
    print('\n## 2. Unanswered MR comments\n')
    if not username:
        print(f'Unavailable: {username_note}.')
        return
    mine = [mr for mr in open_mrs if mr['author']['username'] == username]

    def check(mr):
        return mr, waiting_threads(gitlab.get_all(f"/merge_requests/{mr['iid']}/discussions"), username)

    rows = []
    with ThreadPoolExecutor(8) as pool:
        for mr, waiting in pool.map(check, mine):
            if waiting:
                days = (NOW - min(parse_time(w[1]) for w in waiting)).days
                rows.append((days, mr, [
                    f"!{mr['iid']}", mr['source_branch'], len(waiting),
                    ', '.join(sorted({author for w in waiting for author in w[0]})), f'{days}d',
                    'draft' if mr.get('draft') else '']))
    rows.sort(key=lambda r: -r[0])
    print(f'**Open MRs by {clean(username)} with comments awaiting a reply ({len(rows)} of {len(mine)} MRs)**')
    if rows:
        print(table(['MR', 'branch', 'threads', 'from', 'waiting', 'draft'], [r[2] for r in rows]))
        print(links(r[1] for r in rows))


def review_environments(git, gitlab, username, open_mrs, ingress_namespaces):
    print('\n## 3. Review environments\n')
    environments = [e for e in gitlab.get_all('/environments?states=available') if e['name'].startswith('review/')]
    mr_by_branch = same_project_mrs(open_mrs)
    git_name = git.out('config', 'user.name', check=False)

    def describe(env):
        branch = env['name'][len('review/'):]
        info = gitlab.try_get(f'/repository/branches/{quote(branch)}')
        commit = (info or {}).get('commit') or {}
        last_commit = commit.get('committed_date', '-')[:10]
        author = commit.get('author_name', '-')
        pipeline, trigger = latest_pipeline(gitlab, branch, commit.get('id'))
        mr = mr_by_branch.get(branch)
        updated = env['updated_at'][:10]
        age = (NOW - parse_time(env['updated_at'])).days
        flag = ''
        if (age > 28 and not mr) or age > 56:
            flag = 'RED'
        elif last_commit != '-':
            deploy_lag = (datetime.fromisoformat(last_commit) - datetime.fromisoformat(updated)).days
            idle = (NOW.date() - datetime.fromisoformat(last_commit).date()).days
            if deploy_lag >= 5 or (mr and idle >= 14):
                flag = 'YEL'
        mine = author == git_name or (username and username in (trigger, mr['author']['username'] if mr else None))
        return dict(flag=flag, updated=updated, mine=bool(mine), mr=mr, slug=env.get('slug'), name=env['name'], row=[
            flag, branch, last_commit, author, updated, pipeline, trigger,
            f"!{mr['iid']}" if mr else '-', mr['author']['username'] if mr else '-'])

    with ThreadPoolExecutor(8) as pool:
        rows = list(pool.map(describe, environments))
    headers = ['!', 'branch', 'last commit', 'commit author', 'env updated', 'pipeline', 'pipe trig', 'MR', 'MR author']
    order = {'RED': 0, 'YEL': 1, '': 2}
    for title, mine in (('My review environments', True), ("Everyone else's review environments", False)):
        group = sorted((r for r in rows if r['mine'] == mine), key=lambda r: r['updated'], reverse=True)
        group.sort(key=lambda r: order[r['flag']])
        red = sum(r['flag'] == 'RED' for r in group)
        yellow = sum(r['flag'] == 'YEL' for r in group)
        print(f'**{title} ({len(group)}: {red} RED, {yellow} YEL)**')
        print(table(headers, [r['row'] for r in group]))
        link_line = links(r['mr'] for r in group)
        if link_line:
            print(link_line)
        print()

    if ingress_namespaces is not None:
        project = gitlab.get('')
        prefix = f"{project['path'].lower()}-{project['id']}-"
        cluster = {ns[len(prefix):] for ns in ingress_namespaces if ns.startswith(prefix)}
        cluster = {slug for slug in cluster if slug.startswith('review-')}
        known = {r['slug'] for r in rows}
        print('**Cluster namespaces with an ingress but no available GitLab environment (orphans)**')
        print('\n'.join(f'- {clean(prefix + slug)}' for slug in sorted(cluster - known)) or '(none)')
        print('\n**Available GitLab environments with no matching ingress in the file**')
        print('\n'.join(f"- {clean(r['name'])} (slug {clean(r['slug'])}, env updated {r['updated']})"
                        for r in sorted(rows, key=lambda r: r['updated']) if r['slug'] not in cluster) or '(none)')


def mr_stats(git, gitlab, start, target):
    """MRs merged into target per author and quarter. Line counts come from local git:
    the GitLab diff API silently truncates large diffs and undercounts."""
    since = start.isoformat().replace('+00:00', 'Z')
    merged = [mr for mr in gitlab.get_all(
                  f"/merge_requests?state=merged&target_branch={quote(target)}&updated_after={since}")
              if mr.get('merged_at') and parse_time(mr['merged_at']) >= start]

    def measure(mr):
        refs = (gitlab.try_get(f"/merge_requests/{mr['iid']}") or {}).get('diff_refs') or {}
        result = git.run('diff', '--shortstat', str(refs.get('base_sha')), str(refs.get('head_sha')))
        if result.returncode != 0:
            return mr, None

        def count(word):
            m = re.search(rf'(\d+) {word}', result.stdout)
            return int(m.group(1)) if m else 0

        return mr, (count('insertion'), count('deletion'), count('file'))

    stats, unmeasured = {}, 0
    with ThreadPoolExecutor(8) as pool:
        for mr, counts in pool.map(measure, merged):
            when = parse_time(mr['merged_at'])
            key = (f'{when.year} Q{(when.month - 1) // 3 + 1}', mr['author']['username'])
            entry = stats.setdefault(key, [0, 0, 0, 0])
            entry[0] += 1
            if counts is None:
                unmeasured += 1
            else:
                for i, value in enumerate(counts):
                    entry[i + 1] += value
    print(f'## MRs merged into {clean(target)} since {start.date()} ({len(merged)})\n')
    for quarter in sorted({key[0] for key in stats}):
        rows = sorted(([user, *values] for (q, user), values in stats.items() if q == quarter), key=lambda r: -r[1])
        print(f'**{quarter}**')
        print(table(['author', 'MRs', 'added', 'removed', 'files'], rows))
        print()
    if unmeasured:
        print(f'{unmeasured} MRs are counted without line totals: their head commits are not in this clone. '
              "That is usual for squash-merged MRs, whose commits exist only on GitLab's "
              'refs/merge-requests/<iid>/head.')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--base', help='ref branches are compared against (default origin/master, else origin/main)')
    parser.add_argument('--since-days', type=int, default=183, help='hide unmerged branches idle for longer (default 183)')
    parser.add_argument('--user', help='your GitLab username (default: detected, see SKILL.md)')
    parser.add_argument('--project', help='GitLab project path, e.g. group/project (default: from the origin remote)')
    parser.add_argument('--no-fetch', action='store_true', help='skip `git fetch origin --prune`')
    parser.add_argument('--ingress-file', help='file holding `kubectl get ing -A` output, for orphan detection')
    parser.add_argument('--mr-stats', metavar='SINCE', type=iso_date,
                        help='print merged-MR statistics since YYYY-MM-DD instead')
    args = parser.parse_args()

    top = subprocess.run(['git', 'rev-parse', '--show-toplevel'], capture_output=True, text=True)
    if top.returncode != 0:
        sys.exit('Not inside a git repository')
    git = Git(top.stdout.strip())
    ingress_namespaces = None
    if args.ingress_file:
        try:
            with open(args.ingress_file) as f:
                ingress_namespaces = {line.split('\t')[0].strip() for line in f if line.strip()}
        except OSError as e:
            sys.exit(f'Cannot read --ingress-file: {e}')
    if not args.no_fetch:
        fetch = git.run('fetch', 'origin', '--prune')
        if fetch.returncode != 0:
            print(f'WARNING: git fetch failed, results use stale remote refs: {fetch.stderr.strip()[:200]}\n')
    base = args.base or next((ref for ref in ('origin/master', 'origin/main')
                              if git.run('show-ref', '--verify', '--quiet', f'refs/remotes/{ref}').returncode == 0), None)
    if not base:
        sys.exit('No origin/master or origin/main; pass --base')
    if git.run('rev-parse', '--verify', '--quiet', f'{base}^{{commit}}').returncode != 0:
        sys.exit(f'--base {base} is not a commit in this repository')

    token = read_token()
    # The token is only ever sent to the host it was issued for, not to whatever host
    # the origin remote of the current clone happens to name.
    token_host = os.environ.get('GITLAB_HOST') or git.out('config', 'gitlab.host', check=False) or 'gitlab.com'
    url = git.out('remote', 'get-url', 'origin', check=False)
    remote = parse_remote(url)
    if remote:
        host, project = remote[0], args.project or remote[1]
    elif args.project:
        host, project = token_host, args.project
    else:
        sys.exit(f'Cannot work out the GitLab project from remote URL {url!r}; pass --project '
                 f'(the host is then {token_host}, from GITLAB_HOST or `git config gitlab.host`)')
    host_refused = None
    if token and host.lower() != token_host.lower():
        host_refused = (f"origin is on {clean(host)} but the token is for {clean(token_host)}; set GITLAB_HOST or "
                        f"`git config gitlab.host` to {clean(host)} if the token belongs to that host")
        token = None
    gitlab = GitLab(host, project, token) if token else None
    if args.mr_stats:
        if not gitlab:
            sys.exit(host_refused or '--mr-stats needs a GitLab token (GITLAB_TOKEN or ~/.gitlab-token)')
        try:
            target = base_branch_name(git, base)
            if not target:
                sys.exit(f'--mr-stats needs a branch to count merges into, and --base {base} is not one')
            mr_stats(git, gitlab, args.mr_stats, target)
        except ApiError as e:
            sys.exit(f'GitLab unavailable: {e}')
        return

    open_mrs, api_error = [], None
    if gitlab:
        try:
            open_mrs = gitlab.get_all('/merge_requests?state=opened')
        except ApiError as e:
            api_error = str(e)
    else:
        api_error = host_refused or 'no token in GITLAB_TOKEN or ~/.gitlab-token'

    branch_analysis(args, git, gitlab, base, open_mrs, api_error)
    if api_error:
        print(f'\n## 2. Unanswered MR comments\n\nUnavailable: {api_error}.')
        print(f'\n## 3. Review environments\n\nUnavailable: {api_error}.')
        return
    username, note = find_username(args, git, gitlab, open_mrs)
    try:
        unanswered_comments(gitlab, username, note, open_mrs)
    except ApiError as e:
        print(f'Unavailable: {e}.')
    try:
        review_environments(git, gitlab, username, open_mrs, ingress_namespaces)
    except ApiError as e:
        print(f'Unavailable: {e}.')
    if username:
        print(f'GitLab username {clean(username)} (from {clean(note)}).')


if __name__ == '__main__':
    try:
        main()
    except GitError as e:
        sys.exit(str(e))
