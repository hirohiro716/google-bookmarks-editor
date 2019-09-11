package com.hirohiro716.web.google.bookmark;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import com.hirohiro716.StringConverter;
import com.hirohiro716.file.FileHelper;
import com.hirohiro716.javafx.LayoutHelper;
import com.hirohiro716.javafx.StageBuilder;
import com.hirohiro716.javafx.dialog.AbstractDialog.CloseEventHandler;
import com.hirohiro716.javafx.dialog.DialogResult;
import com.hirohiro716.javafx.dialog.confirm.Confirm;
import com.hirohiro716.javafx.web.WebEngineController;
import com.hirohiro716.javafx.web.WebEngineFlow;
import com.hirohiro716.javafx.web.WebEngineFlow.Task;
import com.hirohiro716.web.XMLCookieStore;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * GoogleBookmarksをブラウズするクラス.
 * @author hiro
 */
public class GoogleBookmarksBrowder {
    
    private static Stage STAGE = null;
    
    /**
     * WEBブラウザのStageを取得する.
     * @return Stage
     */
    public static Stage getStage() {
        return STAGE;
    }
    
    private static WebView WEB_VIEW = null;
    
    /**
     * WEBブラウザを取得する.
     * @return WebView
     */
    public static WebView getWebView() {
        return WEB_VIEW;
    }
    
    private static String SOURCE = null;
    
    /**
     * WEBブラウザからドキュメントのソースコードを取得する.
     * @return Source文字列
     */
    public static String getSourceCode() {
        return SOURCE;
    }
    
    /**
     * WEBブラウザを表示してログインする.
     * @param runnableAfterLogin ログイン実行後の処理
     */
    public static void show(Runnable runnableAfterLogin) {
        // ブラウザを準備
        WEB_VIEW = new WebView();
        String fileSeparator = FileHelper.FILE_SEPARATOR;
        String stringDirectory = StringConverter.join(System.getProperty("user.home"), fileSeparator, ".hirohiro716", fileSeparator, "google-bookmarks-editor", fileSeparator);
        File directory = new File(stringDirectory);
        if (directory.exists() == false) {
            directory.mkdir();
        }
        getWebView().getEngine().setUserDataDirectory(directory);
        try {
            CookieManager cookieManager;
            cookieManager = new CookieManager(new XMLCookieStore(new File(stringDirectory + "cookies.xml")), CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(cookieManager);
        } catch (IOException exception) {
            exception.printStackTrace();
            return;
        }
        AnchorPane pane = new AnchorPane(getWebView());
        LayoutHelper.setAnchor(getWebView(), 0, 0, 0, 0);
        StageBuilder stageBuilder = new StageBuilder();
        stageBuilder.getStage().setScene(new Scene(pane));
        getWebView().getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
                if (newValue == State.SUCCEEDED) {
                    SOURCE = (String) getWebView().getEngine().executeScript("document.documentElement.outerHTML");
                }
            }
        });
        getWebView().getEngine().titleProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue != null && newValue.length() > 0) {
                    stageBuilder.getStage().setTitle(newValue);
                }
            }
        });
        stageBuilder.show();
        STAGE = stageBuilder.getStage();
        FormHelper.applyIcon(getStage());
        FormHelper.applyBugFix(getStage());
        Confirm confirm = new Confirm();
        confirm.setTitle("ログイン");
        confirm.setMessage("ブラウザでGoogleにログインします。");
        confirm.setCloseEvent(new CloseEventHandler<DialogResult>() {
            @Override
            public void handle(DialogResult resultValue) {
                if (resultValue == DialogResult.CANCEL) {
                    getStage().close();
                    return;
                }
                WebEngineFlow flow = new WebEngineFlow(getWebView());
                flow.addTaskLoadURL("https://www.google.com/bookmarks/?hl=ja");
                flow.addTaskWaitForLoadElementById("sidenav");
                flow.addTaskLoadURL("https://www.google.com/bookmarks/lookup?output=xml");
                flow.addTaskWaitForLoadElementByTagName("bookmarks");
                flow.addTask(new Task() {
                    @Override
                    public void execute(WebEngineController controller) throws Exception {
                        getStage().hide();
                        Platform.runLater(runnableAfterLogin);
                    }
                });
                flow.execute();
            }
        });
        confirm.showOnPane(pane);
    }
    
    /**
     * WEBブラウザでページを表示する.
     * @param url
     * @param runnableAfterLoad 読み込み後の処理
     */
    public static void loadURL(String url, Runnable runnableAfterLoad) {
        WebEngineFlow flow = new WebEngineFlow(getWebView());
        getStage().show();
        flow.addTaskLoadURL(url);
        flow.addTask(new Task() {
            @Override
            public void execute(WebEngineController controller) throws Exception {
                getStage().hide();
                Platform.runLater(runnableAfterLoad);
            }
        });
        flow.execute();
    }
    
}
