package com.github.piomin.springboot.istio;

import com.github.piomin.springboot.istio.annotation.EnableIstio;
import com.github.piomin.springboot.istio.annotation.Fault;
import com.github.piomin.springboot.istio.annotation.FaultType;
import com.github.piomin.springboot.istio.annotation.Match;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableIstio(enableGateway = true,
        fault = @Fault(type = FaultType.ABORT, percentage = 50),
        matches = { @Match("/hello") })
public class SampleAppWithIstio {
    public static void main(String[] args) {
        SpringApplication.run(SampleAppWithIstio.class, args);
    }
}
