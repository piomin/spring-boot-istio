package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import com.github.piomin.springboot.istio.service.IstioService;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.FaultType;
import io.fabric8.istio.api.api.networking.v1alpha3.*;
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

    // --- Application name and derived names ---

    @Test
    public void shouldReturnApplicationName() {
        assertEquals("test1", istioService.getApplicationName());
    }

    @Test
    public void shouldReturnDestinationRuleName() {
        assertEquals("test1-destination", istioService.getDestinationRuleName());
    }

    @Test
    public void shouldReturnVirtualServiceName() {
        assertEquals("test1-route", istioService.getVirtualServiceName());
    }

    // --- Metadata ---

    @Test
    public void shouldBuildDestinationRuleMetadata() {
        ObjectMeta meta = istioService.buildDestinationRuleMetadata();
        assertNotNull(meta);
        assertEquals("test1-destination", meta.getName());
    }

    // --- Retry ---

    @Test
    public void shouldReturnNullRetryWhenRetriesIsZero() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, null, false);
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNull(retry);
    }

    @Test
    public void shouldBuildRetryWithPerTryTimeout() {
        EnableIstio enableIstio = createEnableIstio(10000, 3, "", 100, null, null, false);
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(3), retry.getAttempts());
        assertEquals("3s", retry.getPerTryTimeout());
        assertEquals("5xx", retry.getRetryOn());
    }

    @Test
    public void shouldBuildRetryWithNullPerTryTimeoutWhenTimeoutIsZero() {
        EnableIstio enableIstio = createEnableIstio(0, 3, "", 100, null, null, false);
        HTTPRetry retry = istioService.buildRetry(enableIstio);
        assertNotNull(retry);
        assertEquals(Integer.valueOf(3), retry.getAttempts());
        assertNull(retry.getPerTryTimeout());
    }

    // --- Destination ---

    @Test
    public void shouldBuildDestinationWithVersion() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", 100, null, null, false);
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("v1", dest.getSubset());
        assertEquals("test1", dest.getHost());
    }

    @Test
    public void shouldBuildDestinationWithEmptyVersion() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, null, false);
        Destination dest = istioService.buildDestination(enableIstio);
        assertNotNull(dest);
        assertEquals("", dest.getSubset());
        assertEquals("test1", dest.getHost());
    }

    // --- Subset ---

    @Test
    public void shouldBuildSubsetWithVersionLabel() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v2", 100, null, null, false);
        Subset subset = istioService.buildSubset(enableIstio);
        assertNotNull(subset);
        assertEquals("v2", subset.getName());
        assertEquals("v2", subset.getLabels().get("version"));
    }

    // --- Circuit Breaker ---

    @Test
    public void shouldBuildCircuitBreakerWithoutOutlierDetection() {
        EnableIstio enableIstio = createEnableIstioWithCircuitBreaker(0);
        TrafficPolicy policy = istioService.buildCircuitBreaker(enableIstio);
        assertNotNull(policy);
        assertNotNull(policy.getConnectionPool());
        assertNull(policy.getOutlierDetection());
    }

    @Test
    public void shouldBuildCircuitBreakerWithOutlierDetection() {
        EnableIstio enableIstio = createEnableIstioWithCircuitBreaker(5);
        TrafficPolicy policy = istioService.buildCircuitBreaker(enableIstio);
        assertNotNull(policy);
        assertNotNull(policy.getConnectionPool());
        assertNotNull(policy.getOutlierDetection());
        assertEquals(5, policy.getOutlierDetection().getConsecutive5xxErrors());
        assertEquals("30s", policy.getOutlierDetection().getBaseEjectionTime());
        assertEquals(100, policy.getOutlierDetection().getMaxEjectionPercent());
    }

    // --- Route Destination ---

    @Test
    public void shouldBuildRouteDestinationWithWeight() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", 80, null, null, false);
        HTTPRouteDestination routeDest = istioService.buildRouteDestination(enableIstio);
        assertNotNull(routeDest);
        assertEquals(80, routeDest.getWeight());
        assertNotNull(routeDest.getDestination());
        assertEquals("v1", routeDest.getDestination().getSubset());
        assertEquals("test1", routeDest.getDestination().getHost());
    }

    // --- Route ---

    @Test
    public void shouldBuildRouteWithAllFields() {
        Match match = createMatch("/api", MatchType.URI, MatchMode.PREFIX, "");
        Fault fault = createFault(FaultType.ABORT, 50, 503, 0);
        EnableIstio enableIstio = createEnableIstio(6000, 3, "v1", 100, fault, match, false);
        HTTPRoute route = istioService.buildRoute(enableIstio);
        assertNotNull(route);
        assertNotNull(route.getRoute());
        assertFalse(route.getRoute().isEmpty());
        assertNotNull(route.getRetries());
        assertNotNull(route.getFault());
        assertEquals("6s", route.getTimeout());
    }

    @Test
    public void shouldBuildRouteWithoutFaultWhenPercentageIsZero() {
        Match match = createMatch("/api", MatchType.URI, MatchMode.PREFIX, "");
        Fault fault = createFault(FaultType.ABORT, 0, 500, 0);
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", 100, fault, match, false);
        HTTPRoute route = istioService.buildRoute(enableIstio);
        assertNotNull(route);
        assertNull(route.getFault());
        assertNull(route.getTimeout());
    }

    // --- Match types ---

    @Test
    public void shouldBuildUriPrefixMatch() {
        Match match = createMatch("/hello", MatchType.URI, MatchMode.PREFIX, "");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getUri());
        assertEquals(StringMatchPrefix.class, matchReq.getUri().getMatchType().getClass());
        assertEquals("/hello", ((StringMatchPrefix) matchReq.getUri().getMatchType()).getPrefix());
    }

    @Test
    public void shouldBuildUriExactMatch() {
        Match match = createMatch("/hello", MatchType.URI, MatchMode.EXACT, "");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getUri());
        assertEquals(StringMatchExact.class, matchReq.getUri().getMatchType().getClass());
        assertEquals("/hello", ((StringMatchExact) matchReq.getUri().getMatchType()).getExact());
    }

    @Test
    public void shouldBuildUriRegexMatch() {
        Match match = createMatch("/hello.*", MatchType.URI, MatchMode.REGEX, "");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getUri());
        assertEquals(StringMatchRegex.class, matchReq.getUri().getMatchType().getClass());
        assertEquals("/hello.*", ((StringMatchRegex) matchReq.getUri().getMatchType()).getRegex());
    }

    @Test
    public void shouldBuildHeadersMatch() {
        Match match = createMatch("test-value", MatchType.HEADERS, MatchMode.EXACT, "x-custom-header");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getHeaders());
        assertTrue(matchReq.getHeaders().containsKey("x-custom-header"));
    }

    @Test
    public void shouldBuildMethodMatch() {
        Match match = createMatch("GET", MatchType.METHOD, MatchMode.EXACT, "");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getMethod());
    }

    @Test
    public void shouldBuildQueryParamsMatch() {
        Match match = createMatch("bar", MatchType.QUERY_PARAMS, MatchMode.EXACT, "foo");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getQueryParams());
        assertTrue(matchReq.getQueryParams().containsKey("foo"));
    }

    @Test
    public void shouldBuildSourceLabelsMatch() {
        Match match = createMatch("v1", MatchType.SOURCE_LABELS, MatchMode.EXACT, "version");
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertNotNull(matchReq.getSourceLabels());
        assertEquals("v1", matchReq.getSourceLabels().get("version"));
    }

    @Test
    public void shouldBuildMatchWithIgnoreUriCase() {
        Match match = createMatchWithIgnoreCase("/hello", true);
        HTTPMatchRequest matchReq = istioService.buildHTTPMatchRequest(match);
        assertNotNull(matchReq);
        assertTrue(matchReq.getIgnoreUriCase());
    }

    // --- Fault injection ---

    @Test  
    public void shouldBuildAbortFaultInjection() {
        Fault fault = createFault(FaultType.ABORT, 100, 500, 0);
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", 100, fault, null, false);
        HTTPFaultInjection faultInjection = istioService.buildFault(enableIstio);
        assertNotNull(faultInjection);
        assertNotNull(faultInjection.getAbort());
        assertNull(faultInjection.getDelay());
        assertEquals(HTTPFaultInjectionAbortHttpStatus.class, faultInjection.getAbort().getErrorType().getClass());
        assertEquals(500, ((HTTPFaultInjectionAbortHttpStatus) faultInjection.getAbort().getErrorType()).getHttpStatus());
        assertEquals(100.0, faultInjection.getAbort().getPercentage().getValue());
    }

    @Test
    public void shouldBuildDelayFaultInjection() {
        Fault fault = createFault(FaultType.DELAY, 50, 500, 2000);
        EnableIstio enableIstio = createEnableIstio(0, 0, "v1", 100, fault, null, false);
        HTTPFaultInjection faultInjection = istioService.buildFault(enableIstio);
        assertNotNull(faultInjection);
        assertNotNull(faultInjection.getDelay());
        assertNull(faultInjection.getAbort());
        assertEquals(HTTPFaultInjectionDelayFixedDelay.class, faultInjection.getDelay().getHttpDelayType().getClass());
        assertEquals(50.0, faultInjection.getDelay().getPercentage().getValue());
    }

    // --- Annotation fallback values ---

    @Test
    public void shouldReturnAnnotationTimeoutWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(8000, 0, "", 100, null, null, false);
        assertEquals(8000, istioService.getTimeout(enableIstio));
    }

    @Test
    public void shouldReturnAnnotationVersionWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "v3", 100, null, null, false);
        assertEquals("v3", istioService.getVersion(enableIstio));
    }

    @Test
    public void shouldReturnAnnotationWeightWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 75, null, null, false);
        assertEquals(75, istioService.getWeight(enableIstio));
    }

    @Test
    public void shouldReturnAnnotationNumberOfRetriesWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 5, "", 100, null, null, false);
        assertEquals(5, istioService.getNumberOfRetries(enableIstio));
    }

    @Test
    public void shouldReturnAnnotationEnableGatewayWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, null, true);
        assertTrue(istioService.isEnableGateway(enableIstio));
    }

    @Test
    public void shouldReturnAnnotationDomainWhenNoProperties() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, null, false);
        assertEquals("ext", istioService.getDomain(enableIstio));
    }

    @Test
    public void shouldReturnFaultPercentageFromAnnotation() {
        Fault fault = createFault(FaultType.ABORT, 30, 500, 0);
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, fault, null, false);
        assertEquals(30, istioService.getFaultPercentage(enableIstio));
    }

    @Test
    public void shouldReturnZeroFaultPercentageWhenFaultIsNull() {
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, null, false);
        assertEquals(0, istioService.getFaultPercentage(enableIstio));
    }

    @Test
    public void shouldReturnMatchesFromAnnotation() {
        Match match = createMatch("/test", MatchType.URI, MatchMode.PREFIX, "");
        EnableIstio enableIstio = createEnableIstio(0, 0, "", 100, null, match, false);
        Match[] matches = istioService.getMatches(enableIstio);
        assertEquals(1, matches.length);
        assertEquals("/test", matches[0].value());
    }

    // --- Helper methods ---

    private EnableIstio createEnableIstio(int timeout, int numberOfRetries, String version,
                                           int weight, Fault fault, Match match, boolean enableGateway) {
        return new EnableIstio() {
            @Override public Class<? extends Annotation> annotationType() { return EnableIstio.class; }
            @Override public int timeout() { return timeout; }
            @Override public String version() { return version; }
            @Override public int weight() { return weight; }
            @Override public int numberOfRetries() { return numberOfRetries; }
            @Override public int circuitBreakerErrors() { return 0; }
            @Override public Match[] matches() { return match != null ? new Match[] { match } : new Match[0]; }
            @Override public Fault fault() { return fault; }
            @Override public boolean enableGateway() { return enableGateway; }
            @Override public String domain() { return "ext"; }
        };
    }

    private EnableIstio createEnableIstioWithCircuitBreaker(int circuitBreakerErrors) {
        return new EnableIstio() {
            @Override public Class<? extends Annotation> annotationType() { return EnableIstio.class; }
            @Override public int timeout() { return 0; }
            @Override public String version() { return "v1"; }
            @Override public int weight() { return 100; }
            @Override public int numberOfRetries() { return 0; }
            @Override public int circuitBreakerErrors() { return circuitBreakerErrors; }
            @Override public Match[] matches() { return new Match[0]; }
            @Override public Fault fault() { return null; }
            @Override public boolean enableGateway() { return false; }
            @Override public String domain() { return "ext"; }
        };
    }

    private Match createMatch(String value, MatchType type, MatchMode mode, String key) {
        return new Match() {
            @Override public Class<? extends Annotation> annotationType() { return Match.class; }
            @Override public boolean ignoreUriCase() { return false; }
            @Override public MatchType type() { return type; }
            @Override public MatchMode mode() { return mode; }
            @Override public String value() { return value; }
            @Override public String key() { return key; }
        };
    }

    private Match createMatchWithIgnoreCase(String value, boolean ignoreCase) {
        return new Match() {
            @Override public Class<? extends Annotation> annotationType() { return Match.class; }
            @Override public boolean ignoreUriCase() { return ignoreCase; }
            @Override public MatchType type() { return MatchType.URI; }
            @Override public MatchMode mode() { return MatchMode.PREFIX; }
            @Override public String value() { return value; }
            @Override public String key() { return ""; }
        };
    }

    private Fault createFault(FaultType faultType, int percentage, int httpStatus, long delay) {
        return new Fault() {
            @Override public Class<? extends Annotation> annotationType() { return Fault.class; }
            @Override public FaultType type() { return faultType; }
            @Override public int percentage() { return percentage; }
            @Override public int httpStatus() { return httpStatus; }
            @Override public long delay() { return delay; }
        };
    }
}
