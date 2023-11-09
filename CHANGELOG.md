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

## [4.1.0] - 2023-11-09

### Added
- Make order of main categories configurable ([#219](https://github.com/medizininformatik-initiative/feasibility-backend/issues/219))
- Save and load query results for saved queries ([#199](https://github.com/medizininformatik-initiative/feasibility-backend/issues/199))
- Support CRUD for query templates ([#214](https://github.com/medizininformatik-initiative/feasibility-backend/issues/214))

### Fixed
- Fix code scanning alert - parser confusion leads to OOM ([#221](https://github.com/medizininformatik-initiative/feasibility-backend/issues/221))
- Fix code scanning alert - Missing Override annotation ([#223](https://github.com/medizininformatik-initiative/feasibility-backend/issues/223))
- Fix code scanning alert - Unread local variable ([#222](https://github.com/medizininformatik-initiative/feasibility-backend/issues/222))

### Security
- Update Spring Boot to 3.1.5 ([#227](https://github.com/medizininformatik-initiative/feasibility-backend/issues/227))

## [4.0.0] - 2023-10-06

### Added
- Support for self-signed certificates ([#203](https://github.com/medizininformatik-initiative/feasibility-backend/issues/203))
- New DB tables  ([#180](https://github.com/medizininformatik-initiative/feasibility-backend/issues/180))
- v3 api endpoint ([#190](https://github.com/medizininformatik-initiative/feasibility-backend/issues/190))
### Changed
- Ontology is loaded from GitHub ([#201](https://github.com/medizininformatik-initiative/feasibility-backend/issues/201))
- Error handling changed to provide more information to the GUI  ([#116](https://github.com/medizininformatik-initiative/feasibility-backend/issues/116))
### Removed
- v1 and v2 api endpoints ([#190](https://github.com/medizininformatik-initiative/feasibility-backend/issues/190))
### Security
- Update Spring Boot to 3.1.3 ([#188](https://github.com/medizininformatik-initiative/feasibility-backend/issues/188))

The full changelog can be found [here](https://github.com/medizininformatik-initiative/feasibility-backend/milestone/6?closed=1).

## [3.1.3] - 2023-07-13

### Changed
- Obfuscated site ids are no longer consistent over multiple requests of the same result
### Security
- Update Spring Boot to 3.1.1

## [3.1.2] - 2023-07-11

### Security
- Fix potential input resource leak ([#155](https://github.com/medizininformatik-initiative/feasibility-backend/issues/155))

## [3.1.1] - 2023-05-24

### Fixed
- Database configuration changed

## [3.1.0] - 2023-05-24

### Changed
- Result Log files are encrypted ([#124](https://github.com/medizininformatik-initiative/feasibility-backend/pull/124))
### Security
- Update Spring Boot to 3.1.0 ([#130](https://github.com/medizininformatik-initiative/feasibility-backend/pull/130))
- Update Spring Security to 6.1.0 ([#129](https://github.com/medizininformatik-initiative/feasibility-backend/pull/129))
- Update JSON lib to 20230227 ([#131](https://github.com/medizininformatik-initiative/feasibility-backend/pull/131))

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
