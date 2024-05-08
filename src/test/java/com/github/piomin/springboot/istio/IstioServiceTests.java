package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.networking.v1beta1.Destination;
import io.fabric8.istio.api.networking.v1beta1.HTTPRetry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = IstioService.class,
                properties = "spring.application.name=test1")
public class IstioServiceTests {

    @Autowired
    IstioService istioService;

    @Test
    public void buildDestinationRuleMetadata() {
        ObjectMeta meta = istioService.buildDestinationRuleMetadata();
        assertNotNull(meta);
        assertEquals("test1-destination", meta.getName());
    }

    @Test
    public void buildRetryNull() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNull(retry);
    }

    @Test
    public void buildRetry() {
        EnableIstio enableIstio = createEnableIstio(10, 3, "");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(3), retry.getAttempts());
        assertEquals("3s", retry.getPerTryTimeout());
    }

    @Test
    public void buildDestination() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("v1", dest.getSubset());
        assertEquals("test1", dest.getHost());
    }

    private EnableIstio createEnableIstio(int timeout, int numberOfRetries, String version) {
        return new EnableIstio() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return EnableIstio.class;
            }

            @Override
            public int timeout() {
                return timeout;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public int weight() {
                return 0;
            }

            @Override
            public int numberOfRetries() {
                return numberOfRetries;
            }

            @Override
            public int circuitBreakerErrors() {
                return 0;
            }
        };
    }
}
