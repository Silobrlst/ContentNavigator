import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainWindow extends JFrame {
    private JList tagsList;
    private JList searchedList;
    private JTextField tagsFilter;
    private JTextField nameFilter;
    private JPanel rootPanel;

    private JMenu openRecentMenu;

    private JMenuItem saveItem;
    private JMenuItem saveAsItem;
    private JMenu menuEdit;

    private final String appName = "Content navigator";

    private File conteneInfoFile;
    private JSONObject tags;
    private JSONObject paths;

    private DefaultListModel tagsListModel;

    private AddRenameTagDialog addRenameTagDialog;
    private AddPathDialog addPathDialog;


    //<Base i/o>==============================
    private String loadDataFromfile(File fileIn) {
        try {
            fileIn.createNewFile();

            byte[] encoded = Files.readAllBytes(Paths.get(fileIn.getAbsolutePath()));
            return new String(encoded, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void saveDataToFile(File fileIn, String dataIn) {
        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(fileIn);
            bw = new BufferedWriter(fw);
            bw.write(dataIn);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private JSONObject loadJSON(File fileIn) {
        String data = loadDataFromfile(fileIn);

        if (data.isEmpty()) {
            data = "{}";
        }

        return new JSONObject(data);
    }

    private void saveJSON(File fileIn, JSONObject jsonObjectIn) {
        saveDataToFile(fileIn, jsonObjectIn.toString(5));
    }
    //</base i/o>=============================


    //<GUI settings i/o>======================
    private void validateGuiSettings(JSONObject jsonIn){
        if(!jsonIn.has("recent")){
            jsonIn.put("recent", new JSONArray());
        }
    }

    private void loadGuiSettings(){
        JSONObject guiJSON = loadJSON(new File("guiSettings.json"));
        validateGuiSettings(guiJSON);

        JSONArray recentJSON = guiJSON.getJSONArray("recent");

        for (int i=0; i<recentJSON.length(); i++){
            addRecent(new File(recentJSON.getString(i)));
        }
    }

    private void saveGuiSettings(){
        JSONObject guiJSON = new JSONObject();

        JSONArray recentJSON = new JSONArray();
        for (int i=0; i<openRecentMenu.getItemCount(); i++){
            recentJSON.put(openRecentMenu.getItem(i).getText());
        }

        guiJSON.put("recent", recentJSON);

        saveJSON(new File("guiSettings.json"), guiJSON);
    }
    //</GUI settings i/o>=====================


    private void addRecent(File fileIn){
        for(int i=0; i<openRecentMenu.getItemCount(); i++){
            if(openRecentMenu.getItem(i).getText().equals(fileIn.getAbsolutePath())){
                return;
            }
        }

        JMenuItem jMenuItem = new JMenuItem(fileIn.getAbsolutePath());
        jMenuItem.addActionListener(e -> {
            loadContentInfo(new File(jMenuItem.getText()));
        });

        openRecentMenu.add(jMenuItem);
        saveGuiSettings();
    }


    //<Content info i/o>======================
    private void validateContentInfo(JSONObject jsonIn) {
        if (!jsonIn.has("tags")) {
            jsonIn.put("tags", new JSONObject());
        }
    }

    private void loadContentInfo(File fileIn) {
        JSONObject json = loadJSON(fileIn);
        validateContentInfo(json);

        conteneInfoFile = fileIn;

        tags = json.getJSONObject("tags");

        //System.out.println(tags);

        for(String tag: tags.keySet()){
            tagsListModel.addElement(tag);
        }

        saveItem.setEnabled(true);
        saveAsItem.setEnabled(true);
        menuEdit.setEnabled(true);

        this.setTitle(appName + " [" + fileIn.getName() + "]");
    }

    private void saveContentInfo(File fileIn) {
        JSONObject json = new JSONObject();
        json.put("tags", tags);
        saveJSON(fileIn, json);
    }

    private void newContentInfo() {
        tags = new JSONObject();
    }
    //</Content info i/o>=====================


    //<editing>===============================
    private void addTag(String tagNameIn) {
        tags.put(tagNameIn, new JSONArray());
        tagsListModel.addElement(tagNameIn);
        saveContentInfo(conteneInfoFile);
    }

    private void renameTag(String oldTagNameIn, String newTagNameIn){
        tags.put(newTagNameIn, tags.get(oldTagNameIn));
        tags.remove(oldTagNameIn);

        for(int i=0; i<tagsListModel.size(); i++){
            if(tagsListModel.get(i).toString().equals(oldTagNameIn)){
                tagsListModel.setElementAt(oldTagNameIn, i);
                saveContentInfo(conteneInfoFile);
                return;
            }
        }
    }

    private void removeSelectedTag() {
        tags.remove(tagsListModel.get(tagsList.getSelectedIndex()).toString());
        tagsListModel.remove(tagsList.getSelectedIndex());
        saveContentInfo(conteneInfoFile);
    }

    private void addPathToTag(String tagNameIn, String pathIn) {
        tags.getJSONArray(tagNameIn).put(pathIn);
        saveContentInfo(conteneInfoFile);
    }

    private void removePathFromTag(String tagNameIn, String pathIn){
        JSONArray objects = tags.getJSONArray(tagNameIn);

        for(int i=0; i<objects.length(); i++){
            if(objects.getString(i).equals(pathIn)){
                objects.remove(i);
                saveContentInfo(conteneInfoFile);
                return;
            }
        }
    }
    //</editing>==============================


    private MainWindow() {
        this.setTitle(appName);
        this.getContentPane().add(rootPanel);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);


        tagsListModel = new DefaultListModel();
        tagsList.setModel(tagsListModel);

        conteneInfoFile = null;

        addRenameTagDialog = new AddRenameTagDialog(new AddChangeInterface() {
            @Override
            public void add(String newValueIn) {
                addTag(newValueIn);
            }

            @Override
            public void change(String oldValueIn, String newValueIn) {
                renameTag(oldValueIn, newValueIn);
            }
        });
        addPathDialog = new AddPathDialog();


        final JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("json", "json"));

        JMenuBar menuBar = new JMenuBar();

        //<menu file>=================================
        JMenu menuFile = new JMenu("File");
        menuBar.add(menuFile);

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> {
            if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
                newContentInfo();
                addRecent(fc.getSelectedFile());
            }
        });
        menuFile.add(newItem);

        menuFile.addSeparator();

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> {
            if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
                addRecent(fc.getSelectedFile());
                loadContentInfo(fc.getSelectedFile());
            }
        });
        menuFile.add(openItem);

        openRecentMenu = new JMenu("Open Recent");
        menuFile.add(openRecentMenu);

        menuFile.addSeparator();

        saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> {
            if(conteneInfoFile == null){
                if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
                    conteneInfoFile = fc.getSelectedFile();
                    saveContentInfo(conteneInfoFile);
                }
            }else{
                saveContentInfo(conteneInfoFile);
            }
        });
        menuFile.add(saveItem);

        saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> {
            if(fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
                conteneInfoFile = fc.getSelectedFile();
                saveContentInfo(conteneInfoFile);
                loadContentInfo(conteneInfoFile);
            }
        });
        menuFile.add(saveAsItem);
        //</menu file>================================

        //<menu edit>=================================
        menuEdit = new JMenu("Edit");
        menuBar.add(menuEdit);

        JMenuItem addPathItem = new JMenuItem("Add Path");
        addPathItem.addActionListener(e -> {
            addPathDialog.setVisible(true);
        });
        menuEdit.add(addPathItem);

        JMenuItem addTagItem = new JMenuItem("Add Tag");
        addTagItem.addActionListener(e -> {
            addRenameTagDialog.setAddTag();
            addRenameTagDialog.setVisible(true);
        });
        menuEdit.add(addTagItem);

        menuEdit.addSeparator();

        JMenuItem editPathItem = new JMenuItem("Edit Path");
        editPathItem.addActionListener(e -> {
            System.out.println("Edit Path");
        });
        menuEdit.add(editPathItem);

        JMenuItem renameTagItem = new JMenuItem("Rename Tag");
        renameTagItem.addActionListener(e -> {
            System.out.println("Rename Tag");
        });
        menuEdit.add(renameTagItem);

        menuEdit.addSeparator();

        JMenuItem removePathItem = new JMenuItem("Remove Path");
        removePathItem.addActionListener(e -> {
            System.out.println("Remove Path");
        });
        menuEdit.add(removePathItem);

        JMenuItem removeTagItem = new JMenuItem("Remove Tag");
        removeTagItem.addActionListener(e -> {
            removeSelectedTag();
        });
        menuEdit.add(removeTagItem);
        //</menu edit>================================

        loadGuiSettings();

        menuEdit.setEnabled(false);
        saveItem.setEnabled(false);
        saveAsItem.setEnabled(false);

        if(openRecentMenu.getItemCount() > 0){
            loadContentInfo(new File(openRecentMenu.getItem(0).getText()));
        }

        this.setJMenuBar(menuBar);

        //<style>=====================================
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(this);
                    SwingUtilities.updateComponentTreeUI(fc);
                    SwingUtilities.updateComponentTreeUI(addPathDialog);
                    addPathDialog.pack();
                    this.pack();
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //</style>====================================
    }

    public static void main(String[] args) {
        new MainWindow().setVisible(true);
    }
}
