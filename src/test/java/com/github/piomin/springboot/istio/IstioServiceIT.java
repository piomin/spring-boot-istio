package com.github.piomin.springboot.istio;

import io.fabric8.istio.api.networking.v1beta1.VirtualService;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class IstioServiceIT {

    private final static String K8S_TEST_CONTEXT = "kind-c1";

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        Config config = Config.autoConfigure(K8S_TEST_CONTEXT);
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
            System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, client.getConfiguration().getMasterUrl());
            System.setProperty(Config.KUBERNETES_CLIENT_CERTIFICATE_FILE_SYSTEM_PROPERTY,
                    client.getConfiguration().getClientCertFile());
            System.setProperty(Config.KUBERNETES_CA_CERTIFICATE_FILE_SYSTEM_PROPERTY,
                    client.getConfiguration().getCaCertFile());
            System.setProperty(Config.KUBERNETES_CLIENT_KEY_FILE_SYSTEM_PROPERTY,
                    client.getConfiguration().getClientKeyFile());
            System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
            System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
            System.setProperty(Config.KUBERNETES_HTTP2_DISABLE, "true");
            System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "default");
        }
    }

    @Test
    void shouldStart() {
        Config config = Config.autoConfigure(K8S_TEST_CONTEXT);
        try (KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build()) {
            VirtualService vs = client.resources(VirtualService.class)
                    .withName("sample-app-with-istio-route")
                    .get();
            Assertions.assertNotNull(vs);
        }
    }
}
