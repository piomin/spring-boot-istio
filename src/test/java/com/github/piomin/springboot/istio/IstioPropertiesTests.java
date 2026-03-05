package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.config.IstioProperties;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.api.networking.v1alpha3.Destination;
import io.fabric8.istio.api.api.networking.v1alpha3.HTTPFaultInjection;
import io.fabric8.istio.api.api.networking.v1alpha3.HTTPFaultInjectionAbortHttpStatus;
import io.fabric8.istio.api.api.networking.v1alpha3.HTTPRetry;
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
                "istio.spring.fault.type=ABORT",
                "istio.spring.fault.percentage=75",
                "istio.spring.fault.http-status=503",
                "istio.spring.enable-gateway=true",
                "istio.spring.domain=prod"
        })
public class IstioPropertiesTests {

    @Configuration
    @EnableConfigurationProperties(IstioProperties.class)
    static class Config {}

    @Autowired
    IstioService istioService;

    @Test
    public void timeoutFromProperties() {
        EnableIstio enableIstio = createEnableIstio(6000, 3, "v1");
        assertEquals(5000, istioService.getTimeout(enableIstio));
    }

    @Test
    public void numberOfRetriesFromProperties() {
        EnableIstio enableIstio = createEnableIstio(5000, 3, "v1");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(5), retry.getAttempts());
        assertEquals("1s", retry.getPerTryTimeout());
    }

    @Test
    public void versionFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("v2", dest.getSubset());
    }

    @Test
    public void faultPercentageFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals(75, istioService.getFaultPercentage(enableIstio));
    }

    @Test
    public void faultInjectionFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        HTTPFaultInjection fault = istioService.buildFault(enableIstio);
        assertNotNull(fault);
        assertNotNull(fault.getAbort());
        assertEquals(HTTPFaultInjectionAbortHttpStatus.class, fault.getAbort().getErrorType().getClass());
        assertEquals(503, ((HTTPFaultInjectionAbortHttpStatus) fault.getAbort().getErrorType()).getHttpStatus());
    }

    @Test
    public void enableGatewayFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertTrue(istioService.isEnableGateway(enableIstio));
    }

    @Test
    public void domainFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals("prod", istioService.getDomain(enableIstio));
    }

    @Test
    public void weightFromProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        assertEquals(80, istioService.getWeight(enableIstio));
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
