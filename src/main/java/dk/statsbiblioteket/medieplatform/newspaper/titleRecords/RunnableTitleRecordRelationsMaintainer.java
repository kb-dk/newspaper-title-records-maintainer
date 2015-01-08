package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.fedora.structures.FedoraRelation;
import dk.statsbiblioteket.medieplatform.autonomous.*;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Ensures that the relations from editions to newspaper object "titelpost" are exactly those there should be, adding or removing
 * any that might be needed.
 */
public class RunnableTitleRecordRelationsMaintainer extends AbstractRunnableComponent {
    private static final Logger log = LoggerFactory.getLogger(RunnableTitleRecordRelationsMaintainer.class);
    private static final String URI_PREFIX = "info:fedora/";
    protected static final String SET_INACTIVE_COMMENT
            = "Title Record Maintainer sets this object inactive to update relations";
    protected static final String SETS_ACTIVE_COMMENT
            = "Title Record Maintainer sets this object active after updating relations";
    private final EnhancedFedora eFedora;
    private static final String EDITION_TO_NEWSPAPER_RELATION
            = "http://doms.statsbiblioteket.dk/relations/default/0/1/#isPartOfNewspaper";
    private static final String TITLE_RELATIONS_GENERATED = "Title_Relations_Generated";
    private ItemFactory<Item> itemFactory;
    private NewspaperIndex newspaperIndex;

    public RunnableTitleRecordRelationsMaintainer(Properties properties, EnhancedFedora eFedora, ItemFactory<Item> itemFactory,
                                                  NewspaperIndex newspaperIndex) {
        super(properties);
        this.eFedora = eFedora;
        this.itemFactory = itemFactory;
        this.newspaperIndex = newspaperIndex;
    }

    @Override
    public String getEventID() {
        return TITLE_RELATIONS_GENERATED;
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
        String avisID = xpath.selectString(newspaperDOM, "/v3:mods/v3:identifier[@type='title_family']/text()");
        String startDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='start']/text()");
        String endDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='end']/text()");

        // Get all editions that match given newspaper object ("titelpost") and date range, i.e. editions that SHOULD have the
        // relation
        List<Item> wantedEditions =  newspaperIndex.getEditions(avisID, startDate, endDate);

        // Get all editions that already HAVE the relation
        List<Item> editionsWithRelation = getEditionsWithRelation(domsID);

        // Add relations from editions that are wanted but aren't in editionsWithRelation
        List<Item> editionsToAdd = getEditionsWantedButWithoutRelation(wantedEditions, editionsWithRelation);
        for (Item toAdd : editionsToAdd) {
            addRelationFromEditionToNewspaper(toAdd, domsID);
        }

        // Remove relations that are in editionsWithRelation but aren't in wantedEditions
        List<Item> editionsToRemove = getEditionsWithRelationButUnwanted(wantedEditions, editionsWithRelation);
        for (Item toRemove : editionsToRemove) {
            removeRelationFromEditionToNewspaper(toRemove, domsID);
        }
        log.info("All work on {} done",item.getFullID());
    }

    /**
     * Get editions from which relations are wanted but that aren't in editionsWithRelation
     *
     * @param wantedEditions Editions that we want
     * @param editionsWithRelation Editions with a relation to "titelpost" that we already have
     * @return editions from which relations are wanted but that aren't in editionsWithRelation
     */
    private List<Item> getEditionsWantedButWithoutRelation(List<Item> wantedEditions, List<Item> editionsWithRelation) {
        List<Item> result = new ArrayList<>(wantedEditions);
        result.removeAll(editionsWithRelation);
        return result;
    }

    /**
     * Get editions that are in editionsWithRelation but aren't in wantedEditions
     *
     * @param wantedEditions Editions that we want
     * @param editionsWithRelation Editions with a relation to "titelpost" that we already have
     * @return editions that are in editionsWithRelation but aren't in wantedEditions
     */
    private List<Item> getEditionsWithRelationButUnwanted(List<Item> wantedEditions, List<Item> editionsWithRelation) {
        List<Item> result = new ArrayList<>(editionsWithRelation);
        result.removeAll(wantedEditions);
        return result;
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
        List<FedoraRelation> relations = eFedora.getInverseRelations(newspaperDomsID, EDITION_TO_NEWSPAPER_RELATION);

        // Collect the editions that these relations point from. (Relations point from Edition to Newspaper)
        for (FedoraRelation relation : relations) {
            editions.add(itemFactory.create(uriToDomsID(relation.getSubject())));
        }

        return editions;
    }

    /**
     * Add relation from given edition to newspaper object ("titelpost") with given PID (newspaperDomsID) in DOMS.
     * Objects are unpublished/published as needed.
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
        List<FedoraRelation> relations = eFedora.getNamedRelations(edition.getDomsID(), EDITION_TO_NEWSPAPER_RELATION, null);
        for (FedoraRelation relation : relations) {
            if (relation.getSubject().equals(newspaperDomsID)){
                return;
            }
        }

        // Add relation from edition to newspaper object ("titelpost")
        try {
            eFedora.addRelation(edition.getDomsID(), URI_PREFIX + edition.getDomsID(), EDITION_TO_NEWSPAPER_RELATION,
                    URI_PREFIX + newspaperDomsID, false, "linking to");
        } catch (BackendInvalidCredsException objectIsPublished) {
            // Edition was already published, so unpublish (set to "I" (inactive)) before adding
            eFedora.modifyObjectState(edition.getDomsID(), "I", SET_INACTIVE_COMMENT);
            try {
                eFedora.addRelation(edition.getDomsID(), URI_PREFIX + edition.getDomsID(), EDITION_TO_NEWSPAPER_RELATION,
                        URI_PREFIX + newspaperDomsID, false,
                                           "Adding relation " + EDITION_TO_NEWSPAPER_RELATION + " to " +
                                           URI_PREFIX + newspaperDomsID);
            } finally {
                // Re-publish (set to "A" (active))
                eFedora.modifyObjectState(edition.getDomsID(), "A", SETS_ACTIVE_COMMENT);
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
        List<FedoraRelation> relations = eFedora.getNamedRelations(edition.getDomsID(), EDITION_TO_NEWSPAPER_RELATION, null);
        for (FedoraRelation relation : relations) {
            if (relation.getObject().equals(URI_PREFIX + newspaperDomsID)) {
                try {
                    eFedora.deleteRelation(edition.getDomsID(), URI_PREFIX + edition.getDomsID(),
                            EDITION_TO_NEWSPAPER_RELATION, URI_PREFIX + newspaperDomsID, false,
                                                  "Deleting relation " + EDITION_TO_NEWSPAPER_RELATION + " to " +
                                                  URI_PREFIX + newspaperDomsID);
                } catch (BackendInvalidCredsException objectIsPublished) {
                    // Edition was already published, so unpublish (set to "I" (inactive)) before deleting
                    eFedora.modifyObjectState(edition.getDomsID(), "I", SET_INACTIVE_COMMENT);
                    try {
                        eFedora.deleteRelation(edition.getDomsID(), URI_PREFIX + edition.getDomsID(),
                                EDITION_TO_NEWSPAPER_RELATION, URI_PREFIX + newspaperDomsID, false, "Deleting relation "+EDITION_TO_NEWSPAPER_RELATION+" to " +
                                                      URI_PREFIX + newspaperDomsID);
                    } finally {
                        // Re-publish (set to "A" (active))
                        eFedora.modifyObjectState(edition.getDomsID(), "A", SETS_ACTIVE_COMMENT);
                    }
                }
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
        return uri.replace(URI_PREFIX, "");
    }
}
