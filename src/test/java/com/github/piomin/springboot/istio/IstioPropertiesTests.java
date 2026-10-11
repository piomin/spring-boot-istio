package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import com.github.piomin.springboot.istio.config.IstioProperties;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.api.networking.v1alpha3.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {IstioService.class, IstioPropertiesTests.Config.class},
        properties = {
                "spring.application.name=test1",
                "istio.spring.timeout=5000",
                "istio.spring.number-of-retries=5",
                "istio.spring.version=v2",
                "istio.spring.weight=80",
                "istio.spring.circuit-breaker-errors=10",
                "istio.spring.fault.type=ABORT",
                "istio.spring.fault.percentage=75",
                "istio.spring.fault.http-status=503",
                "istio.spring.enable-gateway=true",
                "istio.spring.domain=prod",
                "istio.spring.matches[0].type=URI",
                "istio.spring.matches[0].mode=PREFIX",
                "istio.spring.matches[0].value=/api",
                "istio.spring.matches[1].type=HEADERS",
                "istio.spring.matches[1].mode=EXACT",
                "istio.spring.matches[1].value=test-value",
                "istio.spring.matches[1].key=x-custom"
        })
public class IstioPropertiesTests {

    @Configuration
    @EnableConfigurationProperties(IstioProperties.class)
    static class Config {}

    @Autowired
    IstioService istioService;

    @Test
    public void shouldOverrideTimeoutFromProperties() {
        EnableIstio enableIstio = createEnableIstio(6000, 3, "v1");
        assertEquals(5000, istioService.getTimeout(enableIstio));
    }

    @Test
    public void shouldOverrideNumberOfRetriesFromProperties() {
        EnableIstio enableIstio = createEnableIstio(5000, 3, "v1");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(5), retry.getAttempts());
        assertEquals("1s", retry.getPerTryTimeout());
    }

    @Test
    public void shouldOverrideVersionFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("v2", dest.getSubset());
    }

    @Test
    public void shouldOverrideFaultPercentageFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals(75, istioService.getFaultPercentage(enableIstio));
    }

    @Test
    public void shouldOverrideFaultInjectionFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        HTTPFaultInjection fault = istioService.buildFault(enableIstio);
        assertNotNull(fault);
        assertNotNull(fault.getAbort());
        assertEquals(HTTPFaultInjectionAbortHttpStatus.class, fault.getAbort().getErrorType().getClass());
        assertEquals(503, ((HTTPFaultInjectionAbortHttpStatus) fault.getAbort().getErrorType()).getHttpStatus());
    }

    @Test
    public void shouldOverrideEnableGatewayFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertTrue(istioService.isEnableGateway(enableIstio));
    }

    @Test
    public void shouldOverrideDomainFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals("prod", istioService.getDomain(enableIstio));
    }

    @Test
    public void shouldOverrideWeightFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals(80, istioService.getWeight(enableIstio));
    }

    @Test
    public void shouldOverrideCircuitBreakerErrorsFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals(10, istioService.getCircuitBreakerErrors(enableIstio));
    }

    @Test
    public void shouldBuildCircuitBreakerWithOutlierDetectionFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        TrafficPolicy policy = istioService.buildCircuitBreaker(enableIstio);
        assertNotNull(policy);
        assertNotNull(policy.getOutlierDetection());
        assertEquals(10, policy.getOutlierDetection().getConsecutive5xxErrors());
    }

    @Test
    public void shouldOverrideMatchesFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Match[] matches = istioService.getMatches(enableIstio);
        assertEquals(2, matches.length);

        assertEquals(MatchType.URI, matches[0].type());
        assertEquals(MatchMode.PREFIX, matches[0].mode());
        assertEquals("/api", matches[0].value());

        assertEquals(MatchType.HEADERS, matches[1].type());
        assertEquals(MatchMode.EXACT, matches[1].mode());
        assertEquals("test-value", matches[1].value());
        assertEquals("x-custom", matches[1].key());
    }

    @Test
    public void shouldBuildHTTPMatchRequestFromPropertiesMatches() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Match[] matches = istioService.getMatches(enableIstio);
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(matches[0]);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getUri());
        assertEquals(StringMatchPrefix.class, matchReq.getUri().getMatchType().getClass());
        assertEquals("/api", ((StringMatchPrefix) matchReq.getUri().getMatchType()).getPrefix());
    }

    @Test
    public void shouldBuildRouteDestinationWithPropertiesWeight() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        HTTPRouteDestination routeDest = istioService.buildRouteDestination(enableIstio);
        assertNotNull(routeDest);
        assertEquals(80, routeDest.getWeight());
    }

    private EnableIstio createEnableIstio(int timeout, int numberOfRetries, String version) {
        return new EnableIstio() {
            @Override public Class<? extends Annotation> annotationType() { return EnableIstio.class; }
            @Override public int timeout() { return timeout; }
            @Override public String version() { return version; }
            @Override public int weight() { return 100; }
            @Override public int numberOfRetries() { return numberOfRetries; }
            @Override public int circuitBreakerErrors() { return 0; }
            @Override public Match[] matches() { return new Match[0]; }
            @Override public Fault fault() { return null; }
            @Override public boolean enableGateway() { return false; }
            @Override public String domain() { return "ext"; }
        };
    }
}
