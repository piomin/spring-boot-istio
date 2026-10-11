package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.config.IstioProperties;
import com.github.piomin.springboot.istio.config.SpringBootIstioAutoConfiguration;
import com.github.piomin.springboot.istio.processor.ApplicationStartupListener;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.client.IstioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SpringBootIstioAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringBootIstioAutoConfiguration.class))
            .withPropertyValues("spring.application.name=test-app");

    @Test
    public void shouldRegisterIstioServiceBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(IstioService.class));
    }

    @Test
    public void shouldRegisterAnnotationProcessorBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(EnableIstioAnnotationProcessor.class));
    }

    @Test
    public void shouldRegisterApplicationStartupListenerBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(ApplicationStartupListener.class));
    }

    @Test
    public void shouldRegisterIstioClientBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(IstioClient.class));
    }

    @Test
    public void shouldRegisterIstioPropertiesBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(IstioProperties.class));
    }

    @Test
    public void shouldNotOverrideExistingIstioClientBean() {
        IstioClient customClient = mock(IstioClient.class);
        contextRunner
                .withBean(IstioClient.class, () -> customClient)
                .run(context -> {
                    assertThat(context).hasSingleBean(IstioClient.class);
                    assertThat(context.getBean(IstioClient.class)).isSameAs(customClient);
                });
    }
}
