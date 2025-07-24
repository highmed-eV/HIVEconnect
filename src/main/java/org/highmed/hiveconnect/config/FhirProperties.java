package org.highmed.hiveconnect.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hive-connect.fhir")
@Getter
@Setter
public class FhirProperties {

    private final Convert convert = new Convert();

    private final TerminologyServer terminologyServer = new TerminologyServer();

    @Setter
    @Getter
    public static class Convert {

        private boolean autoPopulateDisplay = false;
    }

    @Setter
    @Getter
    public static class TerminologyServer {

        private String url;
    }
}
