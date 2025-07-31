package org.highmed.hiveconnect.camel.component.ehr;

import lombok.Getter;
import lombok.Setter;
import org.apache.camel.RuntimeCamelException;

import org.ehrbase.openehr.sdk.client.openehrclient.OpenEhrClient;

@SuppressWarnings("java:S2157")
@Getter
@Setter
public class EhrConfiguration implements Cloneable {

    private OpenEhrClient openEhrClient;

    public EhrConfiguration copy() {
        try {
            return (EhrConfiguration) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeCamelException(e);
        }
    }
}
