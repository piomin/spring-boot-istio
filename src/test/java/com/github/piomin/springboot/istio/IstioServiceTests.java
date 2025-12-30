package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.networking.v1beta1.Destination;
import io.fabric8.istio.api.networking.v1beta1.HTTPMatchRequest;
import io.fabric8.istio.api.networking.v1beta1.HTTPRetry;
import io.fabric8.istio.api.networking.v1beta1.StringMatchPrefix;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.FaultType;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.networking.v1beta1.*;
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
        EnableIstio enableIstio = createEnableIstio(0, 0, "", null, null);
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNull(retry);
    }

    @Test
    public void buildRetry() {
        EnableIstio enableIstio = createEnableIstio(10, 3, "", null, null);
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(3), retry.getAttempts());
        assertEquals("3s", retry.getPerTryTimeout());
    }

    @Test
    public void buildDestination() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", null, null);
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("v1", dest.getSubset());
        assertEquals("test1", dest.getHost());
    }

    @Test
    public void buildMatch() {
        Match match = createMatch("/hello");
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", null, match);
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(enableIstio.matches()[0]);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getUri());
        assertEquals(StringMatchPrefix.class, matchReq.getUri().getMatchType().getClass());
        assertEquals("/hello", ((StringMatchPrefix) matchReq.getUri().getMatchType()).getPrefix());
    }
    
    @Test  
    public void buildFaultAbort() {
        Fault fault = createFault(FaultType.ABORT);
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", fault, null);
        HTTPFaultInjection faultInjection = istioService.buildFault(enableIstio);
        assertNotNull(faultInjection);
        assertNotNull(faultInjection.getAbort());
        assertEquals(HTTPFaultInjectionAbortHttpStatus.class, faultInjection.getAbort().getErrorType().getClass());
    }

    @Test
    public void buildFaultDelay() {
        Fault fault = createFault(FaultType.DELAY);
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", fault, null);
        HTTPFaultInjection faultInjection = istioService.buildFault(enableIstio);
        assertNotNull(faultInjection);
        assertNotNull(faultInjection.getDelay());
        assertEquals(HTTPFaultInjectionDelayFixedDelay.class, faultInjection.getDelay().getHttpDelayType().getClass());
    }

    private EnableIstio createEnableIstio(int timeout, int numberOfRetries, String version, Fault fault, Match match) {
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

            @Override
            public Match[] matches() {
                return new Match[] { match };
            }
          
            @Override
            public Fault fault() {
                return fault;
            }
          
            @Override
            public boolean enableGateway() {
                return false;
            }
        };
    }

    private Match createMatch(String value) {
        return new Match() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Match.class;
            }

            @Override
            public boolean ignoreUriCase() {
                return false;
            }

            @Override
            public MatchType type() {
                return MatchType.URI;
            }

            @Override
            public MatchMode mode() {
                return MatchMode.PREFIX;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String key() {
                return "";
            }

        };
    }

    private Fault createFault(FaultType faultType) {
        return new Fault() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Fault.class;
            }

            @Override
            public FaultType type() {
                return faultType;
            }

            @Override
            public int percentage() {
                return 100;
            }

            @Override
            public int httpStatus() {
                return 500;
            }

            @Override
            public long delay() {
                return 1000;
            }
        };
    }
}
