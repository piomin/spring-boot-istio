package com.github.piomin.springboot.istio.service;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import com.github.piomin.springboot.istio.annotation.FaultType;
import com.github.piomin.springboot.istio.config.IstioProperties;
import io.fabric8.istio.api.api.networking.v1alpha3.*;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

public class IstioService {

    private static final String RETRY_CODES = "5xx";
    private static final String CIRCUIT_OPEN_TIME = "30s";
    private static final int MAX_DISABLED_HOSTS_PERCENTAGE = 100;

    @Value("${spring.application.name}")
    private Optional<String> applicationName;

    @Autowired(required = false)
    private IstioProperties properties;

    public String getApplicationName() {
        return applicationName.orElse("default");
    }

    public String getDestinationRuleName() {
        return getApplicationName() + "-destination";
    }

    public String getVirtualServiceName() {
        return getApplicationName() + "-route";
    }

    public int getTimeout(EnableIstio e) {
        return properties != null && properties.getTimeout() != null ? properties.getTimeout() : e.timeout();
    }

    public String getVersion(EnableIstio e) {
        return properties != null && properties.getVersion() != null ? properties.getVersion() : e.version();
    }

    public int getWeight(EnableIstio e) {
        return properties != null && properties.getWeight() != null ? properties.getWeight() : e.weight();
    }

    public int getNumberOfRetries(EnableIstio e) {
        return properties != null && properties.getNumberOfRetries() != null ? properties.getNumberOfRetries() : e.numberOfRetries();
    }

    public int getCircuitBreakerErrors(EnableIstio e) {
        return properties != null && properties.getCircuitBreakerErrors() != null ? properties.getCircuitBreakerErrors() : e.circuitBreakerErrors();
    }

    public boolean isEnableGateway(EnableIstio e) {
        return properties != null && properties.getEnableGateway() != null ? properties.getEnableGateway() : e.enableGateway();
    }

    public String getDomain(EnableIstio e) {
        return properties != null && properties.getDomain() != null ? properties.getDomain() : e.domain();
    }

    public int getFaultPercentage(EnableIstio e) {
        if (properties != null && properties.getFault() != null && properties.getFault().getPercentage() != null)
            return properties.getFault().getPercentage();
        return e.fault() != null ? e.fault().percentage() : 0;
    }

    private FaultType getFaultType(EnableIstio e) {
        if (properties != null && properties.getFault() != null && properties.getFault().getType() != null)
            return properties.getFault().getType();
        return e.fault() != null ? e.fault().type() : FaultType.ABORT;
    }

    private int getFaultHttpStatus(EnableIstio e) {
        if (properties != null && properties.getFault() != null && properties.getFault().getHttpStatus() != null)
            return properties.getFault().getHttpStatus();
        return e.fault() != null ? e.fault().httpStatus() : 500;
    }

    private long getFaultDelay(EnableIstio e) {
        if (properties != null && properties.getFault() != null && properties.getFault().getDelay() != null)
            return properties.getFault().getDelay();
        return e.fault() != null ? e.fault().delay() : 0;
    }

    public Match[] getMatches(EnableIstio e) {
        if (properties != null && !properties.getMatches().isEmpty())
            return properties.getMatches().stream().map(this::toMatch).toArray(Match[]::new);
        return e.matches();
    }

    private Match toMatch(IstioProperties.MatchProperties mp) {
        return new Match() {
            @Override public Class<? extends Annotation> annotationType() { return Match.class; }
            @Override public boolean ignoreUriCase() { return mp.isIgnoreUriCase(); }
            @Override public MatchType type() { return mp.getType(); }
            @Override public MatchMode mode() { return mp.getMode(); }
            @Override public String value() { return mp.getValue(); }
            @Override public String key() { return mp.getKey(); }
        };
    }

    public ObjectMeta buildDestinationRuleMetadata() {
        return new ObjectMetaBuilder().withName(getDestinationRuleName()).build();
    }

    public TrafficPolicy buildCircuitBreaker(EnableIstio enableIstio) {
        TrafficPolicyBuilder builder = new TrafficPolicyBuilder()
                .withConnectionPool(new ConnectionPoolSettingsBuilder().withNewHttp().endHttp().build());
        if (getCircuitBreakerErrors(enableIstio) == 0)
            return builder.build();
        else return builder.withOutlierDetection(new OutlierDetectionBuilder()
                        .withConsecutive5xxErrors(getCircuitBreakerErrors(enableIstio))
                        .withBaseEjectionTime(CIRCUIT_OPEN_TIME)
                        .withMaxEjectionPercent(MAX_DISABLED_HOSTS_PERCENTAGE)
                        .build())
                .build();
    }

