package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableIstio
public class SampleAppWithIstio {
    public static void main(String[] args) {
        SpringApplication.run(SampleAppWithIstio.class, args);
    }
}
