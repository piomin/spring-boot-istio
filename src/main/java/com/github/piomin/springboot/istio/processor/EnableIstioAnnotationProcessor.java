package com.github.piomin.springboot.istio.processor;

import java.util.List;
import java.util.Optional;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.networking.v1beta1.*;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

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

        Resource<DestinationRule> dr = istioClient
                .v1beta1()
                .destinationRules()
                .withName(istioService.getDestinationRuleName());
        if (dr.item() == null) {
            createNewDestinationRule(enableIstioAnnotation);
        } else {
            editDestinationRule(enableIstioAnnotation, dr);
        }

        Resource<VirtualService> vs = istioClient
                .v1beta1()
                .virtualServices()
                .withName(istioService.getVirtualServiceName());
        if (vs.item() == null) {
            createNewVirtualService(enableIstioAnnotation);
        } else {
            editVirtualService(enableIstioAnnotation, vs);
        }

        if (enableIstioAnnotation.enableGateway()) {
            Resource<Gateway> gateway = istioClient
                    .v1beta1()
                    .gateways()
                    .withName(istioService.getApplicationName());
            if (gateway.item() == null) {
                createNewGateway(enableIstioAnnotation);
            } else {
                editGateway(enableIstioAnnotation, gateway);
            }
        }
    }

    private void createNewDestinationRule(EnableIstio enableIstioAnnotation) {
        if (enableIstioAnnotation.version().isEmpty())
            return;
        DestinationRule dr = new DestinationRuleBuilder()
                .withMetadata(istioService.buildDestinationRuleMetadata())
                .withNewSpec()
                .withHost(istioService.getApplicationName())
                .withSubsets(istioService.buildSubset(enableIstioAnnotation))
                .withTrafficPolicy(istioService.buildCircuitBreaker(enableIstioAnnotation))
                .endSpec()
                .build();
        istioClient.v1beta1().destinationRules().create(dr);
        LOGGER.info("New DestinationRule created: \n{}", Serialization.asYaml(dr));
    }

    private void editDestinationRule(EnableIstio enableIstioAnnotation, Resource<DestinationRule> resource) {
        LOGGER.info("Found DestinationRule: {}", resource.item());
        if (!enableIstioAnnotation.version().isEmpty()) {
            DestinationRule rule = (DestinationRule) resource;
            Optional<Subset> subset = rule.getSpec().getSubsets().stream()
                    .filter(s -> s.getName().equals(enableIstioAnnotation.version()))
                    .findAny();
            rule.getSpec().getSubsets().addAll(List.of(istioService.buildSubset(enableIstioAnnotation)));
            rule.getSpec().setTrafficPolicy(istioService.buildCircuitBreaker(enableIstioAnnotation));
            istioClient.v1beta1().destinationRules().createOrReplace(rule);
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
                .withTimeout(enableIstioAnnotation.timeout() == 0 ? null : formatDuration(enableIstioAnnotation.timeout(), "s's'"))
                .withRetries(istioService.buildRetry(enableIstioAnnotation))
                .addNewRoute().withNewDestinationLike(istioService.buildDestination(enableIstioAnnotation))
                .endDestination().endRoute()
                .endHttp()
                .endSpec()
                .build();
        vs = istioClient.v1beta1().virtualServices().create(vs);
        LOGGER.info("New VirtualService created: \n{}", Serialization.asYaml(vs));
    }

    private void editVirtualService(EnableIstio enableIstioAnnotation, Resource<VirtualService> resource) {
        LOGGER.info("Found VirtualService: {}", resource.item());
        if (!enableIstioAnnotation.version().isEmpty()) {
            VirtualService vs = (VirtualService) resource;
            vs.getSpec().getHttp().get(0).setTimeout(enableIstioAnnotation
                    .timeout() == 0 ? null : formatDuration(enableIstioAnnotation.timeout(), "s's'"));
            vs.getSpec().getHttp().get(0).setRetries(istioService.buildRetry(enableIstioAnnotation));
            vs.getSpec().getHttp().get(0).getRoute().get(0).setWeight(enableIstioAnnotation.weight() == 0 ? null : enableIstioAnnotation.weight());
            vs.getSpec().getHttp().get(0).getRoute().get(0).setDestination(istioService.buildDestination(enableIstioAnnotation));
            istioClient.v1beta1().virtualServices().createOrReplace(vs);
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

    private void createNewGateway(EnableIstio enableIstioAnnotation) {
        Gateway gateway = new GatewayBuilder()
                .withNewMetadata().withName(istioService.getApplicationName()).endMetadata()
                .withNewSpec()
                .addToSelector("istio", "ingressgateway")
                .addToServers(new ServerBuilder()
                        .withPort(new PortBuilder()
                                .withNumber(80)
                                .withProtocol("HTTP")
                                .withName("http")
                                .build())
                        .addToHosts(istioService.getApplicationName() + ".ext")
                        .build())
                .endSpec()
                .build();
        gateway = istioClient.v1beta1().gateways().resource(gateway).create();
        LOGGER.info("New Gateway created: \n{}", Serialization.asYaml(gateway));
    }

    private void editGateway(EnableIstio enableIstioAnnotation, Resource<Gateway> resource) {
        LOGGER.info("Found Gateway: {}", resource.item());
        Gateway gateway = (Gateway) resource;
        if (gateway.getSpec().getServers() != null && !gateway.getSpec().getServers().isEmpty()) {
            Server server = gateway.getSpec().getServers().get(0);
            String appHost = istioService.getApplicationName() + ".ext";
            if (server.getHosts() != null && !server.getHosts().contains(appHost)) {
                server.getHosts().add(appHost);
            }
        }
        gateway = istioClient.v1beta1().gateways().resource(gateway).update();
        LOGGER.info("Gateway updated: \n{}", Serialization.asYaml(gateway));
    }

}
