# Website release timestamp backfill

Standalone migration that fills the `published` columns of the Vempain Admin database (`page`, `gallery`, `site_file`, `data_store`)
for resources that were released to the Vempain Website before the admin backend started recording the release time (admin migration
`V1007__website_release_timestamps.sql`). It runs outside the services, reads the admin and the site database (and
optionally the site server over ssh), writes a report and an SQL file for review, and applies the updates only when asked.

The scripts live here on purpose, not in the backend: the backfill is a one-off that needs access the services do not have (the site
server's filesystem) and its decisions deserve a review before they are written.

## What the website knows

- **page**: exact time in `web_site_page.published`, written by every page publish. No comparison needed.
- **site file**: exact time from the modification time of the file on the site server (`vempain.site.www-root/<type>/<path>/<name>`,
  the SFTP upload sets it to the transfer time), or from the commit timestamp of the `web_site_file` row when PostgreSQL tracks them.
  Comparison: the `web_site_file` columns against `site_file` (path, mime type, comment, metadata, dimensions, rights, creator,
  location) and, with `--sha256`, the checksum of the file on the server against `site_file.sha256sum`.
- **gallery**: exact time from the oldest modification time among its files on the site server (a publish rewrites every file of the
  gallery; a newer time only means a shared file was transferred again by another gallery), or from the commit timestamp of the
  `web_site_gallery` row. Comparison: short name, description and the ordered file list against `web_site_gallery` and
  `web_site_gallery_file`.
- **data set**: exact time from the newest commit timestamp of the rows of `website_data__<identifier>` when PostgreSQL tracks them.
  Comparison: the CSV in `data_store.csv_data` against the site table (columns, row count, normalized cell values, order-insensitive).

Commit timestamps exist only when the site database runs with `track_commit_timestamp = on` (and only for rows written after it was
switched on); the script detects the setting and uses them automatically.

## Decision rules

For every resource whose `published` is NULL (or every resource with `--overwrite`), in this order:

1. **exact**: a real release time from the table above is used as is.
2. **inferred, unchanged**: the admin copy and the site copy are the same, so the resource was released at or after its last
   modification: `published` = `modified`, else `created`. The "modified after publish" filter of the admin UI then does not flag it.
3. **inferred, changed**: the copies differ, so the release predates the last modification: the newest known time that is older than
   `modified` is used, i.e. the site copy's own `modified`/`created` (a copy of the admin timestamps at publish time, for galleries and
   for files published after site migration `V1004`), else the admin `created`. The filter then flags the resource, which is right: the
   website shows an outdated copy.
4. **unresolved**: the copies differ but nothing older than `modified` exists (for example a file that was never modified since its
   creation but differs on the site). It is listed in the report and left NULL; publish it again or decide by hand.

Resources that are not on the site at all (no site row, no site table) are skipped and stay NULL, which already means "never released".
Timestamps are written as UTC, the form the admin backend stores.

## Running

Requirements: Python 3.6 or newer, i.e. the `python3` of Rocky/RHEL 8 (3.6) and 9 (3.9), with the distribution's PostgreSQL driver
(`dnf install python3-psycopg2`; `psycopg` 3 is accepted when it is installed, `requirements.txt` lists both for pip), network access to both databases (in the
compose deployment they are published on the host as `4001` admin and `4002` site, see `vempain-cluster`), and for the site
server source an ssh login that can read the published files (`stat`, `sha256sum` and `sh` on the server).

```bash
cd vempain-admin-backend/migration-scripts/website-release-timestamps
python3 -m unittest discover -s . -p 'test_*.py'          # the decision logic, no database needed

# Dry run: report + SQL, nothing is changed
python3 backfill_release_timestamps.py \
    --admin-db 'postgresql://vempain_admin:secret@localhost:4001/vempain_admin_db?sslmode=require' \
    --site-db  'postgresql://vempain_site:secret@localhost:4002/vempain_site_db?sslmode=require' \
    --site-ssh vempain@site.example.org --site-www-root /var/www/site --ssh-option IdentityFile=~/.ssh/id_site \
    --sha256 --verbose

# Review backfill_release_timestamps_report.csv (one line per resource: key, timestamp, source, detail) and
# backfill_release_timestamps.sql, then either apply the SQL with psql or run the script again with --apply
python3 backfill_release_timestamps.py --admin-db ... --site-db ... --site-ssh ... --site-www-root ... --apply
```

Options: `--resources pages,galleries,files,data` limits the run (galleries need the file timestamps, so `galleries` collects them even
when `files` is left out); `--overwrite` recomputes timestamps that are already set; `--sql-out`/`--report-out` change the output paths.
Without `--site-ssh` the file and gallery decisions fall back to commit timestamps and the comparison. `--sha256` checksums every published
file on the server, which takes a while on a large site but is the only way to detect a changed file whose metadata is unchanged.

The generated SQL only touches rows whose `published` is still NULL (`AND published IS NULL`, dropped with `--overwrite`), so it is safe
to run twice. The admin backend keeps the column current from now on; the backfill is needed once per deployment.

## Files

- `backfill_release_timestamps.py`: the command line tool (database access, ssh, SQL/report output).
- `release_inference.py`: the decision rules and comparisons, free of I/O.
- `test_release_inference.py`: unit tests of both, with fake database connections (`python3 -m unittest`).
- `requirements.txt`: the PostgreSQL driver for pip installs (the distribution package `python3-psycopg2` is preferred).
