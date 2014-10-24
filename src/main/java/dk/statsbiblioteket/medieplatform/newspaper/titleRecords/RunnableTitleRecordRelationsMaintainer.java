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
    private String predicate = "";
    private String newspaperRelation;

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
        //What should this component do??
        //First, the item is a newspaper object
        //It will trigger if the newspaper object have changed
        //It should find all the edition objects, which correspond to this newspaper and date range

        String domsID = item.getDomsID();
        String newspaperMods = eFedora.getXMLDatastreamContents(domsID, "MODS");
        Document newspaperDOM = DOM.stringToDOM(newspaperMods, true);
        XPathSelector xpath = DOM.createXPathSelector("v3", "http://www.loc.gov/mods/v3");
        String avisID = xpath.selectString(newspaperDOM, "/v3:mods/v3:identifier[@type='ninestars avis id']/text()");
        String startDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='start']/text()");
        String endDate = xpath.selectString(newspaperDOM, "/v3:mods/v3:originInfo/v3:dateIssued[@point='end']/text()");

        NewspaperIndex newspaperIndex = null;
        Iterator<Item> editions =  newspaperIndex.getEditions(avisID,startDate,endDate);

        while (editions.hasNext()) {
            Item next = editions.next();
            doWorkOnEdition(next, domsID);
        }
    }

    private void doWorkOnEdition(Item edition, String newspaperDomsID) throws
                                                                                                       BackendMethodFailedException,
                                                                                                       BackendInvalidResourceException,
                                                                                                       BackendInvalidCredsException {
        List<FedoraRelation> relations = eFedora.getNamedRelations(edition.getDomsID(), predicate, null);
        for (FedoraRelation relation : relations) {
            if (relation.getSubject().equals(newspaperDomsID)){
                return;
            }
        }
        try {
            eFedora.addRelation(edition.getDomsID(), edition.getDomsID(), newspaperRelation, newspaperDomsID, false, "linking to");
        } catch (BackendInvalidCredsException objectIsPublished){
            eFedora.modifyObjectState(edition.getDomsID(),"I","comment");
            try {
                eFedora.addRelation(edition.getDomsID(),
                                           edition.getDomsID(), newspaperRelation,
                                           newspaperDomsID,
                                           false,
                                           "linking to");
            } finally {
                eFedora.modifyObjectState(edition.getDomsID(), "A", "comment");
            }
        }

    }
}