    public Subset buildSubset(EnableIstio enableIstio) {
        return new SubsetBuilder()
                .withName(getVersion(enableIstio))
                .addToLabels("version", getVersion(enableIstio))
                .build();
    }

    public HTTPMatchRequest buildHTTPMatchRequest(Match match) {
        HTTPMatchRequestBuilder builder = new HTTPMatchRequestBuilder();
        StringMatchBuilder matchBuilder = new StringMatchBuilder();
        if (match.mode().equals(MatchMode.EXACT))
            matchBuilder = matchBuilder.withNewStringMatchExactType(match.value());
        else if (match.mode().equals(MatchMode.PREFIX))
            matchBuilder = matchBuilder.withNewStringMatchPrefixType(match.value());
        else
            matchBuilder = matchBuilder.withNewStringMatchRegexType(match.value());
        if (match.type() == MatchType.URI)
            builder = builder.withUri(matchBuilder.build());
        else if (match.type() == MatchType.HEADERS)
            builder = builder.withHeaders(Map.of(match.key(), matchBuilder.build()));
        else if (match.type() == MatchType.METHOD)
            builder = builder.withMethod(matchBuilder.build());
        else if (match.type() == MatchType.GATEWAYS)
            builder = builder.withGateways(match.value());
        else if (match.type() == MatchType.QUERY_PARAMS)
            builder = builder.withQueryParams(Map.of(match.key(), matchBuilder.build()));
        else
            builder = builder.withSourceLabels(Map.of(match.key(), match.value()));
        return builder.withIgnoreUriCase(match.ignoreUriCase()).build();
    }

    public HTTPRetry buildRetry(EnableIstio enableIstio) {
        if (getNumberOfRetries(enableIstio) == 0)
            return null;
        long ratioTimeout = Math.round((float) getTimeout(enableIstio) / getNumberOfRetries(enableIstio));
        return new HTTPRetryBuilder()
                .withAttempts(getNumberOfRetries(enableIstio))
                .withRetryOn(RETRY_CODES)
                .withPerTryTimeout(getTimeout(enableIstio) != 0 ?
                        formatDuration(ratioTimeout, "s's'") : null)
                .build();
    }

    public Destination buildDestination(EnableIstio enableIstio) {
        return new DestinationBuilder()
                .withHost(getApplicationName())
                .withSubset(getVersion(enableIstio))
                .build();
    }

    public HTTPRouteDestination buildRouteDestination(EnableIstio enableIstio) {
        return new HTTPRouteDestinationBuilder()
                .withDestination(buildDestination(enableIstio))
                .withWeight(getWeight(enableIstio))
                .build();
    }

    public HTTPFaultInjection buildFault(EnableIstio enableIstio) {
        if (getFaultType(enableIstio).equals(FaultType.ABORT))
            return new HTTPFaultInjectionBuilder()
                    .withNewAbort()
                    .withNewPercentage()
                        .withValue((double) getFaultPercentage(enableIstio))
                    .endPercentage()
                    .withNewHTTPFaultInjectionAbortHttpStatusErrorType(getFaultHttpStatus(enableIstio))
                    .endAbort()
                    .build();
        else
            return new HTTPFaultInjectionBuilder()
                    .withNewDelay()
                    .withNewPercentage()
                        .withValue((double) getFaultPercentage(enableIstio))
                    .endPercentage()
                    .withNewHTTPFaultInjectionDelayFixedDelayHttpType()
                        .withFixedDelay(formatDuration(getFaultDelay(enableIstio), "s's'"))
                    .endHTTPFaultInjectionDelayFixedDelayHttpType()
                    .endDelay()
                    .build();
    }

    public HTTPRoute buildRoute(EnableIstio enableIstio) {
        return new HTTPRouteBuilder()
                .withMatch(Arrays.stream(getMatches(enableIstio))
                        .map(this::buildHTTPMatchRequest)
                        .toArray(HTTPMatchRequest[]::new))
                .withTimeout(getTimeout(enableIstio) == 0 ? null : formatDuration(getTimeout(enableIstio), "s's'"))
                .withFault(getFaultPercentage(enableIstio) == 0 ? null : buildFault(enableIstio))
                .withRetries(buildRetry(enableIstio))
                .withRoute(buildRouteDestination(enableIstio))
                .build();
    }

}
