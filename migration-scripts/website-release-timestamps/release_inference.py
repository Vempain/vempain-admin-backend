"""
Pure decision logic of the website release timestamp backfill: no database, no network, so that it can be unit tested on its own.

A resource of the admin database (page, gallery, site file, data set) gets its `published` timestamp from, in order of preference:

1. an exact release time recorded somewhere: `web_site_page.published`, the modification time of the file on the site server (the SFTP
   upload sets it to the transfer time), or the commit timestamp of the site row when PostgreSQL tracks them;
2. an inference from comparing the admin copy with the site copy: when they are the same, the resource was released at or after its
   last modification (`published` = `modified`, else `created`, so the "modified after publish" filter does not flag it); when they
   differ, the release happened before the last modification, so the best lower bound that is older than `modified` is used (the site
   copy's own timestamps, else `created`);
3. nothing: the copies differ but no timestamp older than `modified` exists. The resource is reported and left NULL.
"""

import csv
import io
from datetime import date, datetime, timezone
from decimal import Decimal, InvalidOperation
from typing import Any, Iterable, List, Mapping, Optional, Sequence, Tuple

SOURCE_EXACT_PAGE = "exact:web_site_page.published"
SOURCE_EXACT_MTIME = "exact:site-server-mtime"
SOURCE_EXACT_COMMIT = "exact:commit-timestamp"
SOURCE_INFERRED_UNCHANGED = "inferred:unchanged"
SOURCE_INFERRED_CHANGED = "inferred:changed"
SOURCE_UNRESOLVED = "unresolved"
SOURCE_SKIPPED = "skipped"

# Admin site_file columns that are copied to web_site_file and therefore prove a change when they differ
FILE_FIELDS = ("mime_type", "comment", "metadata", "width", "height", "length", "pages", "rights_holder", "rights_terms", "rights_url", "creator_name",
               "creator_email", "creator_country", "creator_url", "location_id")
# web_site_file names a few of them differently
WEB_FILE_FIELD_NAMES = {"mime_type": "mimetype"}


class Decision:
    """One backfill decision: the resource, its key, the release time to write (None when nothing is written) and why."""

    def __init__(self, resource: str, key: str, published: Optional[datetime], source: str, detail: str = "") -> None:
        self.resource = resource
        self.key = key
        self.published = published
        self.source = source
        self.detail = detail

    @property
    def applies(self) -> bool:
        return self.published is not None

    def __repr__(self) -> str:
        return "Decision({!r}, {!r}, {!r}, {!r}, {!r})".format(self.resource, self.key, self.published, self.source, self.detail)


def to_naive_utc(value: Any) -> Optional[datetime]:
    """Normalizes every timestamp to a naive UTC datetime, the form the admin TIMESTAMP columns hold."""
    if value is None:
        return None
    if isinstance(value, datetime):
        if value.tzinfo is None:
            return value.replace(microsecond=value.microsecond)
        return value.astimezone(timezone.utc).replace(tzinfo=None)
    if isinstance(value, (int, float)):
        return datetime.fromtimestamp(value, tz=timezone.utc).replace(tzinfo=None)
    raise TypeError(f"Unsupported timestamp value: {value!r}")


def site_file_path(file_type: str, file_path: str, file_name: str) -> str:
    """The relative path of an admin site file on the site server and in web_site_file.file_path: <type short name>/<path>/<name>."""
    short_name = (file_type or "unknown").lower()
    path = (file_path or "").strip("/")
    return f"{short_name}/{path}/{file_name}" if path else f"{short_name}/{file_name}"


def infer_from_comparison(resource: str, key: str, differences: Sequence[str], created: Optional[datetime], modified: Optional[datetime],
                          site_lower_bound: Optional[datetime] = None) -> Decision:
    """
    The inferred release time of a resource whose admin and site copies were compared.

    Unchanged copies: the release is at or after the last modification. Changed copies: the release predates the last modification, so
    the newest known time older than `modified` is used (the site copy's own timestamp when it has one, else `created`); when nothing
    older than `modified` exists the resource stays unresolved.
    """
    if not differences:
        published = modified or created
        return Decision(resource, key, published, SOURCE_INFERRED_UNCHANGED, "admin and site copies are the same")

    detail = "differs in " + ", ".join(differences)
    if modified is None:
        return Decision(resource, key, None, SOURCE_UNRESOLVED, detail + "; admin copy was never modified, no release time older than its creation exists")

    candidates = [candidate for candidate in (site_lower_bound, created) if candidate is not None and candidate < modified]
    if not candidates:
        return Decision(resource, key, None, SOURCE_UNRESOLVED, detail + "; no known time older than the last modification")
    return Decision(resource, key, max(candidates), SOURCE_INFERRED_CHANGED, detail)


