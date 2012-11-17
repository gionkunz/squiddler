package com.ctp.squiddler.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import lombok.extern.log4j.Log4j;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.FSDirectory;

import com.ctp.squiddler.search.CouldNotReadIndexException;
import com.ctp.squiddler.search.SearchEngine;

@Log4j
public class MainViewController implements Initializable {
	@FXML
	private TextField searchField;
	@FXML
	private ListView<Document> resultListView;
	@FXML
	private WebView documentationWebView;

	private ObservableList<Document> resultList;
	private SearchEngine searchEngine;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		try {
			FSDirectory index = FSDirectory.open(new File(
					"../squiddler-inkpack/jdk-7-inkpack/index-data"));
			searchEngine = new SearchEngine(index, 100);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		resultList = FXCollections.observableArrayList();
		resultListView.setItems(resultList);

		resultListView
				.setCellFactory(new Callback<ListView<Document>, ListCell<Document>>() {
					@Override
					public ListCell<Document> call(ListView<Document> list) {
						return new DocumentListCell();
					}
				});

		resultListView.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Document>() {
					@Override
					public void changed(
							ObservableValue<? extends Document> observable,
							Document oldValue, Document newValue) {
						log.info("New value: " + newValue);

						if (newValue != null && newValue.get("path") != null) {
							try {
								URL url = new File("../squiddler-inkpack/"
										+ newValue.get("path")).toURI().toURL();
								log.info("Loading URL: " + url.toExternalForm());
								documentationWebView.getEngine().load(
										url.toExternalForm());
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				});

		searchField.textProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				
				if(newValue != null) {

					resultList.clear();
					try {
						resultList.addAll(searchEngine.performSearch(newValue));
					} catch (CouldNotReadIndexException | ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	static class DocumentListCell extends ListCell<Document> {
		@Override
		public void updateItem(Document item, boolean empty) {
			super.updateItem(item, empty);
			Rectangle rect = new Rectangle(20, 20);
			rect.setStroke(Color.LIGHTGREY);
			rect.setStyle("-fx-effect: dropshadow( one-pass-box , rgba(0, 0, 0, 0.5) , 6 , 0.0 , 0 , 0 )");
			if (item != null) {
				String type = item.get("type");

				setTooltip(new Tooltip(item.get("fulltypename")));

				if (type.equals("package")) {
					rect.setFill(Color.web("#ECD078"));
				} else if (type.equals("class")) {
					rect.setFill(Color.web("#53777A"));
				} else if (type.equals("interface")) {
					rect.setFill(Color.web("#C02942"));
				} else if (type.equals("enum")) {
					rect.setFill(Color.web("#542437"));
				}

				String display = item.get("simpletypename");
				if (type.equals("package")) {
					display = item.get("fulltypename");
				}

				setGraphic(rect);
				setText(type + ": " + display);
			}
		}
	}
}
