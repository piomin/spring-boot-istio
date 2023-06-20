package com.github.piomin.springboot.istio.processor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.service.IstioService;
import me.snowdrop.istio.api.Duration;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRule;
import me.snowdrop.istio.api.networking.v1beta1.DestinationRuleBuilder;
import me.snowdrop.istio.api.networking.v1beta1.Subset;
import me.snowdrop.istio.api.networking.v1beta1.VirtualService;
import me.snowdrop.istio.api.networking.v1beta1.VirtualServiceBuilder;
import me.snowdrop.istio.client.IstioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Object resource = istioClient
                .v1beta1DestinationRule()
                .withName(istioService.getDestinationRuleName());
        if (resource == null) {
            createNewDestinationRule(enableIstioAnnotation);
        } else {
            editDestinationRule(enableIstioAnnotation, resource);
        }

        Object resource2 = istioClient
                .v1beta1VirtualService()
                .withName(istioService.getVirtualServiceName());
        if (resource2 == null) {
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

    private void editDestinationRule(EnableIstio enableIstioAnnotation, Object resource) {
        LOGGER.info("Found DestinationRule: {}", resource);
        if (!enableIstioAnnotation.version().isEmpty()) {
            DestinationRule rule = (DestinationRule) resource;
            Optional<Subset> subset = rule.getSpec().getSubsets().stream()
                    .filter(s -> s.getName().equals(enableIstioAnnotation.version()))
                    .findAny();
            rule.getSpec().getSubsets().addAll(List.of(istioService.buildSubset(enableIstioAnnotation)));
            rule.getSpec().setTrafficPolicy(istioService.buildCircuitBreaker(enableIstioAnnotation));
            istioClient.v1beta1DestinationRule().createOrReplace(rule);
//            resource.edit()
//                    .editSpec()
//                    .addAllToSubsets(subset.isEmpty() ? Collections.singletonList(istioService.buildSubset(enableIstioAnnotation)) :
//                            Collections.emptyList())
//                    .editOrNewTrafficPolicyLike(istioService.buildCircuitBreaker(enableIstioAnnotation))
//                    .endTrafficPolicy()
//                    .endSpec()
//                    .done();
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

    private void editVirtualService(EnableIstio enableIstioAnnotation, Object resource) {
        LOGGER.info("Found VirtualService: {}", resource);
        if (!enableIstioAnnotation.version().isEmpty()) {
            VirtualService vs = (VirtualService) resource;
            vs.getSpec().getHttp().get(0).setTimeout(enableIstioAnnotation
                    .timeout() == 0 ? null : new Duration(0, (long) enableIstioAnnotation.timeout()));
            vs.getSpec().getHttp().get(0).setRetries(istioService.buildRetry(enableIstioAnnotation));
            vs.getSpec().getHttp().get(0).getRoute().get(0).setWeight(enableIstioAnnotation.weight() == 0 ? null : enableIstioAnnotation.weight());
            vs.getSpec().getHttp().get(0).getRoute().get(0).setDestination(istioService.buildDestination(enableIstioAnnotation));
            istioClient.v1beta1VirtualService().createOrReplace(vs);
//            istioClient.v1beta1VirtualService().withName(istioService.getVirtualServiceName())
//                    .edit()
//                    .editSpec()
//                    .editFirstHttp()
//                    .withTimeout(enableIstioAnnotation
//                            .timeout() == 0 ? null : new Duration(0, (long) enableIstioAnnotation.timeout()))
//                    .withRetries(istioService.buildRetry(enableIstioAnnotation))
//                    .editFirstRoute()
//                    .withWeight(enableIstioAnnotation.weight() == 0 ? null : enableIstioAnnotation.weight())
//                    .editOrNewDestinationLike(istioService.buildDestination(enableIstioAnnotation)).endDestination()
//                    .endRoute()
//                    .endHttp()
//                    .endSpec()
//                    .done();
        }
    }
}
