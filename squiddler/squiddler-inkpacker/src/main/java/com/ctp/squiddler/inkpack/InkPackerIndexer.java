package com.ctp.squiddler.inkpack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.ctp.squiddler.IndexingConstants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InkPackerIndexer {
	private static Logger logger = Logger.getLogger(InkPackerIndexer.class);

	private File rootFolder;
	private List<String> fileIncludes;
	private List<String> fileExcludes;
	private List<String> folderIncludes;
	private List<String> folderExcludes;

	public void index(IndexWriter indexWriter) {
		recursivelyAddDocuments(indexWriter, rootFolder, rootFolder);
	}

	public void recursivelyAddDocuments(IndexWriter indexWriter,
			File rootFolder, File file) {

		// Process includes and excludes first
		if (file.isDirectory()) {
			if (!doesIncludeButNotExclude(folderIncludes, folderExcludes,
					file.getName())) {
				logger.debug("Ignoring folder " + file.getName()
						+ " and all subfiles");
				return;
			}
		} else {
			if (!doesIncludeButNotExclude(fileIncludes, fileExcludes,
					file.getName())) {
				logger.debug("Irgnoring file " + file.getName());
				return;
			}
		}

		final int depth = file.equals(rootFolder) ? 0 : StringUtils
				.countMatches(
						file.getParentFile()
								.getAbsolutePath()
								.substring(
										rootFolder.getAbsolutePath().length())
								.replaceAll("[/\\\\]", "/"), "/");

		logger.debug("Indexing " + file.getAbsolutePath() + " with depth "
				+ depth);

		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				recursivelyAddDocuments(indexWriter, rootFolder, subFile);
			}
		} else if (file.isFile() && file.getName().endsWith("html")) {
			// It's an html file so we now check what type of page it is and add
			// the index

			// Add basic fileds to document

			Document document = new Document();
			document.add(new StringField(IndexingConstants.DOCUMENT_PATH, file.getPath(),
					Field.Store.YES));
			document.add(new StringField(IndexingConstants.DOCUMENT_NAME, file.getName(),
					Field.Store.YES));
			document.add(new StringField(IndexingConstants.TOPIC_NAME, "java",
					Field.Store.YES));
			document.add(new DoubleField(IndexingConstants.TOPIC_VERSION, 1.6,
					Field.Store.YES));
			
			try {
				document.add(new TextField(IndexingConstants.CONTENT, new FileReader(file)));
			} catch (FileNotFoundException e) {
				logger.warn("Could not add document " + file.getAbsolutePath()
						+ " " + e.getMessage());
				return;
			}

			// Check for package, interface, class or enum
			org.jsoup.nodes.Document htmlDocument;
			try {
				htmlDocument = Jsoup.parse(file, "UTF-8");
			} catch (IOException e) {
				logger.warn("Could not parse HTML file "
						+ file.getAbsolutePath() + " " + e.getMessage());
				return;
			}

			Elements elements = htmlDocument
					.select("html head meta[name=keywords]");
			Iterator<Element> elementIterator = elements.iterator();
			if (!elementIterator.hasNext()) {
				logger.debug("Not a valid javadoc 1.6 file");
				return;
			}

			Element element = elementIterator.next();

			String[] info = element.attr("content").split(" ");
			String fullTypeName = info[0];
			String simpleTypeName = fullTypeName.split("\\.")[fullTypeName.split("\\.").length - 1];
			String type = info[1];

			// If class we need to further distinguish between enum and
			// class
			if (type.equals("class")
					&& htmlDocument.select("html body div.header h2.title")
							.first().attr("title").startsWith("Enum")) {
				type = "enum";
			}

			document.add(new StringField(IndexingConstants.TYPE, type, Field.Store.YES));
			document.add(new StringField(IndexingConstants.FULL_TYPENAME, fullTypeName, Field.Store.YES));
			document.add(new StringField(IndexingConstants.SIMPLE_TYPENAME, simpleTypeName, Field.Store.YES));

			// Adding constructed document to index
			try {
				logger.debug("Adding document " + document);
				indexWriter.addDocument(document);
			} catch (IOException e) {
				throw new CouldNotAddToIndexException(e);
			}
		}
	}

	protected boolean matchesOnePattern(List<String> patterns, String subject) {
		for (String pattern : patterns) {
			if (subject.matches(pattern)) {
				return true;
			}
		}

		return false;
	}

	protected boolean doesIncludeButNotExclude(List<String> includes,
			List<String> excludes, String subject) {
		return matchesOnePattern(includes, subject)
				&& !matchesOnePattern(excludes, subject);
	}
}
