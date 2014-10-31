package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.medieplatform.autonomous.Item;

import java.util.Iterator;

public class NewspaperIndex {

    /**
     * Get all newspaper editions matching the given
     *
     * @param avisID
     * @param startDate
     * @param endDate
     * @return
     */
    public Iterator<Item> getEditions(String avisID, String startDate, String endDate) {
        // Se i SolrProxyIterator for hvordan man tilgår solr til denne søgning...
        return null;
    }



}
