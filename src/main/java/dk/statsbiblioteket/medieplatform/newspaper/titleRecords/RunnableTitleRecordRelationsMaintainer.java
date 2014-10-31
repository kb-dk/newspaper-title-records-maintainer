package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.medieplatform.autonomous.Item;
import dk.statsbiblioteket.medieplatform.autonomous.ResultCollector;
import dk.statsbiblioteket.medieplatform.autonomous.RunnableComponent;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class RunnableTitleRecordRelationsMaintainer implements RunnableComponent<Item> {
    private final Properties properties;
    private final EnhancedFedora eFedora;
    private String editionToNewspaperRelation = "isPartOfNewspaper";

    public RunnableTitleRecordRelationsMaintainer(Properties properties, EnhancedFedora eFedora) {

        this.properties = properties;
        this.eFedora = eFedora;
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

        // Add relations from all matching editions to received newspaper object ("titelpost")
        Iterator<Item> editions =  newspaperIndex.getEditions(avisID, startDate, endDate);
        while (editions.hasNext()) {
            Item next = editions.next();
            addRelationFromEditionToNewspaper(next, domsID);
        }

        // Remove relations from editions that are no longer matched
        Iterator<Item> nonmatchingEditions =  getNonmatchingEditions(avisID, startDate, endDate);
        while (nonmatchingEditions.hasNext()) {
            Item next = editions.next();
            removeRelationFromEditionToNewspaper(next, domsID);
        }
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


}
