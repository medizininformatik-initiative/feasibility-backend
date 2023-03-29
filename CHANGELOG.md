# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),

## [UNRELEASED] - yyyy-mm-dd

### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security

The full changelog can be found [here](https://todo).

## [3.0.0] - 2023-03-29

### Added
- Structured queries can be validated against the JSON schema via a REST endpoint ([#91](https://github.com/medizininformatik-initiative/feasibility-backend/pull/91))
- Limits on how often query results can be retrieved are imposed on users ([#77](https://github.com/medizininformatik-initiative/feasibility-backend/pull/77)), ([#69](https://github.com/medizininformatik-initiative/feasibility-backend/pull/69)), ([#94](https://github.com/medizininformatik-initiative/feasibility-backend/pull/94))
- Direct broker CQL compatibility ([#48](https://github.com/medizininformatik-initiative/feasibility-backend/pull/48))
### Changed
- Limit amount of queries a user can post before being locked out ([#101](https://github.com/medizininformatik-initiative/feasibility-backend/pull/101))
- Query results are no longer persisted but only kept in memory for a configurable time ([#62](https://github.com/medizininformatik-initiative/feasibility-backend/pull/62)), ([#80](https://github.com/medizininformatik-initiative/feasibility-backend/pull/80)), ([#87](https://github.com/medizininformatik-initiative/feasibility-backend/pull/87))
- Return restricted results if certain thresholds are not surpassed ([#63](https://github.com/medizininformatik-initiative/feasibility-backend/pull/63)), ([#64](https://github.com/medizininformatik-initiative/feasibility-backend/pull/64))
### Removed
* Remove obsolete REST endpoints under /api/v1/ ([#109](https://github.com/medizininformatik-initiative/feasibility-backend/pull/109))
### Fixed
- Fix codesystem alias for consent ([#85](https://github.com/medizininformatik-initiative/feasibility-backend/pull/85)).
### Security
* Update Spring Boot to v3.0.5 ([#104](https://github.com/medizininformatik-initiative/feasibility-backend/pull/104))
* Update HAPI to 6.4.2 ([#73](https://github.com/medizininformatik-initiative/feasibility-backend/pull/73))

The full changelog can be found [here](https://github.com/medizininformatik-initiative/feasibility-backend/milestone/4?closed=1).
