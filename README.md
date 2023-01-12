[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Coverage Status](https://coveralls.io/repos/github/creek-service/creek-platform/badge.svg?branch=main)](https://coveralls.io/github/creek-service/creek-platform?branch=main)
[![build](https://github.com/creek-service/creek-platform/actions/workflows/build.yml/badge.svg)](https://github.com/creek-service/creek-platform/actions/workflows/build.yml)
[![CodeQL](https://github.com/creek-service/creek-platform/actions/workflows/codeql.yml/badge.svg)](https://github.com/creek-service/creek-platform/actions/workflows/codeql.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.creekservice/creek-platform-metadata.svg)](https://central.sonatype.dev/search?q=creek-platform-*)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-platform/badge)](https://api.securityscorecards.dev/projects/github.com/creek-service/creek-platform)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/6899/badge)](https://bestpractices.coreinfrastructure.org/projects/6899)

# Creek Platform

This repository defines base types, generally interfaces, used to define architectural components, 
such as a 'service' or an 'aggregate', and the resources they use.  Additionally, it defines
a library for inspecting components, extracting resources and determining which resources 
need initialising for which components.

Users of Creek will implement the service and aggregate descriptor interfaces defined in this repository.
They should not directly implement the base resource interfaces. These are designed to be extended by 
extensions to Creek.

See [CreekService.org](https://www.creekservice.org) for info on Creek Service.

## Modules

The repo contains the following modules:

* [metadata](metadata): A dependency-free library that defines the interfaces used to describe platform components and resources.
* [resource](resource): A common library for working with descriptors and resources.
