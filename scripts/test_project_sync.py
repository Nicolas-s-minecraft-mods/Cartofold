"""Tests of the synchronization of the project: python3 -m unittest discover -s scripts -p 'test_*.py'."""
import io
import unittest
import urllib.error

import project_sync

ORG = "Org"


class FakeGitHub:
    """A project and repositories in memory."""

    def __init__(self, repos, items=None, fields=None, missing=()):
        self.repos = repos
        self.items_ = dict(items or {})
        self.fields = fields if fields is not None else {"Estimate": {"id": "F1", "dataType": "NUMBER"}}
        self.missing = set(missing)
        self.added = []
        self.numbers = []
        self.links = []
        self.blockers = {}

    def project(self, org, number):
        return {"id": "P1", "fields": self.fields}

    def items(self, project_id, field):
        return {key: dict(value) for key, value in self.items_.items()}

    def repositories(self, org):
        return list(self.repos)

    def open_issues(self, owner, repo):
        return self.repos[repo]

    def issue(self, owner, repo, number):
        if (repo, number) in self.missing:
            raise urllib.error.HTTPError("url", 404, "Not Found", {}, io.BytesIO(b""))
        return {"id": hash((repo.lower(), number)) & 0xFFFFFF}

    def add_item(self, project_id, content_id):
        self.added.append(content_id)
        return f"I-{content_id}"

    def set_number(self, project_id, item_id, field_id, value):
        self.numbers.append((item_id, field_id, value))

    def blocked_by(self, owner, repo, number):
        return set(self.blockers.get((repo, number), set()))

    def add_blocked_by(self, owner, repo, number, blocking_id):
        self.links.append((repo, number, blocking_id))


def issue(number, body="", node=None, title="Title"):
    return {"number": number, "node_id": node or f"N{number}", "title": title, "body": body}


def run(api, dry_run=False):
    lines = []
    sync = project_sync.Sync(api, ORG, 2, dry_run=dry_run, out=lines.append)
    return sync.run(), lines


class ParseTest(unittest.TestCase):
    def test_estimate_accepts_lists_bold_and_units(self):
        self.assertEqual(6.0, project_sync.estimate("Text\nEstimate: 6"))
        self.assertEqual(6.0, project_sync.estimate("- **Estimate**: 6h"))
        self.assertEqual(1.5, project_sync.estimate("**Estimate:** 1,5 hours"))
        self.assertEqual(12.0, project_sync.estimate("* estimate : 12 h"))

    def test_estimate_needs_its_own_line(self):
        self.assertIsNone(project_sync.estimate("The estimate: 6h is in a sentence."))
        self.assertIsNone(project_sync.estimate(None))
        self.assertIsNone(project_sync.estimate("Estimate: unknown"))

    def test_references_of_each_form(self):
        refs = project_sync.references(
            "#12, Folkhold#597, other/repo#3 and https://github.com/a/b/issues/9, #12 again", ORG, "Cartofold")

        self.assertEqual([(ORG, "Cartofold", 12), (ORG, "Folkhold", 597), ("other", "repo", 3), ("a", "b", 9)], refs)

    def test_dependencies_split_blocked_by_and_blocks(self):
        body = "## Dependencies\n- **Blocked by**: #4\n- Blocks: Folkhold#597, Folkhold#882\nBlocks nothing #7"

        blocked_by, blocks = project_sync.dependencies(body, ORG, "Cartofold")

        self.assertEqual([(ORG, "Cartofold", 4)], blocked_by)
        self.assertEqual([(ORG, "Folkhold", 597), (ORG, "Folkhold", 882)], blocks)


class SyncTest(unittest.TestCase):
    def test_adds_the_missing_issues_and_sets_the_estimates(self):
        api = FakeGitHub({"Cartofold": [issue(1, "Estimate: 5h"), issue(2)]},
                         items={"N2": {"item": "I2", "estimate": None}})

        errors, lines = run(api)

        self.assertEqual(0, errors)
        self.assertEqual(["N1"], api.added)
        self.assertEqual([("I-N1", "F1", 5.0)], api.numbers)
        self.assertIn("Added: Cartofold#1 Title", lines)

    def test_keeps_an_estimate_that_did_not_change(self):
        api = FakeGitHub({"Cartofold": [issue(1, "Estimate: 5")]}, items={"N1": {"item": "I1", "estimate": 5.0}})

        errors, _ = run(api)

        self.assertEqual(0, errors)
        self.assertEqual([], api.added)
        self.assertEqual([], api.numbers)

    def test_links_the_two_directions_once(self):
        cartofold = [issue(123, "Blocks: Folkhold#597"), issue(124, "Blocked by: #123, #124")]
        folkhold = [issue(597, "Blocked by: Cartofold#123", node="F597")]
        api = FakeGitHub({"Cartofold": cartofold, "Folkhold": folkhold})

        errors, _ = run(api)

        blocker = api.issue(ORG, "Cartofold", 123)["id"]
        self.assertEqual(0, errors)
        self.assertEqual([("Folkhold", 597, blocker), ("Cartofold", 124, blocker)], api.links)

    def test_an_existing_dependency_is_not_added_again(self):
        api = FakeGitHub({"Cartofold": [issue(2, "Blocked by: #1")]})
        api.blockers[("Cartofold", 2)] = {api.issue(ORG, "Cartofold", 1)["id"]}

        errors, _ = run(api)

        self.assertEqual(0, errors)
        self.assertEqual([], api.links)

    def test_an_unknown_issue_is_a_warning_and_the_sync_continues(self):
        api = FakeGitHub({"Cartofold": [issue(1, "Blocked by: Folkhold#9\nEstimate: 2"), issue(2, "Estimate: 3")]},
                         missing={("Folkhold", 9)})

        errors, lines = run(api)

        self.assertEqual(1, errors)
        self.assertEqual(2, len(api.numbers))
        self.assertTrue(any(line.startswith("::warning::No dependency") for line in lines))

    def test_dry_run_changes_nothing(self):
        api = FakeGitHub({"Cartofold": [issue(1, "Estimate: 4\nBlocked by: #2"), issue(2)]})

        errors, lines = run(api, dry_run=True)

        self.assertEqual(0, errors)
        self.assertEqual(([], [], []), (api.added, api.numbers, api.links))
        self.assertIn("Estimate: Cartofold#1 4 h.", lines)
        self.assertIn("Dependency: Org/Cartofold#1 blocked by Org/Cartofold#2.", lines)

    def test_no_estimate_field_is_a_warning(self):
        api = FakeGitHub({"Cartofold": [issue(1, "Estimate: 4")]}, fields={})

        errors, _ = run(api)

        self.assertEqual(1, errors)
        self.assertEqual(["N1"], api.added)
        self.assertEqual([], api.numbers)


if __name__ == "__main__":
    unittest.main()
