"""Unit tests of the backfill (standard library only): python3 -m unittest discover -s . -p 'test_*.py'"""

import contextlib
import os
import tempfile
import unittest
from datetime import datetime, timedelta, timezone

import backfill_release_timestamps as cli
from release_inference import (SOURCE_EXACT_COMMIT, SOURCE_EXACT_MTIME, SOURCE_EXACT_PAGE, SOURCE_INFERRED_CHANGED, SOURCE_INFERRED_UNCHANGED,
                               SOURCE_SKIPPED, SOURCE_UNRESOLVED, compare_data_set, compare_gallery, compare_site_file, decide_page,
                               gallery_release_from_file_mtimes, infer_from_comparison, normalize_cell, site_file_path, to_naive_utc)

CREATED = datetime(2026, 1, 1, 10, 0)
MODIFIED = datetime(2026, 1, 3, 10, 0)
RELEASED = datetime(2026, 1, 2, 10, 0)


class InferenceTests(unittest.TestCase):

    def test_unchanged_copies_are_released_at_the_last_modification(self):
        decision = infer_from_comparison("gallery", "1", [], CREATED, MODIFIED)
        self.assertEqual((MODIFIED, SOURCE_INFERRED_UNCHANGED), (decision.published, decision.source))
        never_modified = infer_from_comparison("gallery", "1", [], CREATED, None)
        self.assertEqual(CREATED, never_modified.published)

    def test_changed_copies_use_the_newest_time_older_than_the_modification(self):
        decision = infer_from_comparison("gallery", "1", ["description"], CREATED, MODIFIED, RELEASED)
        self.assertEqual((RELEASED, SOURCE_INFERRED_CHANGED), (decision.published, decision.source))
        self.assertIn("description", decision.detail)
        without_site_time = infer_from_comparison("gallery", "1", ["description"], CREATED, MODIFIED)
        self.assertEqual(CREATED, without_site_time.published)
        # A site timestamp newer than the modification cannot be the release of the stale copy
        newer_site_time = infer_from_comparison("gallery", "1", ["description"], CREATED, MODIFIED, MODIFIED + timedelta(hours=1))
        self.assertEqual(CREATED, newer_site_time.published)

    def test_changed_copies_without_an_older_time_stay_unresolved(self):
        self.assertEqual(SOURCE_UNRESOLVED, infer_from_comparison("site_file", "1", ["metadata"], CREATED, None).source)
        self.assertEqual(SOURCE_UNRESOLVED, infer_from_comparison("site_file", "1", ["metadata"], MODIFIED, MODIFIED).source)
        self.assertIsNone(infer_from_comparison("site_file", "1", ["metadata"], MODIFIED, MODIFIED).published)

    def test_pages_take_the_site_release_time(self):
        self.assertEqual((RELEASED, SOURCE_EXACT_PAGE), (decide_page(1, RELEASED, True).published, decide_page(1, RELEASED, True).source))
        self.assertEqual(SOURCE_SKIPPED, decide_page(1, None, False).source)
        self.assertEqual(SOURCE_UNRESOLVED, decide_page(1, None, True).source)

    def test_timestamps_are_normalized_to_naive_utc(self):
        aware = datetime(2026, 1, 2, 12, 0, tzinfo=timezone(timedelta(hours=2)))
        self.assertEqual(datetime(2026, 1, 2, 10, 0), to_naive_utc(aware))
        self.assertEqual(datetime(2026, 1, 2, 10, 0), to_naive_utc(int(aware.timestamp())))
        self.assertEqual(RELEASED, to_naive_utc(RELEASED))
        self.assertIsNone(to_naive_utc(None))


