package org.ehrbase.fhirbridge.openehr.openehrclient;

import com.google.common.net.MediaType;

public enum CompositionFormat {
    XML(MediaType.APPLICATION_XML_UTF_8), JSON(MediaType.JSON_UTF_8);

    private final MediaType mediaType;

    CompositionFormat(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
