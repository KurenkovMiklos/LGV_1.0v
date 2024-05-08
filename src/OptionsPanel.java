import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class OptionsPanel extends JPanel {
    HashMap<String, String> state = new HashMap<String, String>();
    public OptionsPanel(VertexPlacementAlgorithm VPA, MainPanel mainPanel){
        JButton btn = new JButton("Regenerate");
        //Change button text on click
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                List<String> ArgList = new ArrayList<String>();
                for( String key : state.keySet()) {
                    ArgList.add(key + "," + state.get(key));
                }
                VPA.SetAguments(ArgList);
                mainPanel.UpdateImage(VPA);
            }
        });
        this.add(btn);


        List<String> options = VPA.GetAguments();
        if (options == null) {return;}
        for( String option : options ) {
            String[] params = option.split(",");
            switch (params[0]) {
                case "enum":
                    state.put(params[1], params[2]);
                    JComboBox<String> ComboBox = new JComboBox<>();
                    for(int i = 2; i < params.length; i++){
                        ComboBox.addItem(params[i]);
                    }
                    ComboBox.addItemListener(new ItemChangeListener(params[1]));
                    this.add(ComboBox);
                    break;
                case "double":
                    state.put(params[1], "1");
                    JLabel l = new JLabel(params[2]);
                    this.add(l);
                    JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1e-16, 1000, 0.1));
                    l.setLabelFor(spinner);
                    this.add(spinner);
                    spinner.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            state.replace(params[1], spinner.getValue().toString());
                            //System.out.println(state.get(params[1]));
                        }
                    });
                    break;
                case "integer":
                    state.put(params[1], "100");
                    JLabel lDouble = new JLabel(params[2]);
                    this.add(lDouble);
                    JSpinner spinnerDouble = new JSpinner(new SpinnerNumberModel(100, 1, 10000000, 1));
                    lDouble.setLabelFor(spinnerDouble);
                    this.add(spinnerDouble);
                    spinnerDouble.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            state.replace(params[1], spinnerDouble.getValue().toString());
                            //System.out.println(state.get(params[1]));
                        }
                    });
                    break;
                case "boolean":
                    state.put(params[1], "false");
                    JCheckBox CheckBox = new JCheckBox(params[2]);
                    this.add(CheckBox);
                    CheckBox.addItemListener(new ItemListener() {
                        @Override
                        public void itemStateChanged(ItemEvent e) {
                            state.replace(params[1], e.getStateChange() == ItemEvent.SELECTED ? "true" : "false");
                            //System.out.println(state.get(params[1]));
                        }
                    });
                    break;
                default:
            }
        }


    }

    class ItemChangeListener implements ItemListener {
        String param;
        public ItemChangeListener(String name){ this.param = name;}

        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                Object item = event.getItem();
                state.replace(param, (String) item);
                //System.out.println(state.get(param));
            }
        }
    }



}