class ComparisonTests(unittest.TestCase):

    def admin_file(self, **overrides):
        file = {"file_type": "IMAGE", "file_path": "2024/trip", "file_name": "a.jpg", "mime_type": "image/jpeg", "sha256sum": "ABC", "comment": "c",
                "metadata": "{}", "width": 10, "height": 20, "length": None, "pages": None, "rights_holder": None, "rights_terms": None,
                "rights_url": None, "creator_name": "Alice", "creator_email": None, "creator_country": None, "creator_url": None, "location_id": 5}
        file.update(overrides)
        return file

    def web_file(self, **overrides):
        file = {"file_path": "image/2024/trip/a.jpg", "mimetype": "image/jpeg", "comment": "c", "metadata": "{}", "width": 10, "height": 20, "length": None,
                "pages": None, "rights_holder": None, "rights_terms": None, "rights_url": None, "creator_name": "Alice", "creator_email": None,
                "creator_country": None, "creator_url": None, "location_id": 5}
        file.update(overrides)
        return file

    def test_site_file_path_is_type_path_and_name(self):
        self.assertEqual("image/2024/trip/a.jpg", site_file_path("IMAGE", "2024/trip", "a.jpg"))
        self.assertEqual("image/2024/trip/a.jpg", site_file_path("image", "/2024/trip/", "a.jpg"))
        self.assertEqual("document/x.pdf", site_file_path("DOCUMENT", "", "x.pdf"))

    def test_site_file_comparison_names_the_differing_fields(self):
        self.assertEqual([], compare_site_file(self.admin_file(), self.web_file()))
        self.assertEqual([], compare_site_file(self.admin_file(), self.web_file(), "abc"))
        self.assertEqual(["sha256sum"], compare_site_file(self.admin_file(), self.web_file(), "other"))
        self.assertEqual(["file_path", "metadata", "width"],
                         compare_site_file(self.admin_file(file_name="b.jpg", metadata="{\"a\":1}", width=11), self.web_file()))
        self.assertEqual(["comment"], compare_site_file(self.admin_file(comment=None), self.web_file(comment="x")))

    def test_gallery_comparison_covers_names_and_the_ordered_file_list(self):
        admin = {"shortname": "trip", "description": "Trip"}
        self.assertEqual([], compare_gallery(admin, [1, 2], {"shortname": "trip", "description": "Trip"}, [1, 2]))
        self.assertEqual(["files"], compare_gallery(admin, [1, 2], {"shortname": "trip", "description": "Trip"}, [2, 1]))
        self.assertEqual(["shortname", "description", "files"], compare_gallery(admin, [1], {"shortname": "x", "description": None}, []))

    def test_gallery_release_is_the_oldest_server_timestamp_of_its_files(self):
        mtimes = {"image/a.jpg": RELEASED, "image/b.jpg": RELEASED + timedelta(days=1)}
        self.assertEqual(RELEASED, gallery_release_from_file_mtimes(["image/a.jpg", "image/b.jpg"], mtimes))
        self.assertIsNone(gallery_release_from_file_mtimes(["image/a.jpg", "image/missing.jpg"], mtimes))
        self.assertIsNone(gallery_release_from_file_mtimes([], mtimes))

    def test_data_set_comparison_normalizes_values_and_ignores_row_order(self):
        csv_text = "title,year,price,released\nAbbey Road,1969,10.50,true\nOK Computer,1997,9,false\n"
        rows = [("OK Computer", 1997, 9.0, False), ("Abbey Road", 1969, 10.5, True)]
        self.assertEqual([], compare_data_set(csv_text, ["title", "year", "price", "released"], rows))
        self.assertEqual(["site table missing"], compare_data_set(csv_text, None, None))
        self.assertEqual(["columns"], compare_data_set(csv_text, ["title", "year"], rows))
        self.assertEqual(["row count"], compare_data_set(csv_text, ["title", "year", "price", "released"], rows[:1]))
        self.assertEqual(["content"], compare_data_set(csv_text, ["title", "year", "price", "released"], [("OK Computer", 1997, 9.0, False),
                                                                                                         ("Abbey Road", 1969, 11, True)]))

    def test_cell_normalization(self):
        self.assertEqual("", normalize_cell(None))
        self.assertEqual("", normalize_cell("  "))
        self.assertEqual("10.5", normalize_cell("10.50"))
        self.assertEqual("10.5", normalize_cell(10.5))
        self.assertEqual("0", normalize_cell("0.0"))
        self.assertEqual("true", normalize_cell("TRUE"))
        self.assertEqual("2026-01-02 10:00:00", normalize_cell("2026-01-02T10:00:00"))
        self.assertEqual("2026-01-02 10:00:00", normalize_cell(datetime(2026, 1, 2, 10, 0)))
        self.assertEqual("2026-01-02 10:00:00", normalize_cell(datetime(2026, 1, 2, 12, 0, tzinfo=timezone(timedelta(hours=2)))))
        self.assertEqual("Abbey Road", normalize_cell(" Abbey Road "))


