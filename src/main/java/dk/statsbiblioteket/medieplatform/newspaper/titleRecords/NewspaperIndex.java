package dk.statsbiblioteket.medieplatform.newspaper.titleRecords;

import dk.statsbiblioteket.medieplatform.autonomous.*;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;

public class NewspaperIndex {
    private static final String FIELD_NAME_EDITION_AVIS_ID = "newspapr_edition_avisID";
    private static final String FIELD_NAME_EDITION_DATE_ISSUED = "newspapr_edition_dateIssued";
    HttpSolrServer summaSearch;

    /**
     * Constructor
     *
     * @param summaSearch Solr server to use for searching
     */
    public NewspaperIndex(HttpSolrServer summaSearch) {
        this.summaSearch = summaSearch;
    }

    /**
     * Get all editions that match given newspaper object ("titelpost") and date range
     *
     * @param avisID The newspaper ID, for example "adresseavisen1759", identifying the newspaper object to be matched
     * @param startDate The start of the date range to be matched
     * @param endDate The end of the date range to be matched
     * @return A list of the editions that match given newspaper object ("titelpost") and date range
     */
    public List<Item> getEditions(String avisID, String startDate, String endDate, ItemFactory<Item> itemFactory) {
        List<Item> editions;
        final int rows = Integer.MAX_VALUE;
        int start = 0;

        try {
            SolrQuery query = new SolrQuery();
            query.setQuery(constructQueryString(avisID, startDate, endDate));
            query.setRows(rows);  // Fetch size. Do not go over 1000 unless you specify fields to fetch not including content_text
            query.setStart(start);
            // IMPORTANT! Only use facets if needed.
            query.set("facet", "false");  // Very important. Must overwrite to false. Facets are very slow and expensive.

            query.setFields(SBOIEventIndex.UUID);

            QueryResponse response = summaSearch.query(query);
            SolrDocumentList results = response.getResults();
            editions = new ArrayList<>();
            for (SolrDocument result : results) {
                String uuid = result.getFirstValue(SBOIEventIndex.UUID).toString();

                editions.add(itemFactory.create(uuid));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return editions;
    }

    /**
     * Construct query string for Solr searching
     *
     * @param avisID The newspaper ID, for example "adresseavisen1759", identifying the newspaper object to be matched
     * @param startDate The start of the date range to be matched
     * @param endDate The end of the date range to be matched
     * @return The query string
     */
    private String constructQueryString(String avisID, String startDate, String endDate) {
        return "q=" + FIELD_NAME_EDITION_AVIS_ID + ":" + avisID + " AND " + FIELD_NAME_EDITION_DATE_ISSUED + ":" + "["
                + startDate + " TO " + endDate + "]";
    }
}
