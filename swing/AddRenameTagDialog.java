import javax.swing.*;
import java.awt.event.*;

public class AddRenameTagDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField tag;

    private AddChangeInterface addChangeInterface;
    private boolean renaming;
    private String oldTagName;

    public AddRenameTagDialog(AddChangeInterface addChangeInterfaceIn) {
        this.setTitle("Add Tag");
        renaming = false;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        addChangeInterface = addChangeInterfaceIn;

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

        this.pack();
    }

    public void setAddTag(){
        this.setTitle("Add Tag");
        renaming = false;
        tag.setText("");
    }

    public void setRenameTag(String tagNameIn){
        this.setTitle("Rename Tag");
        renaming = true;
        tag.setText(tagNameIn);
    }

    private void onOK() {
        if(renaming){
            addChangeInterface.change(oldTagName, tag.getText());
        }else{
            addChangeInterface.add(tag.getText());
        }

        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
