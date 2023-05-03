package agents;

import jade.gui.GuiEvent;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class implementing the gui for InterfaceAgent.
 */
public class InterfaceAgentGui extends JFrame implements ActionListener {

    /**
     * InterfaceAgent to which this gui belongs to
     */
    private InterfaceAgent myAgent;

    /**
     * Main panel for gui
     */
    private JPanel main;

    /**
     * Combobox with which users can be selected
     */
    private JComboBox userBox;
    /**
     * Map of spinners for locations
     */
    private Map<String, JSpinner> setters = new HashMap<>();
    /**
     * Map of checkboxes for automatic mode for locations
     */
    private Map<String, JCheckBox> automaticCheckBoxes = new HashMap<>();
    /**
     * Map of checkboxes for override mode for locations
     */
    private Map<String, JCheckBox> overrideCheckBoxes = new HashMap<>();
    /**
     * Map of labels for output values for locations
     */
    private Map<String, JLabel> outs = new HashMap<>();
    /**
     * Nested map for selected values for locations for each user
     */
    private Map<String, Map<String, Float>> inValues = new HashMap<>(); //input values from user/system
    /**
     * Map for sensor values for locaitons
     */
    private Map<String, String> outValues = new HashMap<>(); //actual values from sensors

    /**
     * Constructor for GUI
     *
     * @param a InterfaceAgent to which the gui belongs
     * @param s List of sensor names
     * @param u List of user names
     */
    public InterfaceAgentGui(InterfaceAgent a, List<String> s, List<String> u) {
        super();
        myAgent = a;

        System.out.println(s);
        System.out.println(u);

        setTitle("GUI of " + a.getLocalName());
        setSize(400,200);

        //main panel
        main = new JPanel();
        main.setLayout(new BoxLayout(main,BoxLayout.Y_AXIS));

        /*JPanel sen_act = new JPanel();
        sen_act.setLayout(new BoxLayout(sen_act, BoxLayout.X_AXIS));

        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JPanel setters = new JPanel();
        setters.setLayout(new BoxLayout(setters, BoxLayout.Y_AXIS));*/

        JPanel users = new JPanel();
        users.setLayout(new BoxLayout(users, BoxLayout.X_AXIS));

        JLabel userLabel = new JLabel("User: ");
        userBox = new JComboBox(u.toArray()); //user combobox
        userBox.setName("user");
        userBox.addActionListener(this);

        users.add(userLabel);
        users.add(userBox);

        main.add(users);

        //creating panels for each location settings
        for (String x : s) {
            JPanel sensor = new JPanel();
            sensor.setLayout(new BoxLayout(sensor, BoxLayout.X_AXIS));

            JLabel labelIn = new JLabel("Input " + x + ": " );
            SpinnerModel sModel = new SpinnerNumberModel(21, 15, 25, 0.5);
            JSpinner spinner = new JSpinner(sModel);
            spinner.setName(x);

            JPanel checkBoxes = new JPanel();
            checkBoxes.setLayout(new BoxLayout(checkBoxes, BoxLayout.Y_AXIS));
            JCheckBox autoCheck = new JCheckBox("Automatic");
            autoCheck.setName(x);
            JCheckBox overrideCheck = new JCheckBox("Override");
            autoCheck.setName(x);

            automaticCheckBoxes.put(x, autoCheck);
            overrideCheckBoxes.put(x, overrideCheck);
            checkBoxes.add(autoCheck);
            checkBoxes.add(overrideCheck);

            setters.put(x, spinner); //setters map, key is identifier of actor/sensor and value is gui element

            sensor.add(labelIn);
            sensor.add(spinner);
            sensor.add(checkBoxes);

            JPanel out = new JPanel();
            out.setLayout(new BoxLayout(out, BoxLayout.X_AXIS));

            JLabel labelOut = new JLabel("Current " + x + ": " );
            JLabel output = new JLabel("-");

            outs.put(x, output);

            out.add(labelOut);
            out.add(output);

            main.add(sensor);
            main.add(out);
        }

        //cerating buttons
        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.X_AXIS));

        JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(this);

        btns.add(confirmButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        btns.add(cancelButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(this);

        btns.add(exitButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(this);

        btns.add(refreshButton);

        main.add(btns);

        getContentPane().add(main, BorderLayout.CENTER);
    }

    /**
     * Action listener processing events from gui
     *
     * @param e event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        JComponent c = (JComponent) e.getSource();

        //System.out.println(userBox.getSelectedItem());

        if (command.equalsIgnoreCase("Confirm")) { //confirm button
            GuiEvent ev = new GuiEvent(null, InterfaceAgent.POST);
            //String msg = "0";
            Map<String, Float> vals = new HashMap<>();
            JSONObject data = new JSONObject();
            data.put("_type", "gui");
            for (String id : setters.keySet()) {
                JSONObject element = new JSONObject();
                if (automaticCheckBoxes.get(id).isSelected() && !overrideCheckBoxes.get(id).isSelected()) {
                    //msg += id + ":" + "automatic" + ",";
                    element.put("additional", "automatic");
                    data.put(id, element);
                    continue;
                }

                vals.put(id, Float.parseFloat(String.valueOf(setters.get(id).getValue())));

                if (overrideCheckBoxes.get(id).isSelected()) {
                    //msg += id + ":" + "override" + ":" + setters.get(id).getValue() + ",";
                    element.put("additional", "override");
                    element.put("value", String.valueOf(setters.get(id).getValue()));
                }
                else {
                    //msg += id + ":" + setters.get(id).getValue() + ",";
                    element.put("value", String.valueOf(setters.get(id).getValue()));
                }
                data.put(id, element);
            }
            inValues.put((String) userBox.getSelectedItem(), vals);
            //msg = msg.substring(0, msg.length() - 1);
            ev.addParameter(data.toJSONString());
            //ev.addParameter(msg);
            ev.addParameter(userBox.getSelectedItem());
            myAgent.postGuiEvent(ev);
        }
        else if (command.equalsIgnoreCase("Cancel")) { //cancel button
            inValues.remove((String) userBox.getSelectedItem());
            GuiEvent ev = new GuiEvent(null, InterfaceAgent.CANCEL);
            ev.addParameter(userBox.getSelectedItem());
            //JSONObject data = new JSONObject();
            //data.put("user", userBox.getSelectedItem());
            //ev.addParameter(data.toJSONString());
            myAgent.postGuiEvent(ev);
        }
        else if (command.equalsIgnoreCase("Exit")) { //exit button
            GuiEvent ev = new GuiEvent(null, InterfaceAgent.EXIT);
            myAgent.postGuiEvent(ev);
        }
        else if (command.equalsIgnoreCase("Refresh") || c.getName().equals("user")) { //refresh button
            displayInValues();
        }
        else {
            System.out.println(command);
        }
    }

    /**
     * Displays inputted values for currently selected user
     */
    private void displayInValues() {
        String user = (String) userBox.getSelectedItem();
        if (inValues.get(user) != null) {
            Map<String, Float> userValues = inValues.get(user);
            System.out.println(userValues);
            for (String s : userValues.keySet()) {
                setters.get(s).setValue(userValues.get(s));
            }
        }
    }

    /**
     * Sets input value for user for location.
     *
     * @param user user
     * @param id location identifier
     * @param value input value
     */
    public void setInValue(String user, String id, String value) {
        Map<String, Float> userValues = inValues.get(user);
        userValues.put(id, Float.valueOf(value));
        inValues.put(user, userValues);
        if (userBox.getSelectedItem() == user) {
            displayInValues();
        }
    }

    /**
     * Sets output value for location
     *
     * @param id location identifier
     * @param v value
     */
    public void setOutValue(String id, String v) {
        outValues.put(id, v);
        outs.get(id).setText(v);
    }
}
