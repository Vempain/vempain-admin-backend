#!/usr/bin/env python3
"""
Backfills the `published` timestamps of the Vempain Admin database (page, gallery, site_file, data_store) from what the published
website knows: the real release times where they exist, and otherwise a comparison of the admin copy with the site copy.

The script runs outside the services. By default it only writes a report and an SQL file for review; `--apply` runs the updates against
the admin database in one transaction. Only NULL `published` columns are filled unless `--overwrite` is given.

See README.md next to this file for the sources, the decision rules and the connection options.
"""

import argparse
import csv
import os
import subprocess
import sys
from collections import defaultdict
from datetime import datetime
from typing import Any, Dict, List, Optional, Tuple

# The decision module sits next to this script; make it importable however the script is started (symlink, python -I, another cwd)
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from release_inference import (SOURCE_EXACT_COMMIT, SOURCE_EXACT_MTIME, SOURCE_SKIPPED, SOURCE_UNRESOLVED, Decision,  # noqa: E402
                               compare_data_set, compare_gallery, compare_site_file, decide_page, gallery_release_from_file_mtimes,
                               infer_from_comparison, site_file_path, to_naive_utc)

# psycopg2 is what Rocky/RHEL 8 and 9 package for their Python (dnf install python3-psycopg2); psycopg 3 is accepted when installed
try:
    import psycopg2 as psycopg  # type: ignore
except ImportError:  # pragma: no cover - depends on the environment
    try:
        import psycopg  # type: ignore
    except ImportError:  # pragma: no cover
        psycopg = None

RESOURCES = ("pages", "galleries", "files", "data")
TABLE_PREFIX = "website_data__"


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)
    if psycopg is None:
        print("psycopg2 (dnf install python3-psycopg2) or psycopg 3 is required, see README.md", file=sys.stderr)
        return 2

    admin = psycopg.connect(args.admin_db)
    site = psycopg.connect(args.site_db)
    try:
        commit_timestamps = site_tracks_commit_timestamps(site)
        log(args, f"site database tracks commit timestamps: {commit_timestamps}")
        site_server = SiteServer(args.site_ssh, args.site_www_root, args.ssh_option, args.sha256) if args.site_ssh else None
        if site_server:
            log(args, f"site server: {args.site_ssh}:{args.site_www_root} (sha256={args.sha256})")

        decisions = []  # type: List[Decision]
        if "pages" in args.resources:
            decisions += backfill_pages(admin, site, args.overwrite)
        file_decisions = {}  # type: Dict[int, Decision]
        file_mtimes = {}  # type: Dict[str, datetime]
        if "files" in args.resources or "galleries" in args.resources:
            file_decisions, file_mtimes = backfill_files(admin, site, site_server, commit_timestamps, args.overwrite, args)
        if "files" in args.resources:
            decisions += file_decisions.values()
        if "galleries" in args.resources:
            decisions += backfill_galleries(admin, site, file_mtimes, commit_timestamps, args.overwrite, args)
        if "data" in args.resources:
            decisions += backfill_data_sets(admin, site, commit_timestamps, args.overwrite)

        write_report(args.report_out, decisions)
        statements = sql_statements(decisions, args.overwrite)
        write_sql(args.sql_out, statements)
        print_summary(decisions, args)

        if args.apply:
            apply_statements(admin, statements)
            print(f"applied {len(statements)} updates to the admin database")
        else:
            print(f"dry run: {len(statements)} updates written to {args.sql_out}, nothing changed (use --apply to run them)")
        return 0
    finally:
        admin.close()
        site.close()


