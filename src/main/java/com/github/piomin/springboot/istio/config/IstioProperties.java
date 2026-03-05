package com.github.piomin.springboot.istio.config;

import com.github.piomin.springboot.istio.annotation.FaultType;
import com.github.piomin.springboot.istio.annotation.MatchMode;
import com.github.piomin.springboot.istio.annotation.MatchType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "istio.spring")
public class IstioProperties {

    private Integer timeout;
    private String version;
    private Integer weight;
    private Integer numberOfRetries;
    private Integer circuitBreakerErrors;
    private Boolean enableGateway;
    private String domain;
    private FaultProperties fault = new FaultProperties();
    private List<MatchProperties> matches = new ArrayList<>();

    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getNumberOfRetries() { return numberOfRetries; }
    public void setNumberOfRetries(Integer numberOfRetries) { this.numberOfRetries = numberOfRetries; }

    public Integer getCircuitBreakerErrors() { return circuitBreakerErrors; }
    public void setCircuitBreakerErrors(Integer circuitBreakerErrors) { this.circuitBreakerErrors = circuitBreakerErrors; }

    public Boolean getEnableGateway() { return enableGateway; }
    public void setEnableGateway(Boolean enableGateway) { this.enableGateway = enableGateway; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public FaultProperties getFault() { return fault; }
    public void setFault(FaultProperties fault) { this.fault = fault; }

    public List<MatchProperties> getMatches() { return matches; }
    public void setMatches(List<MatchProperties> matches) { this.matches = matches; }

    public static class FaultProperties {
        private FaultType type;
        private Integer percentage;
        private Integer httpStatus;
        private Long delay;

        public FaultType getType() { return type; }
        public void setType(FaultType type) { this.type = type; }

        public Integer getPercentage() { return percentage; }
        public void setPercentage(Integer percentage) { this.percentage = percentage; }

        public Integer getHttpStatus() { return httpStatus; }
        public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

        public Long getDelay() { return delay; }
        public void setDelay(Long delay) { this.delay = delay; }
    }

    public static class MatchProperties {
        private boolean ignoreUriCase = false;
        private MatchType type = MatchType.URI;
        private MatchMode mode = MatchMode.PREFIX;
        private String value = "";
        private String key = "";

        public boolean isIgnoreUriCase() { return ignoreUriCase; }
        public void setIgnoreUriCase(boolean ignoreUriCase) { this.ignoreUriCase = ignoreUriCase; }

        public MatchType getType() { return type; }
        public void setType(MatchType type) { this.type = type; }

        public MatchMode getMode() { return mode; }
        public void setMode(MatchMode mode) { this.mode = mode; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }
}
