# Seed data

`src/main/resources/data/reports.json` is a JSON array of reports. Ids run from `RPT-1` through `RPT-30`. `ReportSeed` reads the file with the application `JsonMapper` when the repository is created.

The location is `reporter.seed.location` in `application.yml`, defaulting to `classpath:data/reports.json`. A missing or invalid file fails startup. After that, the data lives only in memory. Restarting the process loads the file again and drops anything created during the previous run.

`RPT-1` is titled `Login`, written by `john.doe`, so this query matches it:

```text
title = Login AND author = 'john.doe'
```

See [search query](search-query.md).