def parse_args(argv: Optional[List[str]]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--admin-db", required=True, help="libpq connection string or URI of the admin database (read, and written with --apply)")
    parser.add_argument("--site-db", required=True, help="libpq connection string or URI of the site database (read only)")
    parser.add_argument("--site-ssh", help="user@host of the site server; enables the file modification time source (and --sha256)")
    parser.add_argument("--site-www-root", help="vempain.site.www-root on the site server, the directory the published files live in")
    parser.add_argument("--ssh-option", action="append", default=[], help="extra ssh -o option, repeatable (e.g. IdentityFile=~/.ssh/id_site)")
    parser.add_argument("--sha256", action="store_true", help="also checksum every published file on the site server and compare it to site_file.sha256sum")
    parser.add_argument("--resources", default=",".join(RESOURCES), help=f"comma separated subset of {','.join(RESOURCES)} (default: all)")
    parser.add_argument("--overwrite", action="store_true", help="replace existing published timestamps as well (default: only NULL ones)")
    parser.add_argument("--apply", action="store_true", help="run the generated updates against the admin database")
    parser.add_argument("--sql-out", default="backfill_release_timestamps.sql", help="path of the generated SQL file")
    parser.add_argument("--report-out", default="backfill_release_timestamps_report.csv", help="path of the decision report (CSV)")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args(argv)
    args.resources = [resource.strip() for resource in args.resources.split(",") if resource.strip()]
    unknown = [resource for resource in args.resources if resource not in RESOURCES]
    if unknown:
        parser.error(f"unknown resources: {', '.join(unknown)}")
    if args.site_ssh and not args.site_www_root:
        parser.error("--site-www-root is required with --site-ssh")
    if args.sha256 and not args.site_ssh:
        parser.error("--sha256 needs --site-ssh")
    return args


def log(args: argparse.Namespace, message: str) -> None:
    if args.verbose:
        print(message, file=sys.stderr)


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Database access

def query(connection, sql: str, params: tuple = ()) -> List[Dict[str, Any]]:
    with connection.cursor() as cursor:
        cursor.execute(sql, params)
        columns = [description[0] for description in cursor.description]
        return [dict(zip(columns, row)) for row in cursor.fetchall()]


def site_tracks_commit_timestamps(site) -> bool:
    rows = query(site, "SHOW track_commit_timestamp")
    return bool(rows) and str(list(rows[0].values())[0]).lower() == "on"


def commit_timestamp_select(enabled: bool, alias: str) -> str:
    return f"pg_xact_commit_timestamp({alias}.xmin) AS commit_ts" if enabled else "NULL AS commit_ts"


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Pages

def backfill_pages(admin, site, overwrite: bool) -> List[Decision]:
    pages = query(admin, "SELECT id, published FROM page ORDER BY id")
    web_pages = {row["page_id"]: row for row in query(site, "SELECT page_id, published FROM web_site_page")}
    decisions = []
    for page in pages:
        if page["published"] is not None and not overwrite:
            decisions.append(Decision("page", str(page["id"]), None, SOURCE_SKIPPED, "published already set"))
            continue
        web = web_pages.get(page["id"])
        decisions.append(decide_page(page["id"], web["published"] if web else None, web is not None))
    return decisions


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Site files

def backfill_files(admin, site, site_server, commit_timestamps: bool, overwrite: bool, args) -> Tuple[Dict[int, Decision], Dict[str, datetime]]:
    files = query(admin, """
        SELECT id, file_type, file_path, file_name, mime_type, sha256sum, comment, metadata, width, height, length, pages, rights_holder,
               rights_terms, rights_url, creator_name, creator_email, creator_country, creator_url, location_id, created, modified, published
        FROM site_file ORDER BY id""")
    web_files = {row["file_id"]: row for row in query(site, f"""
        SELECT w.file_id, w.file_path, w.mimetype, w.comment, w.metadata, w.width, w.height, w.length, w.pages, w.rights_holder, w.rights_terms,
               w.rights_url, w.creator_name, w.creator_email, w.creator_country, w.creator_url, w.location_id, w.created, w.modified,
               {commit_timestamp_select(commit_timestamps, 'w')}
        FROM web_site_file w""")}

    paths = {file["id"]: site_file_path(file["file_type"], file["file_path"], file["file_name"]) for file in files if file["id"] in web_files}
    facts = site_server.facts(sorted(set(paths.values()))) if site_server else {}
    mtimes = {path: fact.mtime for path, fact in facts.items() if fact.mtime is not None}

    decisions = {}  # type: Dict[int, Decision]
    for file in files:
        key = str(file["id"])
        web = web_files.get(file["id"])
        if web is None:
            decisions[file["id"]] = Decision("site_file", key, None, SOURCE_SKIPPED, "not published to the site")
            continue
        if file["published"] is not None and not overwrite:
            decisions[file["id"]] = Decision("site_file", key, None, SOURCE_SKIPPED, "published already set")
            continue
        path = paths[file["id"]]
        fact = facts.get(path)
        if fact is not None and fact.mtime is not None:
            decisions[file["id"]] = Decision("site_file", key, fact.mtime, SOURCE_EXACT_MTIME, path)
            continue
        if web.get("commit_ts") is not None:
            decisions[file["id"]] = Decision("site_file", key, to_naive_utc(web["commit_ts"]), SOURCE_EXACT_COMMIT, path)
            continue
        site_sha = fact.sha256 if fact is not None else None
        differences = compare_site_file(file, web, site_sha)
        if site_server and fact is None:
            differences = differences + ["missing on site server"]
        lower_bound = to_naive_utc(web.get("modified") or web.get("created"))
        decisions[file["id"]] = infer_from_comparison("site_file", key, differences, to_naive_utc(file["created"]), to_naive_utc(file["modified"]),
                                                      lower_bound)
    log(args, f"files: {len(files)} admin, {len(web_files)} on site, {len(mtimes)} server timestamps")
    return decisions, mtimes


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Galleries

def backfill_galleries(admin, site, file_mtimes: Dict[str, datetime], commit_timestamps: bool, overwrite: bool, args) -> List[Decision]:
    galleries = query(admin, "SELECT id, shortname, description, created, modified, published FROM gallery ORDER BY id")
    admin_files = defaultdict(list)  # type: Dict[int, List[int]]
    for row in query(admin, "SELECT gallery_id, site_file_id FROM gallery_file ORDER BY gallery_id, sort_order, site_file_id"):
        admin_files[row["gallery_id"]].append(row["site_file_id"])
    admin_paths = {row["id"]: site_file_path(row["file_type"], row["file_path"], row["file_name"])
                   for row in query(admin, "SELECT id, file_type, file_path, file_name FROM site_file")}

    web_galleries = {row["gallery_id"]: row for row in query(site, f"""
        SELECT g.id, g.gallery_id, g.shortname, g.description, g.created, g.modified, {commit_timestamp_select(commit_timestamps, 'g')}
        FROM web_site_gallery g""")}
    web_files = defaultdict(list)  # type: Dict[int, List[int]]
    for row in query(site, """
        SELECT gf.gallery_id, f.file_id FROM web_site_gallery_file gf JOIN web_site_file f ON f.id = gf.file_id
        ORDER BY gf.gallery_id, gf.sort_order, f.file_id"""):
        web_files[row["gallery_id"]].append(row["file_id"])

    decisions = []
    for gallery in galleries:
        key = str(gallery["id"])
        web = web_galleries.get(gallery["id"])
        if web is None:
            decisions.append(Decision("gallery", key, None, SOURCE_SKIPPED, "not published to the site"))
            continue
        if gallery["published"] is not None and not overwrite:
            decisions.append(Decision("gallery", key, None, SOURCE_SKIPPED, "published already set"))
            continue
        file_ids = admin_files.get(gallery["id"], [])
        if file_mtimes and file_ids:
            release = gallery_release_from_file_mtimes([admin_paths[file_id] for file_id in file_ids if file_id in admin_paths], file_mtimes)
            if release is not None:
                decisions.append(Decision("gallery", key, release, SOURCE_EXACT_MTIME, f"oldest server timestamp of {len(file_ids)} files"))
                continue
        if web.get("commit_ts") is not None:
            decisions.append(Decision("gallery", key, to_naive_utc(web["commit_ts"]), SOURCE_EXACT_COMMIT))
            continue
        differences = compare_gallery(gallery, file_ids, web, web_files.get(web["id"], []))
        lower_bound = to_naive_utc(web.get("modified") or web.get("created"))
        decisions.append(infer_from_comparison("gallery", key, differences, to_naive_utc(gallery["created"]), to_naive_utc(gallery["modified"]),
                                               lower_bound))
    log(args, f"galleries: {len(galleries)} admin, {len(web_galleries)} on site")
    return decisions


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Data sets

def backfill_data_sets(admin, site, commit_timestamps: bool, overwrite: bool) -> List[Decision]:
    data_sets = query(admin, "SELECT identifier, csv_data, created_at, updated_at, published FROM data_store ORDER BY identifier")
    decisions = []
    for data_set in data_sets:
        key = data_set["identifier"]
        if data_set["published"] is not None and not overwrite:
            decisions.append(Decision("data_set", key, None, SOURCE_SKIPPED, "published already set"))
            continue
        table = TABLE_PREFIX + key
        columns = [row["column_name"] for row in query(site, """
            SELECT column_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = %s ORDER BY ordinal_position""",
                                                                (table,))]
        if not columns:
            decisions.append(Decision("data_set", key, None, SOURCE_SKIPPED, "not published to the site"))
            continue
        quoted = '"' + table.replace('"', '""') + '"'
        if commit_timestamps:
            latest = query(site, f"SELECT max(pg_xact_commit_timestamp(xmin)) AS commit_ts FROM {quoted}")[0]["commit_ts"]
            if latest is not None:
                decisions.append(Decision("data_set", key, to_naive_utc(latest), SOURCE_EXACT_COMMIT, "newest row commit of " + table))
                continue
        rows = [tuple(row[column] for column in columns) for row in query(site, f"SELECT * FROM {quoted}")]
        differences = compare_data_set(data_set["csv_data"], columns, rows)
        decisions.append(infer_from_comparison("data_set", key, differences, to_naive_utc(data_set["created_at"]), to_naive_utc(data_set["updated_at"])))
    return decisions


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Site server

class SiteFileFact:
    def __init__(self, mtime: Optional[datetime], sha256: Optional[str]):
        self.mtime = mtime
        self.sha256 = sha256


class SiteServer:
    """Reads the modification times (and optionally the checksums) of the published files over ssh, in one connection per batch."""

    BATCH = 500

    def __init__(self, target: str, www_root: str, options: List[str], sha256: bool):
        self.target = target
        self.www_root = www_root
        self.options = options
        self.sha256 = sha256

    def facts(self, paths: List[str]) -> Dict[str, SiteFileFact]:
        facts = {}  # type: Dict[str, SiteFileFact]
        for start in range(0, len(paths), self.BATCH):
            facts.update(self._batch(paths[start:start + self.BATCH]))
        return facts

    def _batch(self, paths: List[str]) -> Dict[str, SiteFileFact]:
        checksum = 'sum=$(sha256sum -- "$p" | cut -d" " -f1)' if self.sha256 else "sum=-"
        script = f"""cd {shell_quote(self.www_root)} || exit 3
while IFS= read -r p; do
    if [ -f "$p" ]; then
        {checksum}
        printf '%s\\t%s\\t%s\\n' "$(stat -c %Y -- "$p")" "$sum" "$p"
    fi
done <<'VEMPAIN_FILES'
{chr(10).join(paths)}
VEMPAIN_FILES
"""
        command = ["ssh", "-o", "BatchMode=yes"]
        for option in self.options:
            command += ["-o", option]
        command += [self.target, "sh", "-s"]
        # Written for the Python 3.6 of Rocky 8: the stdout/stderr pipes and universal_newlines spell out what newer keywords abbreviate
        completed = subprocess.run(command, input=script, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True, check=False)
        if completed.returncode != 0:
            raise SystemExit(f"ssh {self.target} failed ({completed.returncode}): {completed.stderr.strip()}")
        facts = {}
        for line in completed.stdout.splitlines():
            parts = line.split("\t", 2)
            if len(parts) != 3:
                continue
            epoch, checksum_value, path = parts
            facts[path] = SiteFileFact(to_naive_utc(int(epoch)), None if checksum_value == "-" else checksum_value)
        return facts


def shell_quote(value: str) -> str:
    return "'" + value.replace("'", "'\"'\"'") + "'"


# ---------------------------------------------------------------------------------------------------------------------------------------------
# Output

TABLES = {"page": ("page", "id"), "gallery": ("gallery", "id"), "site_file": ("site_file", "id"), "data_set": ("data_store", "identifier")}


def sql_statements(decisions: List[Decision], overwrite: bool) -> List[str]:
    statements = []
    for decision in decisions:
        if not decision.applies:
            continue
        table, column = TABLES[decision.resource]
        key = decision.key if column == "id" else "'" + decision.key.replace("'", "''") + "'"
        guard = "" if overwrite else " AND published IS NULL"
        statements.append(f"UPDATE {table} SET published = '{decision.published.isoformat(sep=' ')}' WHERE {column} = {key}{guard};"
                          f" -- {decision.source}")
    return statements


def write_sql(path: str, statements: List[str]) -> None:
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("-- Generated by backfill_release_timestamps.py; timestamps are UTC, review before running with psql\n")
        handle.write("BEGIN;\n")
        for statement in statements:
            handle.write(statement + "\n")
        handle.write("COMMIT;\n")


def write_report(path: str, decisions: List[Decision]) -> None:
    with open(path, "w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["resource", "key", "published", "source", "detail"])
        for decision in decisions:
            writer.writerow([decision.resource, decision.key, decision.published.isoformat(sep=" ") if decision.published else "", decision.source,
                             decision.detail])


def apply_statements(admin, statements: List[str]) -> None:
    with admin.cursor() as cursor:
        for statement in statements:
            cursor.execute(statement.split(" -- ")[0])
    admin.commit()


def print_summary(decisions: List[Decision], args) -> None:
    counts = defaultdict(int)  # type: Dict[Tuple[str, str], int]
    for decision in decisions:
        counts[(decision.resource, decision.source)] += 1
    for (resource, source), count in sorted(counts.items()):
        print(f"{resource:10} {source:36} {count}")
    unresolved = [decision for decision in decisions if decision.source == SOURCE_UNRESOLVED]
    if unresolved:
        print(f"{len(unresolved)} resources could not be resolved, see {args.report_out}")


if __name__ == "__main__":
    sys.exit(main())
