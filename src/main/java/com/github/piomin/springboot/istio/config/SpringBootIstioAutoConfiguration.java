package com.github.piomin.springboot.istio.config;

import com.github.piomin.springboot.istio.processor.ApplicationStartupListener;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import com.github.piomin.springboot.istio.service.IstioService;
import io.fabric8.istio.client.DefaultIstioClient;
import io.fabric8.istio.client.IstioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringBootIstioAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    IstioClient istioClient() {
        return new DefaultIstioClient();
    }

    @Bean
    IstioService istioService() {
        return new IstioService();
    }

    @Bean
    ApplicationStartupListener listener(ApplicationContext context) {
        return new ApplicationStartupListener(context, istioAnnotationProcessor());
    }

    @Bean
    EnableIstioAnnotationProcessor istioAnnotationProcessor() {
        return new EnableIstioAnnotationProcessor(istioClient(), istioService());
    }

}
