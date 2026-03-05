# Spring Boot Library for integration with Istio on Kubernetes [![Twitter](https://img.shields.io/twitter/follow/piotr_minkowski.svg?style=social&logo=twitter&label=Follow%20Me)](https://twitter.com/piotr_minkowski)

![Maven Central Version](https://img.shields.io/maven-central/v/com.github.piomin/istio-spring-boot-starter)
[![CircleCI](https://circleci.com/gh/piomin/spring-boot-istio.svg?style=svg)](https://circleci.com/gh/piomin/spring-boot-istio)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-black.svg)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=bugs)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=coverage)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=ncloc)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)

## Main Purpose

This library is dedicated for Spring Boot application. Once it is included and enabled it is creating Istio resources on the current Kubernetes cluster basing on the code and annotation fields `@EnableIstio`.

## Getting Started

### Prerequisites

- Java 17 or higher
- Kubernetes cluster with Istio installed
- kubectl configured to access your cluster

### Installation

Add the dependency to your Maven `pom.xml`:

```xml
<dependency>
  <groupId>com.github.piomin</groupId>
  <artifactId>istio-spring-boot-starter</artifactId>
  <version>1.2.2</version>
</dependency>
```

## Usage

The library provides autoconfigured support for creating Istio resources on Kubernetes basing on annotation `@EnableIstio`.
```java
@SpringBootApplication
@EnableIstio(version = "v1")
public class CallmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallmeApplication.class, args);
	}
	
}
```

We can enable additional things. For example, we can enable `Gateway` and `VirtualService` with fault injection or matches in `VirtualService`.
```java
@SpringBootApplication
@EnableIstio(enableGateway = true,
        fault = @Fault(type = FaultType.ABORT, percentage = 50),
        matches = { @Match("/hello"), @Match("/hello2") })
public class SampleAppWithIstio {
    public static void main(String[] args) {
        SpringApplication.run(SampleAppWithIstio.class, args);
    }
}
```

The `@EnableIstio` annotation provides the following configuration options:

| Parameter              | Type    | Default | Description                                            |
|------------------------|---------|---------|--------------------------------------------------------|
| `version`              | String  | -       | (Required) Version label for the service               |
| `numberOfRetries`      | int     | 3       | Number of retries for failed requests                  |
| `timeout`              | int     | 6000    | Request timeout in milliseconds (0 means no timeout)   |
| `circuitBreakerErrors` | String  | -       | Number of errors in row to trip the circuit breaker    |
| `weight`               | String  | 100     | A weight ot path in load balancing                     |
| `enableGateway`        | boolean | false   | Enable Istio `Gateway` generation                      |
| `fault`                | Fault   | @Fault  | Enable Istio fault (delay. abort) injection            |
| `matches`              | Match[] | {}      | Enable multiple matches (e.g. uri, headers) generation |
| `domain`               | String  | ext     | The name of domain used host                           |

### Spring Configuration Properties

All `@EnableIstio` fields can be overridden via `application.properties` / `application.yml` using the prefix `istio.spring`. Properties take precedence over annotation values when set.

| Property                             | Type    | Description                                  |
|--------------------------------------|---------|----------------------------------------------|
| `istio.spring.timeout`               | Integer | Request timeout in milliseconds              |
| `istio.spring.version`               | String  | Version label for the service                |
| `istio.spring.weight`                | Integer | Weight for load balancing (0–100)            |
| `istio.spring.number-of-retries`     | Integer | Number of retries for failed requests        |
| `istio.spring.circuit-breaker-errors`| Integer | Consecutive 5xx errors to trip circuit breaker |
| `istio.spring.enable-gateway`        | Boolean | Enable Istio Gateway generation              |
| `istio.spring.domain`                | String  | Domain name for gateway hosts                |
| `istio.spring.fault.type`            | FaultType | Fault type: `ABORT` or `DELAY`             |
| `istio.spring.fault.percentage`      | Integer | Fault injection percentage (0–100)           |
| `istio.spring.fault.http-status`     | Integer | HTTP status code for ABORT fault             |
| `istio.spring.fault.delay`           | Long    | Delay in milliseconds for DELAY fault        |
| `istio.spring.matches[n].value`      | String  | Match value (e.g. URI path)                  |
| `istio.spring.matches[n].type`       | MatchType | Match type: `URI`, `HEADERS`, `METHOD`, etc. |
| `istio.spring.matches[n].mode`       | MatchMode | Match mode: `PREFIX`, `EXACT`, `REGEX`     |
| `istio.spring.matches[n].key`        | String  | Key for header/query/label matches           |

Example:
```yaml
istio:
  spring:
    version: v1
    timeout: 3000
    number-of-retries: 2
    enable-gateway: true
    domain: example.com
    fault:
      type: ABORT
      percentage: 10
      http-status: 503
```

### How It Works

The library automatically creates the following Istio resources during application startup:

`DestinationRule`: Defines policies for traffic routing, including load balancing and connection pool settings.\
`VirtualService`: Configures request routing, retries, timeouts, matches, and fault injection.
`Gateway`: Exposes the service to external traffic.

Here's the architecture of presented solution. Spring Boot Istio Library is included to the target application. It uses Java Istio Client to communication with istiod. During application startup the library is communicating with Istio API in order to create `DestinationRule` and `VirtualService` objects.

<img src="https://piotrminkowski.files.wordpress.com/2020/06/spring-boot-istio-arch-2.png" title="Architecture"><br/>