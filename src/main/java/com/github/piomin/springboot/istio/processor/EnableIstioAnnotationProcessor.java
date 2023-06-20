package com.github.piomin.springboot.istio.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.kubernetes.client.dsl.Resource;
import me.snowdrop.istio.api.Duration;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRule;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1beta1.DoneableDestinationRule;
import me.snowdrop.istio.api.networking.v1beta1.DoneableVirtualService;
import me.snowdrop.istio.api.networking.v1beta1.Subset;
import me.snowdrop.istio.api.networking.v1beta1.VirtualService;
import me.snowdrop.istio.api.networking.v1beta1.VirtualServiceBuilder;
import me.snowdrop.istio.client.IstioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class EnableIstioAnnotationProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(EnableIstioAnnotationProcessor.class);
    private final IstioClient istioClient;
    private final IstioService istioService;

    public EnableIstioAnnotationProcessor(IstioClient istioClient, IstioService istioService) {
        this.istioClient = istioClient;
        this.istioService = istioService;
    }

    public void process(EnableIstio enableIstioAnnotation) {
        LOGGER.info("Istio feature enabled: {}", enableIstioAnnotation);

        Resource<DestinationRule, DoneableDestinationRule> resource = istioClient
                .v1beta1DestinationRule()
                .withName(istioService.getDestinationRuleName());
        if (resource.get() == null) {
            createNewDestinationRule(enableIstioAnnotation);
        } else {
            editDestinationRule(enableIstioAnnotation, resource);
        }

        Resource<VirtualService, DoneableVirtualService> resource2 = istioClient
                .v1beta1VirtualService()
                .withName(istioService.getVirtualServiceName());
        if (resource2.get() == null) {
            createNewVirtualService(enableIstioAnnotation);
        } else {
            editVirtualService(enableIstioAnnotation, resource2);
        }
    }

    private void createNewDestinationRule(EnableIstio enableIstioAnnotation) {
        DestinationRule dr = new DestinationRuleBuilder()
                .withMetadata(istioService.buildDestinationRuleMetadata())
                .withNewSpec()
                .withNewHost(istioService.getApplicationName())
                .withSubsets(istioService.buildSubset(enableIstioAnnotation))
                .withTrafficPolicy(istioService.buildCircuitBreaker(enableIstioAnnotation))
                .endSpec()
                .build();
        istioClient.v1beta1DestinationRule().create(dr);
        LOGGER.info("New DestinationRule created: {}", dr);
    }

    private void editDestinationRule(EnableIstio enableIstioAnnotation, Resource<DestinationRule, DoneableDestinationRule> resource) {
        LOGGER.info("Found DestinationRule: {}", resource.get());
        if (!enableIstioAnnotation.version().isEmpty()) {
            Optional<Subset> subset = resource.get().getSpec().getSubsets().stream()
                    .filter(s -> s.getName().equals(enableIstioAnnotation.version()))
                    .findAny();
            resource.edit()
                    .editSpec()
                    .addAllToSubsets(subset.isEmpty() ? Collections.singletonList(istioService.buildSubset(enableIstioAnnotation)) :
                            Collections.emptyList())
                    .editOrNewTrafficPolicyLike(istioService.buildCircuitBreaker(enableIstioAnnotation))
                    .endTrafficPolicy()
                    .endSpec()
                    .done();
        }
    }

    private void createNewVirtualService(EnableIstio enableIstioAnnotation) {
        VirtualService vs = new VirtualServiceBuilder()
                .withNewMetadata().withName(istioService.getVirtualServiceName()).endMetadata()
                .withNewSpec()
                .addToHosts(istioService.getApplicationName())
                .addNewHttp()
                .withTimeout(enableIstioAnnotation.timeout() == 0 ? null : new Duration(0, (long) enableIstioAnnotation
                        .timeout()))
                .withRetries(istioService.buildRetry(enableIstioAnnotation))
                .addNewRoute().withNewDestinationLike(istioService.buildDestination(enableIstioAnnotation))
                .endDestination().endRoute()
                .endHttp()
                .endSpec()
                .build();
        istioClient.v1beta1VirtualService().create(vs);
        LOGGER.info("New VirtualService created: {}", vs);
    }

    private void editVirtualService(EnableIstio enableIstioAnnotation, Resource<VirtualService, DoneableVirtualService> resource) {
        LOGGER.info("Found VirtualService: {}", resource.get());
        if (!enableIstioAnnotation.version().isEmpty()) {
            istioClient.v1beta1VirtualService().withName(istioService.getVirtualServiceName())
                    .edit()
                    .editSpec()
                    .editFirstHttp()
                    .withTimeout(enableIstioAnnotation
                            .timeout() == 0 ? null : new Duration(0, (long) enableIstioAnnotation.timeout()))
                    .withRetries(istioService.buildRetry(enableIstioAnnotation))
                    .editFirstRoute()
                    .withWeight(enableIstioAnnotation.weight() == 0 ? null : enableIstioAnnotation.weight())
                    .editOrNewDestinationLike(istioService.buildDestination(enableIstioAnnotation)).endDestination()
                    .endRoute()
                    .endHttp()
                    .endSpec()
                    .done();
        }
    }
}
