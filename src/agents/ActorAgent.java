package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.nio.charset.StandardCharsets;

/**
 * Agent managing one concrete actor, accepting requests from agents
 * to set actors state. Can serve only one request at a time, however
 * it can be a group request after agents formed a group.
 */
public class ActorAgent extends Agent {

    /**
     * Variable indicating currently served agent.
     */
    private AID beingServed = null;
    //private String topic;
    //private String id;

    /**
     * Adds behaviour that processes incoming messages.
     */
    protected void setup() {
        //id = getAID().getLocalName().substring(6);
        //topic = id + "/set";

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt =
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
                ACLMessage msg = blockingReceive(mt);

                if (msg.getPerformative() == ACLMessage.REQUEST) {
                    if (beingServed == null) { //serving new user
                        String value = msg.getContent();
                        beingServed = msg.getSender();
                        ACLMessage mqtt_com = new ACLMessage(ACLMessage.REQUEST);
                        mqtt_com.addReceiver(new AID("_comm", AID.ISLOCALNAME));
                        mqtt_com.setContent(value);
                        send(mqtt_com);
                        ACLMessage reply = msg.createReply(ACLMessage.AGREE);
                        JSONObject o = new JSONObject();
                        o.put("_type", "actor");
                        reply.setContent(o.toJSONString());
                        send(reply);
                    }
                    else if (beingServed.equals(msg.getSender())) { //serving currently served user with new request
                        //JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                        //String value = (String) o.get("value");
                        String value = msg.getContent();
                        ACLMessage mqtt_com = new ACLMessage(ACLMessage.REQUEST);
                        mqtt_com.addReceiver(new AID("_comm", AID.ISLOCALNAME));
                        mqtt_com.setContent(value);
                        send(mqtt_com);
                        ACLMessage reply = msg.createReply(ACLMessage.AGREE);
                        JSONObject o = new JSONObject();
                        o.put("_type", "actor");
                        reply.setContent(o.toJSONString());
                        send(reply);
                    }
                    else {  //somebody is already being served
                        ACLMessage reply = msg.createReply(ACLMessage.REFUSE);
                        JSONObject o = new JSONObject();
                        o.put("user", beingServed.getName());
                        reply.setContent(o.toJSONString());
                        send(reply);
                    }
                }
                else if (msg.getPerformative() == ACLMessage.CANCEL) {
                    //String value = msg.getContent();
                    AID sender = msg.getSender();
                    if (sender.equals(beingServed)) { //served user is canceling
                        beingServed = null;
                        ACLMessage mqtt_com = new ACLMessage(ACLMessage.REQUEST);
                        mqtt_com.addReceiver(new AID("_comm", AID.ISLOCALNAME));
                        JSONObject o = new JSONObject();
                        o.put("value", "off");
                        mqtt_com.setContent(o.toJSONString());
                        send(mqtt_com);
                    }
                }
                block();
            }
        });
    }
}
