package org.ehrbase.fhirbridge.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import com.nedap.archie.rm.RMObject;
import org.ehrbase.client.classgenerator.interfaces.CompositionEntity;
import org.ehrbase.client.flattener.Flattener;
import org.ehrbase.fhirbridge.TestFileLoader;
import org.ehrbase.fhirbridge.engine.ResourceTemplateProvider;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractMappingTestSetupIT extends AbstractSetupIT {
    protected TestFileLoader testFileLoader;

    public AbstractMappingTestSetupIT(String directory, Class<?> clazz) {
        super();
        this.testFileLoader = new TestFileLoader(directory, clazz, super.context);
    }

    public Diff compareCompositions(Javers javers, String paragonFilePath, CompositionEntity mappedComposition)
            throws IOException {
        RMObject composition = new CanonicalJson().unmarshal(testFileLoader.loadResourceToString(paragonFilePath), com.nedap.archie.rm.composition.Composition.class);
        ResourceTemplateProvider resourceTemplateProvider = new ResourceTemplateProvider("classpath:/opt/");
        resourceTemplateProvider.afterPropertiesSet();

        Flattener cut = new Flattener(resourceTemplateProvider);
        CompositionEntity paragonComposition = cut.flatten(composition, mappedComposition.getClass());
        Diff diff = javers.compare(paragonComposition, mappedComposition);
        diff.getChanges().forEach(System.out::println);
        return diff;
    }

    protected void create(String path) throws IOException {
        String resource = testFileLoader.loadResourceToString(path);
        MethodOutcome outcome = client.create().resource(resource.replaceAll(PATIENT_ID_TOKEN, patientId)).execute();

        assertNotNull(outcome.getId());
        assertEquals(true, outcome.getCreated());
    }

    public abstract Javers getJavers();

    public abstract Exception executeMappingException(String resource) throws IOException;

    public abstract void testMapping(String resourcePath, String paragonPath) throws IOException;

}
