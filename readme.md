# Spring Boot Library for integration with Istio on Kubernetes

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.piomin/istio-spring-boot-starter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.piomin/istio-spring-boot-starter)
[![CircleCI](https://circleci.com/gh/piomin/spring-boot-logging.svg?style=shield)](https://circleci.com/gh/piomin/spring-boot-istio)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=alert_status)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=piomin_spring-boot-istio&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=piomin_spring-boot-istio)

## Main Purpose

This library is dedicated for Spring Boot application. Once it is included and enabled it is creating Istio resources on the current Kubernetes cluster basing on the code and annotation fields `@EnableIstio`.

## Getting Started

The library is published on Maven Central. Current version is `0.1.1.RELEASE`
```
<dependency>
  <groupId>com.github.piomin</groupId>
  <artifactId>istio-spring-boot-starter</artifactId>
  <version>0.1.1.RELEASE</version>
</dependency>
```

The library provides auto-configured support for creating Istio resources on Kubernetes basing on annotation `@EnableIstio`.
```
@SpringBootApplication
@EnableIstio(version = "v1", retries = 3, timeout = 3)
public class CallmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallmeApplication.class, args);
	}
	
}
```

## Usage

Here's the architecture of presented solution. Spring Boot Istio Library is included to the target application. It uses Java Istio Client to communication with istiod. During application startup the library is communicating with Istio API in order to create `DestinationRule` and `VirtualService` objects.

<img src="https://piotrminkowski.files.wordpress.com/2020/06/spring-boot-istio-arch-2.png" title="Architecture"><br/>