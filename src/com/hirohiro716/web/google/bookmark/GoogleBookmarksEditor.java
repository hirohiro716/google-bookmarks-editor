package com.hirohiro716.web.google.bookmark;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.hirohiro716.ExceptionHelper;
import com.hirohiro716.RudeArray;
import com.hirohiro716.StringConverter;
import com.hirohiro716.file.xml.XML;
import com.hirohiro716.javafx.GenerationalRunLater;
import com.hirohiro716.javafx.IMEHelper;
import com.hirohiro716.javafx.control.AutoCompleteTextField;
import com.hirohiro716.javafx.control.IMEOffButton;
import com.hirohiro716.javafx.control.LimitTextField;
import com.hirohiro716.javafx.control.table.EditableTable.FixControlFactory;
import com.hirohiro716.javafx.control.table.RudeArrayTable;
import com.hirohiro716.javafx.control.table.RudeArrayTable.ControlFactory;
import com.hirohiro716.javafx.data.AbstractEditor;
import com.hirohiro716.javafx.dialog.AbstractDialog.CloseEventHandler;
import com.hirohiro716.javafx.dialog.alert.InstantAlert;
import com.hirohiro716.javafx.dialog.sort.SortDialog;
import com.hirohiro716.javafx.web.WebEngineController;
import com.hirohiro716.javafx.web.WebEngineFlow;
import com.hirohiro716.javafx.web.WebEngineFlow.Task;
import com.hirohiro716.robot.InterfaceTypingRobotJapanese.IMEMode;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * GoogleBookmarksを編集する画面を表示する.
 * @author hiro
 */
public class GoogleBookmarksEditor extends AbstractEditor<ArrayList<RudeArray>> {
    
    @Override
    protected void editDataController() throws Exception {
        ArrayList<RudeArray> rows = new ArrayList<>();
        XML xml = new XML();
        xml.importFromSourceString(GoogleBookmarksBrowder.getSourceCode());
        for (Node bookmark : xml.findNodesByName("bookmark")) {
            RudeArray row = new RudeArray();
            Node titleNode = XML.findNodesByName("title", bookmark).get(0);
            row.put("title", titleNode.getTextContent());
            Node urlNode = XML.findNodesByName("url", bookmark).get(0);
            row.put("url", urlNode.getTextContent());
            ArrayList<Node> labelsNode = XML.findNodesByName("labels", bookmark);
            if (labelsNode.size() > 0) {
                Node labelNode = XML.findNodeByName("label", labelsNode.get(0));
                row.put("label", labelNode.getTextContent());
            }
            rows.add(row);
        }
        this.setDataController(rows);
    }
    
    @FXML
    private AnchorPane paneRoot;
    
    @FXML
    private Label labelTitle;
    
    @FXML
    private RudeArrayTable rudeArrayTable;
    
    @FXML
    private Button buttonAdd;

    @FXML
    private Button buttonSort;
    
    @FXML
    private Button buttonImport;
    
    @FXML
    private Button buttonExecute;
    
