import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.io.File;

public class AddPathDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField path;
    private JList availableTags;
    private JList selectedTags;
    private JButton selectTagsButton;
    private JButton unselectTagsButton;
    private JButton exploreButton;
    private JButton addTagButton;
    private JLabel pathValidation;

    public AddPathDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        exploreButton.addActionListener(e -> {
            SwingUtilities.updateComponentTreeUI(fc);
            if(fc.showDialog(this, "choose") == JFileChooser.APPROVE_OPTION){
                path.setText(fc.getSelectedFile().getAbsolutePath());
                fc.setSelectedFile(fc.getSelectedFile());
            }
        });

        path.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }
            public void removeUpdate(DocumentEvent e) {
                warn();
            }
            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                pathValidation.setText("");
            }
        });

        this.pack();
    }

    private void onOK() {
        if(new File(path.getText()).exists()){
            dispose();
        }else{
            pathValidation.setText("Path does not exists");
            this.pack();
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
