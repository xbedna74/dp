package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Agent implementing the communication between agents and MQTT server.
 */
public class CommunicationAgent extends Agent {

    /**
     * Client for connecting to MQTT server.
     */
    private MqttClient client;
    //private AID gui;

    /**
     * This method initializes the agent, connects MQTT client to server,
     * and adds cyclic behaviour for message processing to itself.
     */
    protected void setup() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //gui = new AID("_interface", AID.ISLOCALNAME);

        String broker = "ssl://1113004078a64e9ca046977925d355ee.s2.eu.hivemq.cloud:8883";
        String clientId = "mqtt_test";
        try {
            client = new MqttClient(broker, clientId);

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName("user1");
            connOpts.setPassword("user12345".toCharArray());

            connOpts.setCleanSession(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println("Connection lost");
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    //Pattern get = Pattern.compile("([0-9]|[a-z]|[A-Z])+/get");
                    //Pattern proximity = Pattern.compile("([0-9]|[a-z]|[A-Z])+/proximity/([0-9]|[a-z]|[A-Z])+");
                    if (s.matches("^([0-9]|[a-z]|[A-Z])+/get$")) { //matches topic on which environment states are published
                        String msg = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        AID sensor = new AID("_sensor" + s.split("/")[0], AID.ISLOCALNAME);
                        message.addReceiver(sensor);

                        JSONObject obj = new JSONObject();
                        obj.put("value", msg);
                        message.setContent(obj.toString());
                        send(message);
                    }
                    else if (s.matches("^([0-9]|[a-z]|[A-Z])+/proximity/([0-9]|[a-z]|[A-Z])+$")) { //matches topic for user proximity publishes
                        String msg = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        String user = s.split("/")[2];
                        AID recUser = new AID(user, AID.ISLOCALNAME);
                        message.addReceiver(recUser);

                        JSONObject obj = new JSONObject();
                        obj.put("proximity", msg);
                        obj.put("location", s.split("/")[0]);
                        message.setContent(obj.toString());
                        send(message);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);

            System.out.println("Connected");
        } catch (Exception e)
        {
            System.out.println(e);
            System.out.println("Could not connect to MQTT server.");
            doDelete();
            return;
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


        //subscribes to topics in all locations
        for (DFAgentDescription x : sResult) {
            String id = x.getName().getLocalName().substring(7);
            try {
                client.subscribe(id + "/get");
                client.subscribe(id + "/proximity/+");
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }

        /*dfd = new DFAgentDescription();
        sd = new ServiceDescription();
        sd.setType("user");
        dfd.addServices(sd);

        DFAgentDescription[] uResult = new DFAgentDescription[0];
        try {
            uResult = DFService.search(this, dfd, sc);
        } catch (FIPAException fe) { fe.printStackTrace(); }*/

        /*for (DFAgentDescription x : uResult) {
            String id = x.getName().getLocalName();
            try {
                client.subscribe(id + "/proximity");
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }*/


        //adds behaviour for message processing
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg == null) {
                    block();
                }
                else if (msg.getPerformative() == ACLMessage.REQUEST) {
                    String id = msg.getSender().getLocalName().substring(6);
                    JSONObject obj = (JSONObject) JSONValue.parse(msg.getContent());

                    try {
                        client.publish(id + "/set", ((String) obj.get("value")).getBytes(StandardCharsets.UTF_8), 0, false);
                    } catch (Exception e) {
                        System.out.print(e);
                    }
                }
            }
        });
    }
}
