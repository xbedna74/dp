package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import jade.lang.acl.MessageTemplate;
import jade.wrapper.ControllerException;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent managing one concrete sensor, accepting subscriptions
 * to new data. Is listening for mqtt publishes regarding its sensor.
 */
public class SensorAgent extends Agent{

    /**
     * List for all subscribed agents.
     */
    private List<AID> subs = new ArrayList<>();
    //private ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
    //private String topic;
    //private String id;

    /**
     * Initializes agent and adds behaviour for processing messages.
     */
    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd  = new ServiceDescription();
        sd.setName(getAID().getLocalName());
        sd.setType("sensor");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd );
        }
        catch (FIPAException fe) { fe.printStackTrace(); }

        //id = getAID().getLocalName().substring(7);
        //topic = id + "/get";


        //addBehaviour(tbf.wrap(b));
        //adds behaviour for processing messages
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt =
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE));
                ACLMessage msg = receive(mt);

                if (msg == null) {
                    block();
                }
                else if (msg.getPerformative() == ACLMessage.INFORM) {
                    ACLMessage prop = new ACLMessage(ACLMessage.INFORM);
                    for (AID s : subs) {
                        prop.addReceiver(s);
                    }
                    prop.setContent(msg.getContent());
                    send(prop);
                }
                else if (msg.getPerformative() == ACLMessage.SUBSCRIBE){
                    AID sender = msg.getSender();
                    if (!subs.contains(sender)) {
                        subs.add(sender);
                        System.out.println("Sender " + sender + " subscribed.");
                    }
                    else {
                        System.out.println("Sender " + sender + " already subscribed.");
                    }
                }
            }
        });
    }
}