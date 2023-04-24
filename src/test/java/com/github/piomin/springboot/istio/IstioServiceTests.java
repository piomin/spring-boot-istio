package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import me.snowdrop.istio.api.Duration;
import me.snowdrop.istio.api.networking.v1beta1.Destination;
import me.snowdrop.istio.api.networking.v1beta1.HTTPRetry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.annotation.Annotation;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = IstioService.class,
                properties = "spring.application.name=test1")
public class IstioServiceTests {

    @Autowired
    IstioService istioService;

    @Test
    public void buildDestinationRuleMetadata() {
        ObjectMeta meta = istioService.buildDestinationRuleMetadata();
        Assert.assertNotNull(meta);
        Assert.assertEquals("test1-destination", meta.getName());
    }

    @Test
    public void buildRetryNull() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        Assert.assertNull(retry);
    }

    @Test
    public void buildRetry() {
        EnableIstio enableIstio = createEnableIstio(10, 3, "");
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        Assert.assertNotNull(retry);
        Assert.assertEquals(Integer.valueOf(3), retry.getAttempts());
        Assert.assertEquals(new Duration(0, 3L), retry.getPerTryTimeout());
    }

    @Test
    public void buildDestination() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1");
        Destination dest = istioService.buildDestination(enableIstio);
        Assert.assertNotNull(dest);
        Assert.assertEquals("v1", dest.getSubset());
        Assert.assertEquals("test1", dest.getHost());
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
