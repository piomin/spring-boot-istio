package com.github.piomin.springboot.istio.service;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import com.github.piomin.springboot.istio.annotation.FaultType;
import io.fabric8.istio.api.networking.v1beta1.*;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

public class IstioService {

    private static final String RETRY_CODES = "5xx";
    private static final String CIRCUIT_OPEN_TIME = "30s";
    private static final int MAX_DISABLED_HOSTS_PERCENTAGE = 100;

    @Value("${spring.application.name}")
    private Optional<String> applicationName;

    public String getApplicationName() {
        return applicationName.orElse("default");
    }

    public String getDestinationRuleName() {
        return getApplicationName() + "-destination";
    }

    public String getVirtualServiceName() {
        return getApplicationName() + "-route";
    }

    public ObjectMeta buildDestinationRuleMetadata() {
        return new ObjectMetaBuilder().withName(getDestinationRuleName()).build();
    }

    public TrafficPolicy buildCircuitBreaker(EnableIstio enableIstio) {
        TrafficPolicyBuilder builder = new TrafficPolicyBuilder()
                .withConnectionPool(new ConnectionPoolSettingsBuilder().withNewHttp().endHttp().build());
        if (enableIstio.circuitBreakerErrors() == 0)
            return builder.build();
        else return builder.withOutlierDetection(new OutlierDetectionBuilder()
                        .withConsecutive5xxErrors(enableIstio.circuitBreakerErrors())
                        .withBaseEjectionTime(CIRCUIT_OPEN_TIME)
                        .withMaxEjectionPercent(MAX_DISABLED_HOSTS_PERCENTAGE)
                        .build())
                .build();
    }

    public Subset buildSubset(EnableIstio enableIstio) {
        return new SubsetBuilder()
                .withName(enableIstio.version())
                .addToLabels("version", enableIstio.version())
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
        else
            builder = builder.withSourceLabels(Map.of(match.key(), match.value()));
        return builder.withIgnoreUriCase(match.ignoreUriCase()).build();
    }

    public HTTPRetry buildRetry(EnableIstio enableIstio) {
        if (enableIstio.numberOfRetries() == 0)
            return null;
        long ratioTimeout = Math.round((float) enableIstio.timeout() / enableIstio.numberOfRetries());
        return new HTTPRetryBuilder()
                .withAttempts(enableIstio.numberOfRetries())
                .withRetryOn(RETRY_CODES)
                .withPerTryTimeout(enableIstio.timeout() != 0 ?
                        formatDuration(ratioTimeout * 1000, "s's'") : null)
                .build();
    }

    public Destination buildDestination(EnableIstio enableIstio) {
        return new DestinationBuilder()
                .withHost(getApplicationName())
                .withSubset(enableIstio.version())
                .build();
    }

    public HTTPFaultInjection buildFault(EnableIstio enableIstio) {
        if (enableIstio.fault().type().equals(FaultType.ABORT))
            return new HTTPFaultInjectionBuilder()
                    .withNewAbort()
                    .withNewPercentage()
                        .withValue((double) enableIstio.fault().percentage())
                    .endPercentage()
                    .withNewHTTPFaultInjectionAbortHttpStatusErrorType(enableIstio.fault().httpStatus())
                    .endAbort()
                    .build();
        else
            return new HTTPFaultInjectionBuilder()
                    .withNewDelay()
                    .withNewPercentage()
                        .withValue((double) enableIstio.fault().percentage())
                    .endPercentage()
                    .withNewHTTPFaultInjectionDelayFixedHttpType()
                        .withFixedDelay(formatDuration(enableIstio.fault().delay(), "s's'"))
                    .endHTTPFaultInjectionDelayFixedHttpType()
                    .endDelay()
                    .build();
    }

}
