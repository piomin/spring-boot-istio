package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.kubernetes.client.KubernetesClientException;
import me.snowdrop.istio.client.DefaultIstioClient;
import me.snowdrop.istio.client.IstioClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.annotation.Annotation;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {IstioService.class, DefaultIstioClient.class, EnableIstioAnnotationProcessor.class},
        properties = "spring.application.name=test1")
public class EnableIstioAnnotationProcessorTests {

    @Autowired
    EnableIstioAnnotationProcessor processor;

    @Test(expected = KubernetesClientException.class)
    public void test() {
        EnableIstio enableIstio = createEnableIstio(0, 3, "v1");
        processor.process(enableIstio);
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
        };
    }
}
