package com.github.piomin.springboot.istio.processor;

import java.util.Arrays;
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

        DestinationRule dr = istioClient
                .v1beta1()
                .destinationRules()
                .withName(istioService.getDestinationRuleName())
                .get();
        if (dr == null) {
            createNewDestinationRule(enableIstioAnnotation);
        } else {
            editDestinationRule(enableIstioAnnotation, dr);
        }

        VirtualService vs = istioClient
                .v1beta1()
                .virtualServices()
                .withName(istioService.getVirtualServiceName())
                .get();
        if (vs == null) {
            createNewVirtualService(enableIstioAnnotation);
        } else {
            editVirtualService(enableIstioAnnotation, vs);
        }

        if (enableIstioAnnotation.enableGateway()) {
            Gateway gateway = istioClient
                    .v1beta1()
                    .gateways()
                    .withName(istioService.getApplicationName())
                    .get();
            if (gateway == null) {
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
        dr = istioClient.v1beta1().destinationRules().resource(dr).create();
        LOGGER.info("New DestinationRule created: \n{}", Serialization.asYaml(dr));
    }

    private void editDestinationRule(EnableIstio enableIstioAnnotation, DestinationRule dr) {
        LOGGER.info("Found DestinationRule: {}", dr);
        if (!enableIstioAnnotation.version().isEmpty()) {
            Optional<Subset> subset = dr.getSpec().getSubsets().stream()
                    .filter(s -> s.getName().equals(enableIstioAnnotation.version()))
                    .findAny();
            if (subset.isEmpty()) {
                dr.getSpec().getSubsets().add(istioService.buildSubset(enableIstioAnnotation));
            }
            dr.getSpec().setTrafficPolicy(istioService.buildCircuitBreaker(enableIstioAnnotation));
            dr = istioClient.v1beta1().destinationRules().resource(dr).update();
            LOGGER.info("DestinationRule updated: \n{}", Serialization.asYaml(dr));
        }
    }

    private void createNewVirtualService(EnableIstio enableIstioAnnotation) {
        VirtualServiceBuilder vsBuilder = new VirtualServiceBuilder();
        vsBuilder = vsBuilder
                .withNewMetadata().withName(istioService.getVirtualServiceName()).endMetadata()
                .withNewSpec()
                .withHosts(addToHosts(enableIstioAnnotation))
//                .withGateways(enableIstioAnnotation.enableGateway() ? istioService.getApplicationName() : "null")
                .addNewHttp()
                .withMatch(Arrays.stream(enableIstioAnnotation.matches())
                        .map(istioService::buildHTTPMatchRequest)
                        .toArray(HTTPMatchRequest[]::new))
                .withTimeout(enableIstioAnnotation.timeout() == 0 ? null : formatDuration(enableIstioAnnotation.timeout(), "s's'"))
                .withFault(enableIstioAnnotation.fault().percentage() == 0 ? null : istioService.buildFault(enableIstioAnnotation))
                .withRetries(istioService.buildRetry(enableIstioAnnotation))
                .addNewRoute().withNewDestinationLike(istioService.buildDestination(enableIstioAnnotation))
                .endDestination().endRoute()
                .endHttp()
                .endSpec();
        if (enableIstioAnnotation.enableGateway()) {
            vsBuilder = vsBuilder.editSpec().withGateways(istioService.getApplicationName()).endSpec();
        }
        VirtualService vs = vsBuilder.build();
        vs = istioClient.v1beta1().virtualServices().resource(vs).create();
        LOGGER.info("New VirtualService created: \n{}", Serialization.asYaml(vs));
    }

    private void editVirtualService(EnableIstio enableIstioAnnotation, VirtualService vs) {
        LOGGER.info("Found VirtualService: {}", vs);
        vs.getSpec().setHosts(List.of(addToHosts(enableIstioAnnotation)));
        if (enableIstioAnnotation.enableGateway()) {
            vs.getSpec().setGateways(List.of(istioService.getApplicationName()));
        }
//        vs.getSpec().getHttp().get(0).setTimeout(enableIstioAnnotation
//                .timeout() == 0 ? null : formatDuration(enableIstioAnnotation.timeout(), "s's'"));
//        vs.getSpec().getHttp().get(0).setRetries(istioService.buildRetry(enableIstioAnnotation));
//        vs.getSpec().getHttp().get(0).setFault(enableIstioAnnotation.fault().percentage() == 0 ? null : istioService.buildFault(enableIstioAnnotation));

        if (!enableIstioAnnotation.version().isEmpty()) {
            int index = findDestination(vs.getSpec().getHttp(), enableIstioAnnotation.version());
            if (index != -1) {
                vs.getSpec().getHttp().get(index).getRoute().getFirst()
                        .setWeight(enableIstioAnnotation.weight() == 0 ? null : enableIstioAnnotation.weight());
            } else {
                if (enableIstioAnnotation.weight() == 100)
                    vs.getSpec().getHttp().add(istioService.buildRoute(enableIstioAnnotation));
                else
                    vs.getSpec().getHttp().getFirst().getRoute()
                            .add(istioService.buildRouteDestination(enableIstioAnnotation));
            }
        } else {
            vs.getSpec().getHttp().get(0).setTimeout(enableIstioAnnotation
                    .timeout() == 0 ? null : formatDuration(enableIstioAnnotation.timeout(), "s's'"));
            vs.getSpec().getHttp().get(0).setRetries(istioService.buildRetry(enableIstioAnnotation));
            vs.getSpec().getHttp().get(0).setFault(enableIstioAnnotation.fault().percentage() == 0 ? null : istioService.buildFault(enableIstioAnnotation));
            vs.getSpec().getHttp().get(0).getRoute().get(0).setDestination(istioService.buildDestination(enableIstioAnnotation));
        }
        vs = istioClient.v1beta1().virtualServices().resource(vs).update();
        LOGGER.info("VirtualService updated: \n{}", Serialization.asYaml(vs));
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
                        .addToHosts(istioService.getApplicationName() + "." + enableIstioAnnotation.domain())
                        .build())
                .endSpec()
                .build();
        gateway = istioClient.v1beta1().gateways().resource(gateway).create();
        LOGGER.info("New Gateway created: \n{}", Serialization.asYaml(gateway));
    }

    private void editGateway(EnableIstio enableIstioAnnotation, Gateway gateway) {
        LOGGER.info("Found Gateway: {}", gateway);
        if (gateway.getSpec().getServers() != null && !gateway.getSpec().getServers().isEmpty()) {
            Server server = gateway.getSpec().getServers().get(0);
            String appHost = istioService.getApplicationName() + "." + enableIstioAnnotation.domain();
            if (server.getHosts() != null && !server.getHosts().contains(appHost)) {
                server.getHosts().add(appHost);
            }
        }
        gateway = istioClient.v1beta1().gateways().resource(gateway).update();
        LOGGER.info("Gateway updated: \n{}", Serialization.asYaml(gateway));
    }

    private String[] addToHosts(EnableIstio enableIstioAnnotation) {
        if (enableIstioAnnotation.enableGateway())
            return new String[] { istioService.getApplicationName(), istioService.getApplicationName() + "." + enableIstioAnnotation.domain()};
        else return new String[] { istioService.getApplicationName() };
    }

    private int findDestination(List<HTTPRoute> routes, String version) {
        if (routes == null || version == null || version.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < routes.size(); i++) {
            HTTPRouteDestination dest = routes.get(i).getRoute().getFirst();
            if (dest != null &&
                    dest.getDestination() != null &&
                    version.equals(dest.getDestination().getSubset())) {
                return i;
            }
        }
        return -1;
    }

}
