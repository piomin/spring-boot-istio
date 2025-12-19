package com.github.piomin.springboot.istio;

import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Testcontainers
public class IstioServiceIT {
}
