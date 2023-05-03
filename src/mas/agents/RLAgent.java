package agents;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import qlearning.QLearningTemperature;

/**
 * Agent responsible for Q-Learning for a certain location for one user.
 */
public class RLAgent extends Agent {
    /**
     * User
     */
    private String user;
    /**
     * Actor
     */
    private String actor;
    /**
     * Instance of Q-Learning algorithm (QLearningTemperature)
     */
    private QLearningTemperature ql;

    /**
     * Initializes agent and Q-Learning class, adds behaviour for processing messages.
     */
    public void setup() {
        String name = getAID().getLocalName();
        user = name.split("-")[0];
        actor = name.split("-")[1];

        ql = new QLearningTemperature();

        addBehaviour(new SimpleBehaviour() {
            @Override
            public void action() {
                ACLMessage msg;
                msg = receive();

                if (msg == null) {
                    block();
                }
                else if (msg.getPerformative() == ACLMessage.QUERY_REF) { //querying for action
                    //String[] messageContent = msg.getContent().split(",");
                    if (msg.getSender().getLocalName().equals(user)) {
                        JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());

                        //int day = (int) (long) o.get("day");
                        int interval = (int) (long) o.get("interval");
                        ACLMessage reply = msg.createReply(ACLMessage.INFORM_REF);

                        JSONObject query_ref = new JSONObject();
                        query_ref.put("value", String.valueOf(ql.chooseTemperature(interval)));
                        reply.setContent(query_ref.toJSONString());
                        send(reply);
                    }
                }
                else if (msg.getPerformative() == ACLMessage.INFORM) { //informing to update q-table
                    //String[] messageContent = msg.getContent().split(",");
                    JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                    //int day = (int) (long) o.get("day");
                    int interval = (int) (long) o.get("interval");
                    float temperature = Float.parseFloat((String) o.get("value"));
                    float reward = Float.parseFloat((String) o.get("reward"));
                    ql.updateQTable(interval, ql.getActionIndex(temperature), reward);
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        });
    }
}
