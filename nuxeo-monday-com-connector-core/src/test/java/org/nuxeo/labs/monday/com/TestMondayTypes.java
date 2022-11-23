package org.nuxeo.labs.monday.com;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "nuxeo-monday-com-connector-core" })
public class TestMondayTypes {
    @Inject
    CoreSession session;

    @Test
    public void testFacetAndSchema() {
        DocumentModel doc = session.createDocumentModel("/", "File", "File");
        doc.addFacet("Monday");
        doc.setPropertyValue("monday:boardId","123");
        doc.setPropertyValue("monday:pulseId","abc");
        doc =session.createDocument(doc);
        assertTrue(doc.hasFacet("Monday"));
        assertEquals("123",doc.getPropertyValue("monday:boardId"));
        assertEquals("abc",doc.getPropertyValue("monday:pulseId"));
    }
}
