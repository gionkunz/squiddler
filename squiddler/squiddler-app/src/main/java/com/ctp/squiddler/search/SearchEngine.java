package com.ctp.squiddler.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.log4j.Log4j;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;

import com.ctp.squiddler.IndexingConstants;

@Log4j
public class SearchEngine {
	private Directory index;
	private int numHits;

	public SearchEngine(Directory index, int numHits) {
		this.index = index;
		this.numHits = numHits;
	}

	public List<Document> performSearch(String queryString)
			throws CouldNotReadIndexException, ParseException {
		DirectoryReader directoryReader;
		try {
			directoryReader = DirectoryReader.open(index);
		} catch (IOException e) {
			throw new CouldNotReadIndexException(e);
		}

		String split[] = queryString.split("\\s");

		Query prefixQuery = new PrefixQuery(
				new Term(IndexingConstants.SIMPLE_TYPENAME, split[0]));
		Query regexQuery = new RegexpQuery(new Term(IndexingConstants.SIMPLE_TYPENAME, ".*"
				+ split[0] + ".*"));
		BooleanQuery query = new BooleanQuery();

		query.add(prefixQuery, Occur.SHOULD);
		query.add(regexQuery, Occur.SHOULD);

		if (split.length == 2) {
			Query contentQuery = new RegexpQuery(new Term(IndexingConstants.CONTENT, ".*"
					+ split[1].toLowerCase() + ".*"));
			query.add(contentQuery, Occur.MUST);
		}

		IndexSearcher searcher = new IndexSearcher(directoryReader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(numHits,
				true);
		try {
			searcher.search(query, collector);
		} catch (IOException e) {
			throw new CouldNotReadIndexException(e);
		}

		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		ArrayList<Document> documents = new ArrayList<>();

		for (ScoreDoc hit : hits) {
			try {
				documents.add(searcher.doc(hit.doc));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return documents;
	}
}
