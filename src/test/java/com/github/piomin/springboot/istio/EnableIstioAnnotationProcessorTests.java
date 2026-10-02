package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.Match;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.api.api.networking.v1alpha3.Destination;
import io.fabric8.istio.api.api.networking.v1alpha3.DestinationBuilder;
import io.fabric8.istio.api.api.networking.v1alpha3.Subset;
import io.fabric8.istio.api.api.networking.v1alpha3.SubsetBuilder;
import io.fabric8.istio.api.api.networking.v1alpha3.TrafficPolicyBuilder;
import io.fabric8.istio.api.networking.v1beta1.DestinationRule;
import io.fabric8.istio.api.networking.v1beta1.DestinationRuleBuilder;
import io.fabric8.istio.api.networking.v1beta1.Gateway;
import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.istio.client.IstioClient;
import io.fabric8.istio.client.V1beta1APIGroupDSL;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnableIstioAnnotationProcessorTests {

    @Mock
    private IstioClient istioClient;
    @Mock
    private IstioService istioService;
    @Mock
    private V1beta1APIGroupDSL v1beta1;
    @Mock
    private MixedOperation destinationRuleOps;
    @Mock
    private MixedOperation virtualServiceOps;
    @Mock
    private MixedOperation gatewayOps;
    @Mock
    private Resource destinationRuleResource;
    @Mock
    private Resource virtualServiceResource;
    @Mock
    private Resource gatewayResource;

    private EnableIstioAnnotationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new EnableIstioAnnotationProcessor(istioClient, istioService);
        lenient().when(istioClient.v1beta1()).thenReturn(v1beta1);
        lenient().when(v1beta1.destinationRules()).thenReturn(destinationRuleOps);
        lenient().when(v1beta1.virtualServices()).thenReturn(virtualServiceOps);
        lenient().when(v1beta1.gateways()).thenReturn(gatewayOps);
        lenient().when(istioService.getDestinationRuleName()).thenReturn("test-app-destination");
        lenient().when(istioService.getVirtualServiceName()).thenReturn("test-app-route");
        lenient().when(istioService.getApplicationName()).thenReturn("test-app");
    }

    @Test
    public void shouldCreateNewDestinationRuleWhenNotExists() {
        EnableIstio enableIstio = createEnableIstio("v1", false);
        when(destinationRuleOps.withName("test-app-destination")).thenReturn(destinationRuleResource);
        when(destinationRuleResource.get()).thenReturn(null);
        when(istioService.getVersion(enableIstio)).thenReturn("v1");
        when(istioService.buildDestinationRuleMetadata()).thenReturn(
                new ObjectMetaBuilder().withName("test-app-destination").build());
        when(istioService.buildSubset(enableIstio)).thenReturn(
                new SubsetBuilder().withName("v1").build());
        when(istioService.buildCircuitBreaker(enableIstio)).thenReturn(
                new TrafficPolicyBuilder().build());

        Resource drCreatedResource = mock(Resource.class);
        when(destinationRuleOps.resource(any(DestinationRule.class))).thenReturn(drCreatedResource);
        when(drCreatedResource.create()).thenReturn(new DestinationRule());

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        verify(destinationRuleOps).resource(any(DestinationRule.class));
        verify(drCreatedResource).create();
    }

    @Test
    public void shouldSkipDestinationRuleCreationWhenVersionIsEmpty() {
        EnableIstio enableIstio = createEnableIstio("", false);
        when(destinationRuleOps.withName("test-app-destination")).thenReturn(destinationRuleResource);
        when(destinationRuleResource.get()).thenReturn(null);
        when(istioService.getVersion(enableIstio)).thenReturn("");

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        verify(destinationRuleOps, never()).resource(any(DestinationRule.class));
    }

    @Test
    public void shouldEditExistingDestinationRuleWhenExists() {
        EnableIstio enableIstio = createEnableIstio("v1", false);
        DestinationRule existingDr = new DestinationRuleBuilder()
                .withNewSpec()
                .withSubsets(new ArrayList<>(List.of(new SubsetBuilder().withName("v0").build())))
                .endSpec()
                .build();
        when(destinationRuleOps.withName("test-app-destination")).thenReturn(destinationRuleResource);
        when(destinationRuleResource.get()).thenReturn(existingDr);
        when(istioService.getVersion(enableIstio)).thenReturn("v1");
        when(istioService.buildSubset(enableIstio)).thenReturn(
                new SubsetBuilder().withName("v1").build());
        when(istioService.buildCircuitBreaker(enableIstio)).thenReturn(
                new TrafficPolicyBuilder().build());

        Resource drUpdatedResource = mock(Resource.class);
        when(destinationRuleOps.resource(any(DestinationRule.class))).thenReturn(drUpdatedResource);
        when(drUpdatedResource.update()).thenReturn(existingDr);

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        verify(drUpdatedResource).update();
    }

    @Test
    public void shouldNotAddDuplicateSubsetWhenEditingDestinationRule() {
        EnableIstio enableIstio = createEnableIstio("v1", false);
        List<Subset> subsets = new ArrayList<>(List.of(new SubsetBuilder().withName("v1").build()));
        DestinationRule existingDr = new DestinationRuleBuilder()
                .withNewSpec()
                .withSubsets(subsets)
                .endSpec()
                .build();
        when(destinationRuleOps.withName("test-app-destination")).thenReturn(destinationRuleResource);
        when(destinationRuleResource.get()).thenReturn(existingDr);
        when(istioService.getVersion(enableIstio)).thenReturn("v1");
        when(istioService.buildCircuitBreaker(enableIstio)).thenReturn(
                new TrafficPolicyBuilder().build());

        Resource drUpdatedResource = mock(Resource.class);
        when(destinationRuleOps.resource(any(DestinationRule.class))).thenReturn(drUpdatedResource);
        when(drUpdatedResource.update()).thenReturn(existingDr);

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        // Subset "v1" already exists, so buildSubset should not be called
        verify(istioService, never()).buildSubset(enableIstio);
    }

    @Test
    public void shouldCreateNewVirtualServiceWhenNotExists() {
        EnableIstio enableIstio = createEnableIstio("v1", false);
        stubDestinationRuleNotExistsWithVersion(enableIstio);

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        verify(virtualServiceOps).resource(any(VirtualService.class));
    }

    @Test
    public void shouldCreateGatewayWhenEnabled() {
        EnableIstio enableIstio = createEnableIstio("v1", true);
        stubDestinationRuleNotExistsWithVersion(enableIstio);

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(true);
        when(istioService.getDomain(enableIstio)).thenReturn("ext");

        Resource gatewayNameResource = mock(Resource.class);
        when(gatewayOps.withName("test-app")).thenReturn(gatewayNameResource);
        when(gatewayNameResource.get()).thenReturn(null);

        Resource gatewayCreatedResource = mock(Resource.class);
        when(gatewayOps.resource(any(Gateway.class))).thenReturn(gatewayCreatedResource);
        when(gatewayCreatedResource.create()).thenReturn(new Gateway());

        processor.process(enableIstio);

        verify(gatewayOps).resource(any(Gateway.class));
        verify(gatewayCreatedResource).create();
    }

    @Test
    public void shouldNotCreateGatewayWhenDisabled() {
        EnableIstio enableIstio = createEnableIstio("v1", false);
        stubDestinationRuleNotExistsWithVersion(enableIstio);

        when(virtualServiceOps.withName("test-app-route")).thenReturn(virtualServiceResource);
        when(virtualServiceResource.get()).thenReturn(null);
        stubVirtualServiceCreate(enableIstio);

        when(istioService.isEnableGateway(enableIstio)).thenReturn(false);

        processor.process(enableIstio);

        verify(gatewayOps, never()).withName(anyString());
    }

    // --- Helper methods ---

    private void stubDestinationRuleNotExistsWithVersion(EnableIstio enableIstio) {
        when(destinationRuleOps.withName("test-app-destination")).thenReturn(destinationRuleResource);
        when(destinationRuleResource.get()).thenReturn(null);
        when(istioService.getVersion(enableIstio)).thenReturn(enableIstio.version());
        if (!enableIstio.version().isEmpty()) {
            when(istioService.buildDestinationRuleMetadata()).thenReturn(
                    new ObjectMetaBuilder().withName("test-app-destination").build());
            when(istioService.buildSubset(enableIstio)).thenReturn(
                    new SubsetBuilder().withName(enableIstio.version()).build());
            when(istioService.buildCircuitBreaker(enableIstio)).thenReturn(
                    new TrafficPolicyBuilder().build());
            Resource drCreatedResource = mock(Resource.class);
            when(destinationRuleOps.resource(any(DestinationRule.class))).thenReturn(drCreatedResource);
            when(drCreatedResource.create()).thenReturn(new DestinationRule());
        }
    }

    @SuppressWarnings("unchecked")
    private void stubVirtualServiceCreate(EnableIstio enableIstio) {
        when(istioService.getMatches(enableIstio)).thenReturn(new Match[0]);
        when(istioService.getTimeout(enableIstio)).thenReturn(0);
        when(istioService.getFaultPercentage(enableIstio)).thenReturn(0);
        when(istioService.buildRetry(enableIstio)).thenReturn(null);
        when(istioService.buildDestination(enableIstio)).thenReturn(
                new DestinationBuilder().withHost("test-app").withSubset(enableIstio.version()).build());
        Resource vsCreatedResource = mock(Resource.class);
        when(virtualServiceOps.resource(any(VirtualService.class))).thenReturn(vsCreatedResource);
        when(vsCreatedResource.create()).thenReturn(new VirtualService());
    }

    private EnableIstio createEnableIstio(String version, boolean enableGateway) {
        return new EnableIstio() {
            @Override public Class<? extends Annotation> annotationType() { return EnableIstio.class; }
            @Override public int timeout() { return 0; }
            @Override public String version() { return version; }
            @Override public int weight() { return 100; }
            @Override public int numberOfRetries() { return 3; }
            @Override public int circuitBreakerErrors() { return 0; }
            @Override public Match[] matches() { return new Match[0]; }
            @Override public Fault fault() { return null; }
            @Override public boolean enableGateway() { return enableGateway; }
            @Override public String domain() { return "ext"; }
        };
    }
}
