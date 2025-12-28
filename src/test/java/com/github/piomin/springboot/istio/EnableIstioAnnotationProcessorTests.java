package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = {IstioService.class, DefaultIstioClient.class, EnableIstioAnnotationProcessor.class},
        properties = "spring.application.name=test1")
public class EnableIstioAnnotationProcessorTests {

    @Autowired
    EnableIstioAnnotationProcessor processor;

    @Test
    public void test() {
        EnableIstio enableIstio = createEnableIstio(0, 3, "v1");
        assertThrows(KubernetesClientException.class, () -> processor.process(enableIstio));
    }

    private EnableIstio createEnableIstio(int timeout, int numberOfRetries, String version) {
        return new EnableIstio() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return EnableIstio.class;
            }

            @Override
            public int timeout() {
                return timeout;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public int weight() {
                return 0;
            }

            @Override
            public int numberOfRetries() {
                return numberOfRetries;
            }

            @Override
            public int circuitBreakerErrors() {
                return 0;
            }

            @Override
            public boolean enableGateway() {
                return false;
            }
        };
    }
}
