package agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agent controlling gui.
 */
public class InterfaceAgent extends GuiAgent {

    /**
     * code for EXIT event
     */
    public static final int EXIT = 1000;
    /**
     * code for POST event
     */
    public static final int POST = 1001;
    /**
     * code for CANCEL event
     */
    public static final int CANCEL = 1002;

    /**
     * gui
     */
    transient protected InterfaceAgentGui gui;

    //private boolean automatic = false;

    /**
     * Initializes agent, creates gui and adds behaviour for message processing.
     */
    public void setup() {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(-1));

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd  = new ServiceDescription();
        sd.setType("sensor");
        dfd.addServices(sd);

        DFAgentDescription[] sResult = new DFAgentDescription[0];
        try {
            sResult = DFService.search(this, dfd, sc);
        } catch (FIPAException fe) { fe.printStackTrace(); }

        List<String> sensors = new ArrayList<>();

        for (DFAgentDescription x : sResult) {
            String sName = x.getName().getName();
            ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
            subscribe.addReceiver(new AID(sName));
            send(subscribe);

            sensors.add(x.getName().getLocalName().substring(7)); //7 is the index at which sensor identifier begins "_sensor"...
        }

        dfd = new DFAgentDescription();
        sd = new ServiceDescription();
        sd.setType("user");
        dfd.addServices(sd);

        DFAgentDescription[] uResult = new DFAgentDescription[0];
        try {
            uResult = DFService.search(this, dfd, sc);
        } catch (FIPAException fe) { fe.printStackTrace(); }

        List<String> users = new ArrayList<>();

        for (DFAgentDescription x : uResult) {
            users.add(x.getName().getLocalName());
        }

        //creates gui
        gui = new InterfaceAgentGui(this, sensors, users);
        gui.setVisible(true);

        Behaviour b1 = new SimpleBehaviour() {
            @Override
            public void action() {
                ACLMessage msg;
                MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                        MessageTemplate.MatchPerformative(ACLMessage.AGREE));

                msg = receive(mt);
                if (msg == null) {
                    block();
                    return;
                }
                else if (msg.getPerformative() == ACLMessage.INFORM) {
                    String sender = msg.getSender().getLocalName();
                    if (sender.startsWith("_sensor")) { //inform about output values
                        JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                        String v = (String) o.get("value");
                        gui.setOutValue(sender.substring(7), v);
                    }
                    else { //inform about automatic value for input
                        JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                        String a = (String) o.get("actor");
                        String v = (String) o.get("value");

                        gui.setInValue(sender, a.substring(6), v);
                    }
                }
                else if (msg.getPerformative() == ACLMessage.AGREE) {
                    ;
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        };

        addBehaviour(b1);
    }

    /**
     * Method frees up resources in case of agent deletion.
     */
    public void takeDown() {
        if (gui!=null) {
            gui.dispose();
            gui.setVisible(false);
        }
        System.out.println(getLocalName()+" is now shutting down.");
    }

    /**
     * Method processes events from gui
     *
     * @param event event
     */
    @Override
    protected void onGuiEvent(GuiEvent event) {
        if (event.getType() == POST) {
            String val = (String) event.getParameter(0);
            String receiver = (String) event.getParameter(1);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            msg.setContent(val);
            send(msg);
        }
        else if (event.getType() == EXIT) {
            gui.dispose();
            gui = null;
            doDelete();
        }
        else if (event.getType() == CANCEL) {
            String receiver = (String) event.getParameter(0);
            ACLMessage msg = new ACLMessage(ACLMessage.CANCEL);
            msg.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            send(msg);
        }
    }
}
