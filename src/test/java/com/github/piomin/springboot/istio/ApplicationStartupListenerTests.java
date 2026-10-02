package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.processor.ApplicationStartupListener;
import com.github.piomin.springboot.istio.processor.EnableIstioAnnotationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApplicationStartupListenerTests {

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private EnableIstioAnnotationProcessor processor;
    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;

    @Test
    public void shouldCallProcessorWhenEnableIstioAnnotationPresent() {
        EnableIstio annotation = mock(EnableIstio.class);
        when(applicationContext.getBeansWithAnnotation(EnableIstio.class))
                .thenReturn(Map.of("sampleApp", new Object()));
        when(applicationContext.findAnnotationOnBean("sampleApp", EnableIstio.class))
                .thenReturn(annotation);

        ApplicationStartupListener listener =
                new ApplicationStartupListener(applicationContext, processor);
        listener.onApplicationEvent(contextRefreshedEvent);

        verify(processor).process(annotation);
    }

    @Test
    public void shouldNotCallProcessorWhenNoAnnotatedBeans() {
        when(applicationContext.getBeansWithAnnotation(EnableIstio.class))
                .thenReturn(Collections.emptyMap());

        ApplicationStartupListener listener =
                new ApplicationStartupListener(applicationContext, processor);
        listener.onApplicationEvent(contextRefreshedEvent);

        verify(processor, never()).process(any());
    }

    @Test
    public void shouldUseFirstAnnotatedBeanWhenMultipleExist() {
        EnableIstio annotation = mock(EnableIstio.class);
        // LinkedHashMap preserves insertion order
        Map<String, Object> beans = new java.util.LinkedHashMap<>();
        beans.put("app1", new Object());
        beans.put("app2", new Object());
        when(applicationContext.getBeansWithAnnotation(EnableIstio.class))
                .thenReturn(beans);
        when(applicationContext.findAnnotationOnBean("app1", EnableIstio.class))
                .thenReturn(annotation);

        ApplicationStartupListener listener =
                new ApplicationStartupListener(applicationContext, processor);
        listener.onApplicationEvent(contextRefreshedEvent);

        verify(processor).process(annotation);
        verify(applicationContext, never()).findAnnotationOnBean(eq("app2"), eq(EnableIstio.class));
    }
}
