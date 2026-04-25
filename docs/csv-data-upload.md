# CSV Data Upload & Publication Feature

## Security Considerations

> **Warning**: The `create_sql`, `fetch_all_sql`, and `fetch_subset_sql` fields allow administrators to provide arbitrary SQL that will be executed against the site database during publication. This feature is therefore **only suitable for trusted administrators**. The following safeguards are in place:
>
> - **Identifier validation**: The `identifier` is validated against `^[a-z][a-z0-9_]*$` and the resulting table name is double-quoted during DROP/creation, preventing any SQL injection through the identifier.
> - **Create SQL validation**: The `create_sql` must start with `CREATE TABLE` (case-insensitive); other DDL/DML statements are rejected.
> - **Target table enforcement**: During publication the backend always creates `website_data__<identifier>` regardless of the table name typed in `create_sql`; this prevents accidental/malicious publication to unintended table names.
> - **Column name validation**: CSV header column names are validated against `^[a-zA-Z_][a-zA-Z0-9_]*$` and double-quoted in INSERT statements.
> - **Parameterized inserts**: CSV data rows are inserted using JDBC parameterized queries (`?` placeholders), preventing injection through data values.
>
> Access to these endpoints should be restricted to administrator roles at the application security level.

## Overview

The CSV Data Upload & Publication feature allows administrators to upload structured data in CSV format
along with accompanying metadata. The data is stored in the admin database and can then be published
to the site database as a dynamically created table.

## Metadata Fields

Each data set requires the following metadata:

| Field                | Description                                             | Constraints                                                                                                        |
|----------------------|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `identifier`         | Unique identifier for the data set                      | Must start with a lowercase letter; only lowercase letters, numbers, and underscores allowed (`^[a-z][a-z0-9_]*$`) |
| `type`               | Type of the data (e.g. `time_series`, `tabulated`)      | Required                                                                                                           |
| `description`        | Human-readable description of the data set              | Optional                                                                                                           |
| `column_definitions` | JSON array describing column names and types            | Required                                                                                                           |
| `create_sql`         | SQL used to create the table in the site database       | Required                                                                                                           |
| `fetch_all_sql`      | SQL used to fetch all rows from the site database table | Required                                                                                                           |
| `fetch_subset_sql`   | SQL used to fetch a filtered subset of rows             | Required                                                                                                           |
| `data_timestamp`     | Timestamp indicating when the data was generated        | Optional; defaults to the time of upload                                                                           |
| `csv_data`           | Raw CSV content with a header row                       | Required                                                                                                           |

## Admin Database Storage

Both the metadata and the raw CSV data are stored in a single table named `data_store` in the admin database:

```sql
CREATE TABLE data_store
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identifier         VARCHAR(255) NOT NULL UNIQUE,
    type               VARCHAR(100) NOT NULL,
    description        TEXT,
    column_definitions TEXT         NOT NULL,
    create_sql         TEXT         NOT NULL,
    fetch_all_sql      TEXT         NOT NULL,
    fetch_subset_sql   TEXT         NOT NULL,
    data_timestamp     TIMESTAMP    NOT NULL,
    csv_data           TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Publication Logic

When a data set is published, the following steps are performed:

1. The site database table name is generated as `website_data__<identifier>`.
   For example, an identifier of `cd_collection` produces the table name `website_data__cd_collection`.
2. If the table already exists in the site database, it is dropped (`DROP TABLE IF EXISTS`).
3. The table is created using the column definition from `create_sql`, but the table name is always forced to `website_data__<identifier>`.
4. The CSV data is parsed and each row is inserted into the newly created table.

## API Endpoints

All endpoints are under the path `/api/content-management/data` and require Bearer Authentication.

### List All Data Sets

```
GET /api/content-management/data
```

Returns a list of all data sets with their metadata but **without** the raw CSV data.

**Response:** `200 OK` – Array of `DataSummaryResponse` objects.

---

### Get a Data Set by Identifier

```
GET /api/content-management/data/{identifier}
```

Returns the metadata **and** the raw CSV data for the specified data set.

**Response:**
- `200 OK` – `DataResponse` object with all fields including `csv_data`.
- `404 Not Found` – If no data set with the given identifier exists.

---

### Create a New Data Set

```
POST /api/content-management/data
Content-Type: application/json
```

Stores new metadata and CSV data.

**Response:**
- `200 OK` – `DataResponse` object for the newly created data set.
- `400 Bad Request` – If the request is malformed (e.g. invalid identifier format, missing required fields).
- `409 Conflict` – If a data set with the same identifier already exists.

---

### Update an Existing Data Set

```
PUT /api/content-management/data
Content-Type: application/json
```

Replaces the metadata and CSV data of an existing data set identified by `identifier`.

**Response:**
- `200 OK` – `DataResponse` object for the updated data set.
- `400 Bad Request` – If the request is malformed.
- `404 Not Found` – If no data set with the given identifier exists.

---

### Publish a Data Set to the Site Database

```
POST /api/content-management/data/{identifier}/publish
```

Triggers the publication of the specified data set to the site database.

**Response:**
- `200 OK` – `DataResponse` object for the published data set.
- `404 Not Found` – If no data set with the given identifier exists.
- `500 Internal Server Error` – If the publication fails.

---

## Example

### Example CSV Data

```csv
title,artist,year,genre
Abbey Road,The Beatles,1969,Rock
OK Computer,Radiohead,1997,Alternative
Kind of Blue,Miles Davis,1959,Jazz
Thriller,Michael Jackson,1982,Pop
```

### Example Metadata Request Body (Create/Update)

```json
{
  "identifier": "cd_collection",
  "type": "tabulated",
  "description": "Personal music CD collection",
  "column_definitions": "[{\"name\":\"title\",\"type\":\"string\"},{\"name\":\"artist\",\"type\":\"string\"},{\"name\":\"year\",\"type\":\"integer\"},{\"name\":\"genre\",\"type\":\"string\"}]",
  "create_sql": "CREATE TABLE website_data__cd_collection (id BIGSERIAL PRIMARY KEY, title VARCHAR(255) NOT NULL, artist VARCHAR(255), year INTEGER, genre VARCHAR(100))",
  "fetch_all_sql": "SELECT id, title, artist, year, genre FROM website_data__cd_collection ORDER BY year, title",
  "fetch_subset_sql": "SELECT id, title, artist, year, genre FROM website_data__cd_collection WHERE genre = :genre ORDER BY year, title",
  "data_timestamp": "2024-06-01T12:00:00Z",
  "csv_data": "title,artist,year,genre\nAbbey Road,The Beatles,1969,Rock\nOK Computer,Radiohead,1997,Alternative\nKind of Blue,Miles Davis,1959,Jazz\nThriller,Michael Jackson,1982,Pop"
}
```

### Example SQL Queries

#### Create Table

```sql
CREATE TABLE website_data__cd_collection
(
    id     BIGSERIAL PRIMARY KEY,
    title  VARCHAR(255) NOT NULL,
    artist VARCHAR(255),
    year   INTEGER,
    genre  VARCHAR(100)
);
```

#### Fetch All Rows

```sql
SELECT id, title, artist, year, genre
FROM website_data__cd_collection
ORDER BY year, title;
```

#### Fetch a Subset (e.g. filter by genre)

```sql
SELECT id, title, artist, year, genre
FROM website_data__cd_collection
WHERE genre = 'Rock'
ORDER BY year, title;
```

#### Fetch a Subset (e.g. filter by year range)

```sql
SELECT id, title, artist, year, genre
FROM website_data__cd_collection
WHERE year BETWEEN 1960 AND 1999
ORDER BY year, title;
```

## Identifier Validation Rules

The `identifier` field is used both as the logical name and as part of the site database table name.
It must conform to the following rules:

- Must begin with a **lowercase letter** (`a`–`z`).
- May contain **lowercase letters**, **digits** (`0`–`9`), and **underscores** (`_`).
- **No spaces, hyphens, uppercase letters, or special characters** are allowed.
- Regex: `^[a-z][a-z0-9_]*$`

Valid examples: `cd_collection`, `temperature2024`, `sales_q1`

Invalid examples: `1data`, `MyData`, `data-set`, `data set`

## CSV Format Notes

- The **first row** must be the header row containing column names.
- Column names must contain only **letters, digits, and underscores**, and must start with a **letter or underscore**.
- Values **may be quoted** with double-quotes (`"`); embedded quotes are escaped by doubling them (`""`).
- Empty lines are ignored during import.
- If a data row has a different number of columns than the header, publication fails with `400 Bad Request`.
