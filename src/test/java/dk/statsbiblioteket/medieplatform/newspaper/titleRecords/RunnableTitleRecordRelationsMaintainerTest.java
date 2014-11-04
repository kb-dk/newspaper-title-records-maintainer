package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import org.testng.annotations.Test;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.ItemFactory;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;

import java.util.Arrays;
import java.util.Properties;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RunnableTitleRecordRelationsMaintainerTest {

    private static final String URL_PREFIX = "info:fedora/";
    private static final String PREDICATE = "http://doms.statsbiblioteket.dk/relations/default/0/1/#isPartOfNewspaper";
    private static final String DOMS_SUBJECT = "uuid:0c1969ca-94be-4ebb-abab-0bd8130e59d7";
    private static final String DOMS_OBJECT_1 = "uuid:38deefa7-381f-4abf-a6c1-a3531b54f997";
    private static final String DOMS_OBJECT_2 = "uuid:781732b1-ca9a-46d4-94cd-ae1c0b7f1ebf";
    private static final String DOMS_OBJECT_3 = "uuid:aff7e1f0-4242-4fc1-877d-6382f2bbaaa9";
    private static final String MESSAGE = "linking to";

    /**
     * Test where one relation should be added.
     *
     * Index query returns three items. DOMS returns two known.
     *
     * The result should be that the missing relation is added.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemAddOneRelation() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1), new Item(DOMS_OBJECT_2), new Item(DOMS_OBJECT_3)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);

        //One relation should be added
        verify(enhancedFedoraMock).addRelation(DOMS_OBJECT_3, URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT, false,
                                               MESSAGE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }

    /**
     * Test where one relation should be removed.
     *
     * Index query returns two items. DOMS returns three known.
     *
     * The result should be that the superfluous relation is removed.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemRemoveOneRelation() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1), new Item(DOMS_OBJECT_2)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);


        //One relation should be deleted
        verify(enhancedFedoraMock).deleteRelation(DOMS_OBJECT_3, URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT,
                                                  false, MESSAGE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }

    /**
     * Test where no relations should be added or removed.
     *
     * Index query returns three items. DOMS returns the same three known.
     *
     * The result should be that no relations should be added or removed.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemNoRelationsChanged() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1), new Item(DOMS_OBJECT_2), new Item(DOMS_OBJECT_3)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }

    /**
     * Test where two relations should be added.
     *
     * Index query returns three items. DOMS returns one known.
     *
     * The result should be that the missing two relations are added.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemAddMultipleRelations() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1), new Item(DOMS_OBJECT_2), new Item(DOMS_OBJECT_3)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);

        //Two relations should be added
        verify(enhancedFedoraMock).addRelation(DOMS_OBJECT_2, URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT, false,
                                               MESSAGE);
        verify(enhancedFedoraMock).addRelation(DOMS_OBJECT_3, URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT, false,
                                               MESSAGE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }

    /**
     * Test where two relations should be removed.
     *
     * Index query returns one item. DOMS returns three known.
     *
     * The result should be that the superfluous relations are removed.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemRemoveMultipleRelations() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);

        //Two relations should be removed
        verify(enhancedFedoraMock).deleteRelation(DOMS_OBJECT_2, URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT,
                                                  false, MESSAGE);
        verify(enhancedFedoraMock).deleteRelation(DOMS_OBJECT_3, URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT,
                                                  false, MESSAGE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }

    /**
     * Test where a mix of relations should be added and removed.
     *
     * Index query returns one item. DOMS returns two different items known.
     *
     * The result should be that one relation is added and the superfluous relations are removed.
     *
     * @throws Exception
     */
    @Test
    public void testDoWorkOnItemAddAndRemoveMultipleRelations() throws Exception {
        //Mock item, return id when asked
        Item itemMock = mock(Item.class);
        when(itemMock.getDomsID()).thenReturn(DOMS_SUBJECT);

        //Mock item factory, knows how to create the test objects
        ItemFactory<Item> itemFactoryMock = mock(ItemFactory.class);
        when(itemFactoryMock.create(DOMS_OBJECT_1)).thenReturn(new Item(DOMS_OBJECT_1));
        when(itemFactoryMock.create(DOMS_OBJECT_2)).thenReturn(new Item(DOMS_OBJECT_2));
        when(itemFactoryMock.create(DOMS_OBJECT_3)).thenReturn(new Item(DOMS_OBJECT_3));

        //Mock result collector.
        ResultCollector resultCollectorMock = mock(ResultCollector.class);

        //Mock Fedora. Will return MODS when asked, and list three existing relations
        EnhancedFedora enhancedFedoraMock = mock(EnhancedFedora.class);
        when(enhancedFedoraMock.getXMLDatastreamContents(DOMS_SUBJECT, "MODS")).thenReturn("<mods xmlns=\"http://www.loc.gov/mods/v3\"><identifier type=\"ninestars avis id\">avis</identifier><originInfo><dateIssued point=\"start\">1970-01-01T01:00:00.000+01:00</dateIssued><dateIssued point=\"end\">1980-01-01T01:00:00.000+01:00</dateIssued></originInfo></mods>");
        when(enhancedFedoraMock.getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE)).thenReturn(
                Arrays.asList(new FedoraRelation(URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT),
                              new FedoraRelation(URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT)));

        //Mock newspaper index. Will return three items when asked
        NewspaperIndex newspaperIndexMock = mock(NewspaperIndex.class);
        when(newspaperIndexMock.getEditions(anyString(), anyString(), anyString())).thenReturn(
                Arrays.asList(new Item(DOMS_OBJECT_1)));

        //Call the component with the mocks
        new RunnableTitleRecordRelationsMaintainer(new Properties(), enhancedFedoraMock, itemFactoryMock, newspaperIndexMock)
            .doWorkOnItem(itemMock, resultCollectorMock);

        //Verify expected DOMS calls
        verify(enhancedFedoraMock).getXMLDatastreamContents(DOMS_SUBJECT, "MODS");
        verify(enhancedFedoraMock).getInverseRelations(URL_PREFIX + DOMS_SUBJECT, PREDICATE);

        //One relation should be added
        verify(enhancedFedoraMock).addRelation(DOMS_OBJECT_1, URL_PREFIX + DOMS_OBJECT_1, PREDICATE, URL_PREFIX + DOMS_SUBJECT, false,
                                               MESSAGE);
        //Two relations should be deleted
        verify(enhancedFedoraMock).deleteRelation(DOMS_OBJECT_2, URL_PREFIX + DOMS_OBJECT_2, PREDICATE, URL_PREFIX + DOMS_SUBJECT,
                                                  false, MESSAGE);
        verify(enhancedFedoraMock).deleteRelation(DOMS_OBJECT_3, URL_PREFIX + DOMS_OBJECT_3, PREDICATE, URL_PREFIX + DOMS_SUBJECT,
                                                  false, MESSAGE);

        //No more calls should be made to DOMS, and none to the result collector
        verifyNoMoreInteractions(enhancedFedoraMock, resultCollectorMock);
    }
}