package com.hirohiro716.web.google;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * アプリケーションの開始.
 * @author hiro
 */
public class Run extends Application {
	
	/**
	 * アプリケーションの開始.
	 * @param args 
	 */
	public static void main(String[] args) {
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		Run.launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		GoogleBookmarksBrowder.show(new Runnable() {
			@Override
			public void run() {
				try {
					GoogleBookmarksEditor adder = new GoogleBookmarksEditor();
					adder.show();
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
		});
	}
	
}
