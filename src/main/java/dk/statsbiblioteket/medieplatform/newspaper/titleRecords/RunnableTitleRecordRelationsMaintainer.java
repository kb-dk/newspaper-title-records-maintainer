package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.medieplatform.autonomous.*;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class RunnableTitleRecordRelationsMaintainer implements RunnableComponent<Item> {
    private final Properties properties;
    private final EnhancedFedora eFedora;
    private String editionToNewspaperRelation = "isPartOfNewspaper";
    private ItemFactory<Item> itemFactory;

    public RunnableTitleRecordRelationsMaintainer(Properties properties, EnhancedFedora eFedora, ItemFactory<Item> itemFactory) {
        this.properties = properties;
        this.eFedora = eFedora;
        this.itemFactory = itemFactory;
    }

    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public String getComponentVersion() {
        return null;
    }

    @Override
    public String getEventID() {
        return null;
    }

    @Override
    public void doWorkOnItem(Item item, ResultCollector resultCollector) throws Exception {
        // We assume that the received item is a newspaper object ("titelpost").
        // This method is called whenever a newspaper object has changed

        // Get PID for the newspaper object
        String domsID = item.getDomsID();

        String newspaperMods = eFedora.getXMLDatastreamContents(domsID, "MODS");
        Document newspaperDOM = DOM.stringToDOM(newspaperMods, true);
        XPathSelector xpath = DOM.createXPathSelector("v3", "http://www.loc.gov/mods/v3");
        String avisID = xpath.selectString(newspaperDOM, "/v3:mods/v3:identifier[@type='ninestars avis id']/text()");
        String startDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='start']/text()");
        String endDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='end']/text()");

        NewspaperIndex newspaperIndex = new NewspaperIndex();

        // Get all editions that match given newspaper object ("titelpost") and date range, i.e. editions that SHOULD have the
        // relation
        List<Item> wantedEditions =  newspaperIndex.getEditions(avisID, startDate, endDate);

        // Get all editions that already HAVE the relation
        List<Item> editionsWithRelation = getEditionsWithRelation(domsID);

        // Now we want wantedEditions to = editionsWithRelation

        // Add relations from editions that are wanted but aren't in editionsWithRelation TODO
        getEditionsWantedButWithoutRelation(wantedEditions, editionsWithRelation);

        // Remove relations that are in editionsWithRelation but aren't in wantedEditions TODO
        getEditionsWithRelationButUnwanted(wantedEditions, editionsWithRelation);


        // Add relations from all matching editions to received newspaper object ("titelpost")
        /*while (editions.hasNext()) {
            Item next = editions.next();
            addRelationFromEditionToNewspaper(next, domsID);
        }

        // Remove relations from editions that are no longer matched
        Iterator<Item> nonmatchingEditions =  getNonmatchingEditions(avisID, startDate, endDate);
        while (nonmatchingEditions.hasNext()) {
            Item next = editions.next();
            removeRelationFromEditionToNewspaper(next, domsID);
        }*/
    }

    /**
     * Get relations that are in editionsWithRelation but aren't in wantedEditions
     *
     * @param wantedEditions
     * @param editionsWithRelation
     * @return
     */
    private List<Item> getEditionsWithRelationButUnwanted(List<Item> wantedEditions, List<Item> editionsWithRelation) {
        return null;
    }

    /**
     * Get relations from editions that are wanted but aren't in editionsWithRelation
     *
     * @param wantedEditions
     * @param editionsWithRelation
     */
    private List<Item> getEditionsWantedButWithoutRelation(List<Item> wantedEditions, List<Item> editionsWithRelation) {
        return null;
    }

    /**
     * Get all editions that have the wanted relation to newspaper object ("titelpost") with given DOMS PID
     *
     * @param newspaperDomsID Newspaper object ("titelpost") to which relation should go
     * @return All editions that have the wanted relation to newspaper object ("titelpost") with given DOMS PID
     */
    private List<Item> getEditionsWithRelation(String newspaperDomsID) throws
            BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {

        List<Item> editions = new ArrayList<>();

        // Get all relations that go from an edition to given newspaper object ("titelpost")
        List<FedoraRelation> relations = eFedora.getInverseRelations(newspaperDomsID, editionToNewspaperRelation);

        // Collect the editions that these relations point from. (Relations point from Object to Subject)
        for (FedoraRelation relation : relations) {
            editions.add(itemFactory.create(uriToDomsID(relation.getObject())));
        }

        return editions;
    }

    /**
     * Get all editions that do not match given strings
     *
     * @param avisID
     * @param startDate
     * @param endDate
     * @return
     */
    private Iterator<Item> getNonmatchingEditions(String avisID, String startDate, String endDate) {
        // TODO
        return null;
    }

    /**
     * Add relation from given edition to newspaper object ("titelpost") with given PID (newspaperDomsID) in DOMS
     *
     * @param edition The edition which should be at the "source" end of the wanted relation
     * @param newspaperDomsID The DOMS PID of the newspaper object ("titelpost") which should be the "target" of wanted relation
     * @throws BackendMethodFailedException
     * @throws BackendInvalidResourceException
     * @throws BackendInvalidCredsException
     */
    private void addRelationFromEditionToNewspaper(Item edition, String newspaperDomsID) throws
            BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {

        // If edition already has wanted relation, nothing to do here, return
        List<FedoraRelation> relations = eFedora.getNamedRelations(edition.getDomsID(), editionToNewspaperRelation, null);
        for (FedoraRelation relation : relations) {
            if (relation.getSubject().equals(newspaperDomsID)){
                return;
            }
        }

        // Add relation from edition to newspaper object ("titelpost")
        try {
            eFedora.addRelation(edition.getDomsID(), edition.getDomsID(), editionToNewspaperRelation, newspaperDomsID, false,
                    "linking to");
        } catch (BackendInvalidCredsException objectIsPublished) {
            // Edition was already published, so unpublish (set to "I" (inactive)) before adding
            eFedora.modifyObjectState(edition.getDomsID(), "I", "comment");
            try {
                eFedora.addRelation(edition.getDomsID(), edition.getDomsID(), editionToNewspaperRelation, newspaperDomsID, false,
                        "linking to");
            } finally {
                // Re-publish (set to "A" (active))
                eFedora.modifyObjectState(edition.getDomsID(), "A", "comment");
            }
        }
    }

    /**
     * Add relation from given edition to newspaper object ("titelpost") with given PID (newspaperDomsID) in DOMS, if it exists
     *
     * @param edition The edition from which the possible relation to newspaperDomsID should be removed
     * @param newspaperDomsID The target of the relations from edition that should be removed
     * @throws BackendMethodFailedException
     * @throws BackendInvalidResourceException
     * @throws BackendInvalidCredsException
     */
    private void removeRelationFromEditionToNewspaper(Item edition, String newspaperDomsID) throws
            BackendMethodFailedException, BackendInvalidResourceException, BackendInvalidCredsException {

        // If edition has relation to newspaperDomsID, remove it
        List<FedoraRelation> relations = eFedora.getNamedRelations(edition.getDomsID(), editionToNewspaperRelation, null);
        for (FedoraRelation relation : relations) {
            if (relation.getSubject().equals(newspaperDomsID)){
                eFedora.deleteRelation(edition.getDomsID(), edition.getDomsID(), editionToNewspaperRelation, newspaperDomsID,
                        false, "linking to");
            }
        }
    }

    /**
     * Remove the "info:fedora/" prefix from uri, making it a proper DOMS PID (starting with "uuid")
     *
     * @param uri The uri from which to remove prefix
     * @return The DOMS PID
     */
    private String uriToDomsID(String uri) {
        return uri.replace("info:fedora/", "");
    }
}