class FakeCursor:
    """A cursor that answers each query from the canned result whose key (a distinctive substring) appears in the SQL."""

    def __init__(self, results):
        self.results = results
        self.rows = []
        self.description = []
        self.executed = []

    def execute(self, sql, params=()):
        self.executed.append((sql, params))
        for key, (columns, rows) in self.results.items():
            if key in sql:
                self.description = [(column,) for column in columns]
                self.rows = rows
                return
        self.description = []
        self.rows = []

    def fetchall(self):
        return self.rows

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


class FakeConnection:

    def __init__(self, results):
        self.results = results
        self.cursors = []
        self.committed = False

    def cursor(self):
        cursor = FakeCursor(self.results)
        self.cursors.append(cursor)
        return cursor

    def commit(self):
        self.committed = True

    def close(self):
        pass

    def executed(self):
        return [sql for cursor in self.cursors for sql, _ in cursor.executed]


class Args:
    verbose = False
    report_out = "report.csv"


class CliTests(unittest.TestCase):

    def test_pages_use_the_site_release_and_skip_set_or_unpublished_pages(self):
        admin = FakeConnection({"FROM page": (["id", "published"], [(1, None), (2, None), (3, RELEASED)])})
        site = FakeConnection({"FROM web_site_page": (["page_id", "published"], [(1, RELEASED)])})
        decisions = cli.backfill_pages(admin, site, overwrite=False)
        self.assertEqual([(RELEASED, SOURCE_EXACT_PAGE), (None, SOURCE_SKIPPED), (None, SOURCE_SKIPPED)],
                         [(decision.published, decision.source) for decision in decisions])
        overwritten = cli.backfill_pages(admin, site, overwrite=True)
        self.assertEqual("not published to the site", overwritten[2].detail)

    def test_files_prefer_server_timestamps_then_commit_timestamps_then_the_comparison(self):
        file_columns = ["id", "file_type", "file_path", "file_name", "mime_type", "sha256sum", "comment", "metadata", "width", "height", "length",
                        "pages", "rights_holder", "rights_terms", "rights_url", "creator_name", "creator_email", "creator_country", "creator_url",
                        "location_id", "created", "modified", "published"]

        def admin_row(file_id, name, modified, metadata="{}"):
            return (file_id, "IMAGE", "2024", name, "image/jpeg", "abc", None, metadata, 1, 1, None, None, None, None, None, None, None, None, None,
                    None, CREATED, modified, None)

        web_columns = ["file_id", "file_path", "mimetype", "comment", "metadata", "width", "height", "length", "pages", "rights_holder", "rights_terms",
                       "rights_url", "creator_name", "creator_email", "creator_country", "creator_url", "location_id", "created", "modified", "commit_ts"]

        def web_row(file_id, name, commit_ts=None):
            return (file_id, f"image/2024/{name}", "image/jpeg", None, "{}", 1, 1, None, None, None, None, None, None, None, None, None, None, None,
                    None, commit_ts)

        admin = FakeConnection({"FROM site_file": (file_columns, [admin_row(1, "a.jpg", MODIFIED), admin_row(2, "b.jpg", MODIFIED),
                                                                  admin_row(3, "c.jpg", MODIFIED), admin_row(4, "d.jpg", MODIFIED, "{\"x\":1}"),
                                                                  admin_row(5, "e.jpg", None)])})
        site = FakeConnection({"FROM web_site_file": (web_columns, [web_row(1, "a.jpg"), web_row(2, "b.jpg", datetime(2026, 1, 2, 8, 0, tzinfo=timezone.utc)),
                                                                   web_row(3, "c.jpg"), web_row(4, "d.jpg")])})

        class Server:
            def facts(self, paths):
                self.paths = paths
                return {"image/2024/a.jpg": cli.SiteFileFact(RELEASED, None)}

        server = Server()
        decisions, mtimes = cli.backfill_files(admin, site, server, True, False, Args())

        self.assertEqual(["image/2024/a.jpg", "image/2024/b.jpg", "image/2024/c.jpg", "image/2024/d.jpg"], server.paths)
        self.assertEqual({"image/2024/a.jpg": RELEASED}, mtimes)
        self.assertEqual((RELEASED, SOURCE_EXACT_MTIME), (decisions[1].published, decisions[1].source))
        self.assertEqual((datetime(2026, 1, 2, 8, 0), SOURCE_EXACT_COMMIT), (decisions[2].published, decisions[2].source))
        # c.jpg: same content but missing on the server, so the copies differ and the release predates the modification
        self.assertEqual((CREATED, SOURCE_INFERRED_CHANGED), (decisions[3].published, decisions[3].source))
        self.assertIn("missing on site server", decisions[3].detail)
        self.assertEqual((CREATED, SOURCE_INFERRED_CHANGED), (decisions[4].published, decisions[4].source))
        self.assertIn("metadata", decisions[4].detail)
        self.assertEqual(SOURCE_SKIPPED, decisions[5].source)

    def test_galleries_use_the_oldest_file_timestamp_else_the_comparison(self):
        admin = FakeConnection({
            "FROM gallery ORDER": (["id", "shortname", "description", "created", "modified", "published"],
                                   [(1, "trip", "Trip", CREATED, MODIFIED, None), (2, "old", "Old", CREATED, MODIFIED, None),
                                    (3, "new", "New", CREATED, None, None)]),
            "FROM gallery_file": (["gallery_id", "site_file_id"], [(1, 10), (1, 11), (2, 12)]),
            "FROM site_file": (["id", "file_type", "file_path", "file_name"], [(10, "IMAGE", "t", "a.jpg"), (11, "IMAGE", "t", "b.jpg"),
                                                                              (12, "IMAGE", "o", "c.jpg")])})
        site = FakeConnection({
            "FROM web_site_gallery g": (["id", "gallery_id", "shortname", "description", "created", "modified", "commit_ts"],
                                        [(100, 1, "trip", "Trip", CREATED, MODIFIED, None), (101, 2, "old", "Older", CREATED, RELEASED, None)]),
            "FROM web_site_gallery_file": (["gallery_id", "file_id"], [(100, 10), (100, 11), (101, 12)])})
        mtimes = {"image/t/a.jpg": RELEASED + timedelta(hours=2), "image/t/b.jpg": RELEASED}

        decisions = cli.backfill_galleries(admin, site, mtimes, False, False, Args())

        self.assertEqual((RELEASED, SOURCE_EXACT_MTIME), (decisions[0].published, decisions[0].source))
        # Gallery 2: its file has no server timestamp, the description differs, the site copy's modified time is the lower bound
        self.assertEqual((RELEASED, SOURCE_INFERRED_CHANGED), (decisions[1].published, decisions[1].source))
        self.assertEqual(SOURCE_SKIPPED, decisions[2].source)

    def test_data_sets_compare_the_csv_with_the_site_table(self):
        admin = FakeConnection({"FROM data_store": (["identifier", "csv_data", "created_at", "updated_at", "published"],
                                                    [("music", "title,year\nA,1\n", CREATED, MODIFIED, None), ("gps", "t,v\n1,2\n", CREATED, MODIFIED, None),
                                                     ("none", "a\n1\n", CREATED, MODIFIED, None)])})

        class Site(FakeConnection):
            def cursor(self):
                cursor = super().cursor()
                original = cursor.execute

                def execute(sql, params=()):
                    if "information_schema.columns" in sql:
                        table = params[0]
                        cursor.description = [("column_name",)]
                        cursor.rows = {"website_data__music": [("title",), ("year",)], "website_data__gps": [("t",), ("v",)]}.get(table, [])
                        cursor.executed.append((sql, params))
                        return
                    if 'FROM "website_data__music"' in sql:
                        cursor.description = [("title",), ("year",)]
                        cursor.rows = [("A", 1)]
                        return
                    if 'FROM "website_data__gps"' in sql:
                        cursor.description = [("t",), ("v",)]
                        cursor.rows = [(1, 3)]
                        return
                    original(sql, params)

                cursor.execute = execute
                return cursor

        decisions = cli.backfill_data_sets(admin, Site({}), False, False)

        # In fixture order: music (site table is the CSV), gps (a value differs), none (no site table)
        self.assertEqual((MODIFIED, SOURCE_INFERRED_UNCHANGED), (decisions[0].published, decisions[0].source))
        self.assertEqual((CREATED, SOURCE_INFERRED_CHANGED), (decisions[1].published, decisions[1].source))
        self.assertEqual(SOURCE_SKIPPED, decisions[2].source)

    def test_sql_and_report_output(self):
        decisions = [cli.Decision("page", "1", RELEASED, SOURCE_EXACT_PAGE), cli.Decision("data_set", "it's", RELEASED, SOURCE_INFERRED_UNCHANGED, "same"),
                     cli.Decision("gallery", "2", None, SOURCE_UNRESOLVED, "differs")]
        statements = cli.sql_statements(decisions, overwrite=False)
        self.assertEqual(["UPDATE page SET published = '2026-01-02 10:00:00' WHERE id = 1 AND published IS NULL; -- exact:web_site_page.published",
                          "UPDATE data_store SET published = '2026-01-02 10:00:00' WHERE identifier = 'it''s' AND published IS NULL; -- inferred:unchanged"],
                         statements)
        self.assertTrue(cli.sql_statements(decisions, overwrite=True)[0].endswith("WHERE id = 1; -- exact:web_site_page.published"))
        with tempfile.TemporaryDirectory() as directory:
            sql_path = os.path.join(directory, "out.sql")
            report_path = os.path.join(directory, "report.csv")
            cli.write_sql(sql_path, statements)
            cli.write_report(report_path, decisions)
            with open(sql_path, encoding="utf-8") as handle:
                sql = handle.read()
            self.assertTrue(sql.startswith("-- Generated") and "BEGIN;\n" in sql and sql.endswith("COMMIT;\n"))
            with open(report_path, encoding="utf-8") as handle:
                report = handle.read().splitlines()
            self.assertEqual("resource,key,published,source,detail", report[0])
            self.assertEqual("gallery,2,,unresolved,differs", report[3])
        admin = FakeConnection({})
        cli.apply_statements(admin, statements)
        self.assertTrue(admin.committed)
        self.assertEqual(2, len(admin.executed()))
        self.assertNotIn("--", admin.executed()[0])

    def test_argument_validation(self):
        args = cli.parse_args(["--admin-db", "a", "--site-db", "s", "--resources", "pages, data"])
        self.assertEqual(["pages", "data"], args.resources)
        for argv in (["--admin-db", "a", "--site-db", "s", "--resources", "pages,bogus"], ["--admin-db", "a", "--site-db", "s", "--site-ssh", "u@h"],
                     ["--admin-db", "a", "--site-db", "s", "--sha256"]):
            with self.subTest(argv=argv), open(os.devnull, "w") as devnull, contextlib.redirect_stderr(devnull):
                with self.assertRaises(SystemExit):
                    cli.parse_args(argv)

    def test_site_server_script_quotes_the_root_and_parses_the_output(self):
        server = cli.SiteServer("deploy@site", "/var/www/it's", ["IdentityFile=~/.ssh/id"], sha256=True)
        self.assertEqual("'/var/www/it'\"'\"'s'", cli.shell_quote(server.www_root))
        self.assertEqual({}, server.facts([]))


if __name__ == "__main__":
    unittest.main()
