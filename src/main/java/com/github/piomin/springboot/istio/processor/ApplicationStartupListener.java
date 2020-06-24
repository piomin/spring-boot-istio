package com.github.piomin.springboot.istio.processor;

import java.util.Optional;

import com.github.piomin.springboot.istio.annotation.EnableIstio;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class ApplicationStartupListener implements ApplicationListener<ContextRefreshedEvent> {

	private ApplicationContext context;
	private EnableIstioAnnotationProcessor processor;

	public ApplicationStartupListener(ApplicationContext context, EnableIstioAnnotationProcessor processor) {
		this.context = context;
		this.processor = processor;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		Optional<EnableIstio> annotation =  context.getBeansWithAnnotation(EnableIstio.class).keySet().stream()
				.map(key -> context.findAnnotationOnBean(key, EnableIstio.class))
				.findFirst();
		annotation.ifPresent(enableIstio -> processor.process(enableIstio));
	}
}
