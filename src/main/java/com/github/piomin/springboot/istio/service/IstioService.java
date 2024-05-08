package com.github.piomin.springboot.istio.service;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import io.fabric8.istio.api.networking.v1beta1.*;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.springframework.beans.factory.annotation.Value;

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

}
