package org.ehrbase.fhirbridge.openehr.openehrclient;

import java.util.List;
import java.util.Set;

public interface FolderDAO {

    String getName();

    void setName(String name);

    Set<String> listSubFolderNames();

    FolderDAO getSubFolder(String path);

    <T> T addCompositionEntity(T entity);

    <T> List<T> find(Class<T> clazz);
}