def decide_page(page_id: int, web_published: Optional[datetime], web_exists: bool) -> Decision:
    key = str(page_id)
    if not web_exists:
        return Decision("page", key, None, SOURCE_SKIPPED, "not published to the site")
    if web_published is None:
        return Decision("page", key, None, SOURCE_UNRESOLVED, "site row carries no published timestamp")
    return Decision("page", key, to_naive_utc(web_published), SOURCE_EXACT_PAGE)


def compare_site_file(admin: Mapping[str, Any], web: Mapping[str, Any], site_sha256: Optional[str] = None) -> List[str]:
    """Names of the fields in which the admin site file and its site copy differ (the site server's checksum included when given)."""
    differences = []
    expected_path = site_file_path(admin.get("file_type"), admin.get("file_path"), admin.get("file_name"))
    if _norm(expected_path) != _norm(web.get("file_path")):
        differences.append("file_path")
    for field in FILE_FIELDS:
        if _norm(admin.get(field)) != _norm(web.get(WEB_FILE_FIELD_NAMES.get(field, field))):
            differences.append(field)
    if site_sha256 is not None and _norm(site_sha256).lower() != _norm(admin.get("sha256sum")).lower():
        differences.append("sha256sum")
    return differences


def compare_gallery(admin: Mapping[str, Any], admin_file_ids: Sequence[int], web: Mapping[str, Any], web_file_ids: Sequence[int]) -> List[str]:
    """Names of the fields in which the admin gallery and its site copy differ; the file lists are compared in gallery order."""
    differences = []
    if _norm(admin.get("shortname")) != _norm(web.get("shortname")):
        differences.append("shortname")
    if _norm(admin.get("description")) != _norm(web.get("description")):
        differences.append("description")
    if list(admin_file_ids) != list(web_file_ids):
        differences.append("files")
    return differences


def gallery_release_from_file_mtimes(file_paths: Iterable[str], mtimes: Mapping[str, datetime]) -> Optional[datetime]:
    """
    The release time of a gallery from the site server: a publish rewrites every file of the gallery, so the oldest modification time among
    them is the last publish (a newer time only means another gallery transferred the shared file again). None when a file is missing on
    the server, because then the gallery was not published as it is now.
    """
    times = []
    for path in file_paths:
        mtime = mtimes.get(path)
        if mtime is None:
            return None
        times.append(mtime)
    return min(times) if times else None


def parse_csv(csv_text: str) -> Tuple[List[str], List[List[str]]]:
    """Header and rows of the data set CSV the way the admin backend reads it (first line is the header, blank lines are skipped)."""
    reader = csv.reader(io.StringIO(csv_text or ""))
    rows = [row for row in reader if any(cell.strip() for cell in row)]
    if not rows:
        return [], []
    header = [cell.strip() for cell in rows[0]]
    return header, [[cell for cell in row] for row in rows[1:]]


def compare_data_set(csv_text: str, table_columns: Optional[Sequence[str]], table_rows: Optional[Sequence[Sequence[Any]]]) -> List[str]:
    """
    Reasons why the admin CSV of a data set and its published site table differ: the table is missing, its columns are not the CSV columns,
    the row counts differ, or the normalized cell values differ (order-insensitive). An empty list means the site table is the CSV.
    """
    if table_columns is None or table_rows is None:
        return ["site table missing"]
    header, csv_rows = parse_csv(csv_text)
    columns = [column.lower() for column in table_columns]
    if [column.lower() for column in header] != columns:
        return ["columns"]
    if len(csv_rows) != len(table_rows):
        return ["row count"]
    csv_values = sorted(tuple(normalize_cell(cell) for cell in row) for row in csv_rows)
    table_values = sorted(tuple(normalize_cell(cell) for cell in row) for row in table_rows)
    return [] if csv_values == table_values else ["content"]


def normalize_cell(value: Any) -> str:
    """One textual form for a CSV cell and the same value read back from PostgreSQL."""
    if value is None:
        return ""
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, datetime):
        return to_naive_utc(value).isoformat(sep=" ", timespec="seconds") if value.tzinfo else value.isoformat(sep=" ", timespec="seconds")
    if isinstance(value, date):
        return value.isoformat()
    if isinstance(value, (int, float, Decimal)):
        return _canonical_number(Decimal(str(value)))
    text = str(value).strip()
    if text == "":
        return ""
    lowered = text.lower()
    if lowered in ("true", "false"):
        return lowered
    try:
        return _canonical_number(Decimal(text))
    except InvalidOperation:
        pass
    for pattern in ("%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S%z", "%Y-%m-%d %H:%M:%S%z", "%Y-%m-%dT%H:%M:%SZ"):
        try:
            parsed = datetime.strptime(text, pattern)
            return normalize_cell(parsed)
        except ValueError:
            continue
    return text


def _canonical_number(number: Decimal) -> str:
    normalized = number.normalize()
    text = format(normalized, "f")
    return "0" if text in ("-0", "0.0") else text


def _norm(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float, Decimal)):
        return _canonical_number(Decimal(str(value)))
    return str(value).strip()
