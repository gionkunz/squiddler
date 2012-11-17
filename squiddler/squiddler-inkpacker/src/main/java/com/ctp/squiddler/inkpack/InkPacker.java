package com.ctp.squiddler.inkpack;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import com.beust.jcommander.JCommander;

public class InkPacker {
	private static Logger logger = Logger.getLogger(InkPacker.class);

	public static void main(String[] args) {
		InkPacker inkPack = new InkPacker();
		ApplicationParameters params = new ApplicationParameters();
		inkPack.run(new JCommander(params, args), params);
	}

	public void run(JCommander commander, ApplicationParameters params) {
		if (params.isHelp()) {
			commander.usage();
		} else {
			params.getTarget().mkdirs();
			
			File sourceFolder = new File(params.getSourceFolders().get(0));
			
			
			
			File docRoot = new File(params.getTarget(), "doc");
			docRoot.mkdir();
			try {
				logger.info("Copying documentation");
				FileUtils.copyDirectory(sourceFolder, docRoot);
			} catch (IOException e) {
				logger.error("Could not copy documentation source to target folder! "+e.getMessage());
				return;
			}
			
			Directory directory;
			try {
				directory = new NIOFSDirectory(new File(params.getTarget(), "index-data"));
			} catch (IOException e) {
				logger.error("Could not open index file! "+e.getMessage());
				return;
			}
			
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40,
					analyzer);

			try {
				IndexWriter indexWriter = new IndexWriter(directory, config);
				// TODO Handle multiple documentation folders
				

				InkPackerIndexer indexer = new InkPackerIndexer(docRoot,
						Arrays.asList(new String[] { "^.+\\.html$" }),
						Arrays.asList(new String[] { "package-frame\\.html",
								"package-tree\\.html", "package-use\\.html",
								"index\\.html", "allclasses-frame\\.html",
								"allclasses-noframe\\.html",
								"deprecated-list\\.html", "help-doc\\.html",
								"overview-frame\\.html",
								"overview-summary\\.html",
								"overview-tree\\.html", "package-list",
								"serialized-form\\.html",
								"constant-values\\.html" }),
						Arrays.asList(new String[] { ".+" }),
						Arrays.asList(new String[] { "index-files",
								"resources", "class-use" }));
				
				logger.info("Indexing documentation");
				indexer.index(indexWriter);
				indexWriter.commit();
				indexWriter.close();
				logger.debug("Index writer commited");
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