    @Override
    protected void beforeShowPrepare() throws Exception {
        GoogleBookmarksEditor editor = this;
        // FXMLを読んでStageを生成
        this.setFxml(this.getClass().getResource(this.getClass().getSimpleName() + ".fxml"));
        String title = "Google Bookmarks 編集";
        this.getStage().setTitle(title);
        this.labelTitle.setText(title);
        FormHelper.applyIcon(this.getStage());
        FormHelper.applyBugFix(this.getStage());
        // コントロールの初期化
        this.prepareControl();
        // ラベルコンボボックス
        this.updateLabelsList();
        // 行追加
        this.buttonAdd.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                RudeArray newRow = new RudeArray();
                editor.rudeArrayTable.addRow(newRow);
                editor.rudeArrayTable.loadMoreRows();
                GenerationalRunLater.runLater(500, new Runnable() {
                    @Override
                    public void run() {
                        editor.rudeArrayTable.getControl(newRow, "title").requestFocus();
                    }
                });
            }
        });
        // 並び替え
        this.buttonSort.setOnAction(this.sortEvent);
        // HTMLから取り込み
        this.buttonImport.setOnAction(this.importEvent);
        // 実行
        this.buttonExecute.setOnAction(this.executeEvent);
    }
    
    /**
     * コントロールの初期化.
     */
    private void prepareControl() {
        GoogleBookmarksEditor editor = this;
        RudeArrayTable table = this.rudeArrayTable;
        this.rudeArrayTable.setLoadRowsCount(100);
        this.rudeArrayTable.addColumnButton("del", "削除", new FixControlFactory<RudeArray, IMEOffButton>() {
            @Override
            public IMEOffButton newInstance(RudeArray item) {
                IMEOffButton button = new IMEOffButton("削除");
                button.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        table.removeRow(item);
                    }
                });
                return button;
            }
        });
        this.rudeArrayTable.addColumnTextField("title", "タイトル", new ControlFactory<LimitTextField>() {
            @Override
            public LimitTextField newInstance(RudeArray item) {
                LimitTextField textField = new LimitTextField();
                IMEHelper.apply(textField, IMEMode.HIRAGANA);
                return textField;
            }
        });
        this.rudeArrayTable.getHeaderLabel("title").setPrefWidth(260);
        this.rudeArrayTable.addColumnTextField("url", "URL", new ControlFactory<LimitTextField>() {
            @Override
            public LimitTextField newInstance(RudeArray item) {
                LimitTextField textField = new LimitTextField();
                IMEHelper.apply(textField, IMEMode.OFF);
                return textField;
            }
        });
        this.rudeArrayTable.getHeaderLabel("url").setPrefWidth(260);
        this.rudeArrayTable.addColumnTextField("label", "ラベル", new ControlFactory<AutoCompleteTextField>() {
            @Override
            public AutoCompleteTextField newInstance(RudeArray item) {
                AutoCompleteTextField textField = new AutoCompleteTextField();
                textField.setItems(editor.labelsList);
                textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                        if (newValue) {
                            textField.showItems();
                        }
                    }
                });
                IMEHelper.apply(textField, IMEMode.HIRAGANA);
                return textField;
            }
        });
        this.rudeArrayTable.getHeaderLabel("label").setPrefWidth(120);
        for (RudeArray row : this.getDataController()) {
            this.rudeArrayTable.addRow(row);
        }
        GenerationalRunLater.runLater(1, new Runnable() {
            @Override
            public void run() {
                editor.rudeArrayTable.loadMoreRows();
            }
        });
    }
    
    private ObservableList<String> labelsList = FXCollections.observableArrayList();
    
    /**
     * ラベル一覧の配列を更新する.
     */
    private void updateLabelsList() {
        this.labelsList.clear();
        for (RudeArray row: this.rudeArrayTable.getItems()) {
            String label = row.getString("label");
            if (label != null && this.labelsList.contains(label) == false) {
                this.labelsList.add(label);
            }
        }
    }
    
    /**
     * 並び替えを行う.
     */
    private EventHandler<ActionEvent> sortEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            GoogleBookmarksEditor editor = GoogleBookmarksEditor.this;
            RudeArrayTable table = editor.rudeArrayTable;
            LinkedHashMap<RudeArray, String> rows = new LinkedHashMap<>();
            for (RudeArray row: table.getItems()) {
                String title = row.getString("title");
                String label = row.getString("label");
                if (label != null && label.length() > 0) {
                    title = StringConverter.join("[", label, "] ", title);
                }
                rows.put(row, title);
            }
            SortDialog<RudeArray> dialog = new SortDialog<>(rows);
            dialog.setTitle("並び替え");
            dialog.setMessage("ブックマークを並び替えてください。");
            dialog.setCloseEvent(new CloseEventHandler<LinkedHashMap<RudeArray,String>>() {
                @Override
                public void handle(LinkedHashMap<RudeArray, String> resultValue) {
                    if (resultValue == null) {
                        return;
                    }
                    table.clearRows();
                    for (RudeArray row: resultValue.keySet()) {
                        table.addRow(row);
                    }
                    table.loadMoreRows();
                }
            });
            dialog.showOnPane(editor.paneRoot);
        }
    };
    
    /**
     * HTMLからインポートする.
     */
    private EventHandler<ActionEvent> importEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            GoogleBookmarksEditor editor = GoogleBookmarksEditor.this;
            FileChooser chooser = new FileChooser();
            chooser.setTitle("ブックマークHTMLの選択");
            chooser.getExtensionFilters().add(new ExtensionFilter("HTML", "*.html", "*.htm"));
            File file = chooser.showOpenDialog(editor.getStage());
            if (file == null) {
                return;
            }
            try {
                GoogleBookmarksBrowder.loadURL(file.toURI().toURL().toString(), new Runnable() {
                    @Override   
                    public void run() {
                        WebEngineController controller = new WebEngineController(GoogleBookmarksBrowder.getWebView().getEngine());
                        String temporary = null;
                        String label = null;
                        for (Node node: controller.getAllChildElements()) {
                            switch (node.getNodeName()) {
                            case "H3":
                            case "h3":
                                temporary = node.getTextContent();
                                break;
                            case "P":
                            case "p":
                                label = temporary;
                                temporary = null;
                                break;
                            case "A":
                            case "a":
                                String title = node.getTextContent();
                                String url = node.getAttributes().getNamedItem("href").getNodeValue();
                                RudeArray row = new RudeArray();
                                row.put("title", title);
                                row.put("url", url);
                                row.put("label", label);
                                editor.rudeArrayTable.addRow(row);
                                break;
                            }
                        }
                        editor.rudeArrayTable.loadMoreRows();
                        editor.updateLabelsList();
                    }
                });
            } catch (Exception exception) {
                InstantAlert.show(editor.paneRoot, ExceptionHelper.createDetailMessage(exception), Pos.CENTER, 3000);
            }
        }
    };
    
    /**
     * GoogleBookmarksに書き込み.
     */
    private EventHandler<ActionEvent> executeEvent = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            GoogleBookmarksBrowder.getStage().show();
            GoogleBookmarksEditor editor = GoogleBookmarksEditor.this;
            RudeArrayTable table = editor.rudeArrayTable;
            WebEngineFlow flow = new WebEngineFlow(GoogleBookmarksBrowder.getWebView());
            for (int index = table.getItems().size() - 1; index >= 0; index--) {
                RudeArray row = table.getItems().get(index);
                flow.addTaskLoadURL("https://www.google.co.jp/bookmarks/mark?op=edit&output=popup");
                flow.addTaskWaitForLoadElementById("bkmk_n");
                flow.addTask(new Task() {
                    @Override
                    public void execute(WebEngineController controller) throws Exception {
                        controller.selectElementById("bkmk_n");
                        Element titleElement = controller.getSelectedElement();
                        titleElement.setAttribute("value", row.getString("title"));
                        controller.clearSelectedElements();
                        controller.selectElementById("bkmk_u");
                        Element urlElement = controller.getSelectedElement();
                        urlElement.setAttribute("value", row.getString("url"));
                        controller.clearSelectedElements();
                        controller.selectElementById("bkmk_label_1");
                        Element labelElement = controller.getSelectedElement();
                        labelElement.setAttribute("value", row.getString("label"));
                        controller.getWebEngine().executeScript("document.add_bkmk_form.onsubmit(); document.add_bkmk_form.submit();");
                    }
                });
            }
            flow.addTask(new Task() {
                @Override
                public void execute(WebEngineController controller) throws Exception {
                    GoogleBookmarksBrowder.getStage().hide();
                }
            });
            flow.execute();
        }
    };
    
    @Override
    protected void importDataFromForm() {
    }
    
    @Override
    protected void beforeClosePrepare() throws Exception {
    }
    
}
