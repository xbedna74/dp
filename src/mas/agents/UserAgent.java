package agents;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.core.ContainerID;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Agent managing control of actors,
 * communicating and cooperating with other UserAgent on actor control.
 */
public class UserAgent extends Agent {

    /**
     * Name of agent
     */
    private String name;

    //actor is understood as the whole local name of actor agent
    /**
     * Map with leaders for each actor
     */
    private Map<String, AID> leader = new HashMap<>();
    /**
     * Nested map with groups for each actor
     */
    private Map<String, Map<AID, Float>> coalition = new HashMap<>();
    /**
     * Map indicating whether group exists for each actor
     */
    private Map<String, Boolean> isCoalition = new HashMap<>();
    //private boolean isActive = false;
    /**
     * Map with values for each actor for user
     */
    private Map<String, Float> values = new HashMap<>();
    /**
     * Map for pending requests for control of actor
     */
    private Map<String, String> pendingRequests = new HashMap<>();

    /**
     * Set of actors which are in location in which user is present
     */
    private Set<String> present = new HashSet<>();
    /**
     * Set of actors that are in automatic mode
     */
    private Set<String> automatic = new HashSet<>();
    /**
     * Set of actors that are in override mode
     */
    private Set<String> override = new HashSet<>();
    /**
     * Interval of a day
     */
    private int interval;
    //private int day;
    /**
     * Length of one interval
     */
    private final int period = 900;

    /**
     * Sets values for actors from JSON object
     *
     * @param o JSONObject
     */
    private void setValues(JSONObject o) {
        //String[] l = s.replaceAll("\\s", "").split(",");
        List<String> automaticTemp = new ArrayList<>(automatic);
        automatic.clear();
        override.clear();
        for (Object actor : o.keySet()) {
            String key = (String) actor;
            String a = "_actor" + key;

            JSONObject element = (JSONObject) o.get(key);
            String v = (String) element.get("value");

            if (Objects.equals((String) element.get("additional"), "override")) { //actor is in override
                override.add(a);
                ACLMessage queryRL = new ACLMessage(ACLMessage.QUERY_REF);
                queryRL.addReceiver(new AID(name + "-" + a, AID.ISLOCALNAME));

                int interval = LocalTime.now().toSecondOfDay() / 1800;
                //int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;

                JSONObject query = new JSONObject();
                //query.put("day", dayIndex);
                query.put("interval", interval);

                queryRL.setContent(query.toJSONString());

                //queryRL.setContent(dayIndex + "," + interval);
                send(queryRL); //query to RLAgent
                values.put(a, Float.valueOf(v));
            }
            else if (Objects.equals((String) element.get("additional"), "automatic")) { //actor is automatic
                override.remove(a);
                ACLMessage queryRL = new ACLMessage(ACLMessage.QUERY_REF);
                queryRL.addReceiver(new AID(name + "-" + a, AID.ISLOCALNAME));

                int interval = LocalTime.now().toSecondOfDay() / 1800;
                //int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;

                JSONObject query = new JSONObject();
                //query.put("day", dayIndex);
                query.put("interval", interval);

                queryRL.setContent(query.toJSONString());

                //queryRL.setContent(dayIndex + "," + interval);
                send(queryRL); //query to RLAgent
                automatic.add(a);
            }
            else { //sets value
                values.put(a, Float.valueOf(v));
            }

        }
        /*for (String x : l) {
            String a = "_actor" + x.split(":")[0];

            if (x.split(":").length == 3) {
                if (x.split(":")[1].equals("override")) {
                    String v = (x.split(":")[2]);
                    override.add(a);
                    values.put(a, Float.valueOf(v));
                    if (override.contains(a)) {
                        values.put(a, Float.valueOf(v));
                    }
                    else {
                        //automatic.remove(a);
                        override.add(a);
                        values.put(a, Float.valueOf(v));
                    }
                    continue;
                }
            }

            String v = (x.split(":")[1]);
            setValue(a, v);
        }*/
    }

    /*private void setValue(String a, String v) {
        override.remove(a);
        if (v.equals("automatic")) {
            ACLMessage queryRL = new ACLMessage(ACLMessage.QUERY_REF);
            queryRL.addReceiver(new AID(name + "-" + a, AID.ISLOCALNAME));

            int interval = LocalTime.now().toSecondOfDay() / 1800;
            int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;

            queryRL.setContent(dayIndex + "," + interval);
            send(queryRL);
            automatic.add(a);
        }
        else {
            values.put(a, Float.valueOf(v));
        }
    }*/

    /**
     * Sends request to actor according to group values.
     *
     * @param actor actor
     */
    private void coalitionToActor(String actor) {
        Float sum = values.get(actor);
        int i = 1;
        System.out.println(actor);
        System.out.println("Coalition to actor");
        System.out.println(coalition);
        /*for (Map<AID, Float> u: coalition.values()) {
            System.out.println(u);
            for (Float v : u.values()) {
                i += 1;
                sum += v;
            }
        }*/
        for (Float v : coalition.get(actor).values()) {
            i += 1;
            sum += v;
        }
        sum /= i;
        System.out.println(sum + ":" + i);
        ACLMessage actorReq = new ACLMessage(ACLMessage.REQUEST);
        actorReq.addReceiver(new AID(actor, AID.ISLOCALNAME));
        JSONObject o = new JSONObject();
        o.put("value", String.valueOf(sum));
        actorReq.setContent(o.toJSONString());
        send(actorReq);
    }

    /**
     * Requests control for all actors
     */
    private void requestActors() {
        for (String act : values.keySet()) {
            if (present.contains(act)) {requestActor(act);}
        }
    }

    /**
     * Requests control for one actor
     *
     * @param act actor
     */
    private void requestActor(String act) {
        if (!values.containsKey(act)) {return;} //if values is not set, do nothing
        if (isCoalition.get(act) == null || !isCoalition.get(act)) { //send request to actor if not in group
            //leader.put(act, getAID());
            ACLMessage actorReq = new ACLMessage(ACLMessage.REQUEST);
            actorReq.addReceiver(new AID(act, AID.ISLOCALNAME));
            JSONObject o = new JSONObject();
            o.put("value", String.valueOf(values.get(act)));
            actorReq.setContent(o.toJSONString());

            if (!Objects.equals(leader.get(act), new AID(getAID().getName()))) { //if not leader, mark the conversation so that agree message can be recognised
                String id = String.valueOf(System.currentTimeMillis()) + Math.floor(Math.random() * (1000 - 100 + 1) + 100);
                pendingRequests.put(id, act);
                actorReq.setConversationId(id);
            }

            send(actorReq);
        }
        else { //if group exists
            AID lead = leader.get(act);
            if (lead.equals(new AID(getAID().getName()))) { //is leader of coalition
                coalitionToActor(act);
            }
            else { //coalition with different leader
                ACLMessage actorReq = new ACLMessage(ACLMessage.REQUEST);
                actorReq.addReceiver(lead);
                JSONObject o = new JSONObject();
                o.put("_type", "user");
                o.put("actor", act);
                o.put("value", String.valueOf(values.get(act)));

                actorReq.setContent(o.toJSONString());
                //actorReq.setContent("1" + act + values.get(act)); //"1" because its request from user to user
                String id = String.valueOf(System.currentTimeMillis()) + Math.floor(Math.random() * (1000 - 100 + 1) + 100);
                pendingRequests.put(id, act);
                actorReq.setConversationId(id);
                send(actorReq);
            }
        }
    }

    /**
     * Cancel control from all actors.
     *
     * @param deletion flag whether values should be deleted or not
     */
    private void cancelActors(boolean deletion) {
        for (String act : values.keySet()) {
            cancelActor(deletion, act);
        }
    }

    /**
     * Cancel control for actor.
     *
     * @param deletion flag whether value should be deleted or not
     * @param act actor
     */
    private void cancelActor(boolean deletion, String act) {
        if (!values.containsKey(act)) {return;} //if value is not set, do nothing
        if (!isCoalition.containsKey(act) || !isCoalition.get(act)) { //if group does not exist
            System.out.println("CANCEL without coalition");
            leader.remove(act);
            if (deletion) {
                values.remove(act);
                automatic.remove(act);
                override.remove(act);
            }
            ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
            cancel.addReceiver(new AID(act, AID.ISLOCALNAME));
            send(cancel);
        }
        else { //group exists
            if (Objects.equals(leader.get(act), new AID(getAID().getName()))) { //is leader of group, cancels group
                System.out.println("CANCEL with coalition -> dismantling");
                System.out.println(leader.get(act));
                System.out.println(getAID());
                System.out.println();
                ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                cancel.addReceiver(new AID(act, AID.ISLOCALNAME));
                send(cancel);

                Map<AID, Float> c = coalition.get(act);
                ACLMessage dismantle = new ACLMessage(ACLMessage.INFORM);
                for (AID ag : c.keySet()) {
                    dismantle.addReceiver(ag);
                }
                JSONObject o = new JSONObject();
                o.put("actor", act);
                o.put("information", "coalition cancel");
                dismantle.setContent(o.toJSONString());
                send(dismantle);
            }
            else { //is not leader, just leaves the group
                System.out.println("CANCEL with coalition -> leaving coalition");
                ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                cancel.addReceiver(leader.get(act));
                JSONObject o = new JSONObject();
                o.put("actor", act);
                cancel.setContent(o.toJSONString());
                send(cancel);
            }

            if (deletion) {
                values.remove(act);
                automatic.remove(act);
                override.remove(act);
            }
            coalition.remove(act);
            isCoalition.remove(act);
            leader.remove(act);
        }
    }

    /**
     * Initializes agent, creates RLAgents and adds behaviour for message processing.
     */
    public void setup() {

        name = getAID().getLocalName();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd  = new ServiceDescription();
        sd.setName(name);
        sd.setType("user");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd );
        }
        catch (FIPAException fe) { fe.printStackTrace(); }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(-1));

        dfd = new DFAgentDescription();
        sd  = new ServiceDescription();
        sd.setType("sensor");
        dfd.addServices(sd);

        DFAgentDescription[] sResult = new DFAgentDescription[0];
        try {
            sResult = DFService.search(this, dfd, sc);
        } catch (FIPAException fe) { fe.printStackTrace(); }

        LocalDate currentDate = LocalDate.now();
        //day = currentDate.getDayOfWeek().getValue() - 1;

        LocalTime currentTime = LocalTime.now();
        interval = currentTime.toSecondOfDay() / period;


        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
        getContentManager().registerOntology(JADEManagementOntology.getInstance());
        //creates RLAgents for each actor
        for (DFAgentDescription s : sResult) {
            String id = s.getName().getLocalName().substring(7);
            this.getContainerController();

            CreateAgent ca = new CreateAgent();
            ca.setAgentName(name + "-" + "_actor" + id);
            ca.setClassName("agents.RLAgent");
            ca.setContainer(new ContainerID("Main-Container", null));
            Action actExpr = new Action(getAMS(), ca);

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(getAMS());
            request.setOntology(JADEManagementOntology.getInstance().getName());
            request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
            request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
            try {
                getContentManager().fillContent(request, actExpr);
                addBehaviour(new AchieveREInitiator(this, request) {
                    protected void handleInform(ACLMessage inform) {
                        System.out.println("Agent successfully created");
                    }
                    protected void handleFailure(ACLMessage failure) {
                        System.out.println("Error creating agent.");
                    }
                } );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }


        //userActorAgents name will consist of two parts separated by "-"
        //first part will be users name and second name of actor ("_actor" + identifier)
        //actor = new AID("_actor" + (getAID().getLocalName().split("-")[1]), AID.ISLOCALNAME);
        Behaviour b1 = new SimpleBehaviour() {
            @Override
            public void action() {
                LocalDate currentDate = LocalDate.now();
                //int dayIndex = currentDate.getDayOfWeek().getValue() - 1;

                LocalTime currentTime = LocalTime.now();
                int daySeconds = currentTime.toSecondOfDay();
                int currentInterval = daySeconds / period;

                if (currentInterval != interval) { //checks whether interval hasn't changed
                    interval = currentInterval;
                    for (String act : values.keySet()) { //queries for values of actors that are in automatic or override
                        ACLMessage queryRL = new ACLMessage(ACLMessage.QUERY_REF); //query message
                        queryRL.addReceiver(new AID(name + "-" + act, AID.ISLOCALNAME));
                        JSONObject o1 = new JSONObject();
                        //o1.put("day", dayIndex);
                        o1.put("interval", interval);
                        queryRL.setContent(o1.toJSONString());
                        //queryRL.setContent(dayIndex + "," + interval);

                        ACLMessage informRL = new ACLMessage(ACLMessage.INFORM); //inform message for updating ql
                        informRL.addReceiver(new AID(name + "-" + act, AID.ISLOCALNAME));
                        JSONObject o2 = new JSONObject();
                        //o2.put("day", dayIndex);
                        o2.put("interval", interval);
                        o2.put("value", String.valueOf(values.get(act)));
                        o2.put("reward", "1");

                        informRL.setContent(o2.toJSONString());
                        //informRL.setContent(dayIndex + "," + interval + "," + values.get(act) + ",1");
                        if (automatic.contains(act)) {
                            send(queryRL);
                        }
                        else if (override.contains(act)) {
                            send(queryRL);
                            send(informRL);
                        }
                        else {
                            send(informRL);
                        }
                    }
                }

                ACLMessage msg;
                /*MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                                MessageTemplate.MatchPerformative(ACLMessage.CANCEL))
                );*/

                msg = receive();

                if (msg == null) {
                    //block until next interval
                    block((period - (LocalTime.now().toSecondOfDay() % period)) * period);
                }
                else if (msg.getPerformative() == ACLMessage.REQUEST) {

                    JSONObject obj = (JSONObject) JSONValue.parse(msg.getContent());

                    String type = (String) obj.get("_type");
                    obj.remove("_type");
                    //String payload = msg.getContent().substring(1);
                    if (type.equals("gui")) { //request from InterfaceAgent (meaning gui)
                        System.out.println(msg);
                        System.out.println(obj);
                        ACLMessage reply = msg.createReply(ACLMessage.AGREE);
                        send(reply);
                        setValues(obj);
                        requestActors();
                    }
                    else if (type.equals("user")) { //request from UserActorAgent -> coalition
                        //System.out.print(msg.getSender().getLocalName() + ": ");
                        //System.out.println(payload);
                        //String[] l = payload.replaceAll("\\s", "").split(":");
                        String act = (String) obj.get("actor");
                        Float val = Float.parseFloat((String) obj.get("value"));

                        ACLMessage reply = msg.createReply(ACLMessage.AGREE);
                        JSONObject o = new JSONObject();
                        o.put("_type", "user");
                        reply.setContent(o.toJSONString());
                        send(reply);

                        AID sender = msg.getSender();
                        System.out.println(leader);
                        System.out.println(new AID(getAID().getName()));
                        if (Objects.equals(leader.get(act), new AID(getAID().getName()))) { //is actor leader

                            if (isCoalition.containsKey(act) && isCoalition.get(act)) { //group exists
                                Map<AID, Float> rec = coalition.get(act);
                                rec.put(sender, val);
                                coalition.put(act, rec);
                            }
                            else { //group creation
                                isCoalition.put(act, true);
                                Map<AID, Float> m = new HashMap<>();
                                m.put(sender, val);
                                System.out.println(coalition);
                                coalition.put(act, m);
                                System.out.println(coalition);
                            }

                            coalitionToActor(act);
                        }
                        /*else {
                            ACLMessage reply = msg.createReply(ACLMessage.REFUSE);
                            reply.setContent("not a leader," + msg.getContent());
                            send(reply);
                        }*/
                    }
                }
                else if (msg.getPerformative() == ACLMessage.CANCEL) {
                    System.out.println(msg);

                    if (!msg.getSender().getLocalName().equals("_interface")) { //cancel from user
                        JSONObject obj = (JSONObject) JSONValue.parse(msg.getContent());

                        String actor = (String) obj.get("actor");
                        //String actor = msg.getContent();
                        Map<AID, Float> x = coalition.get(actor);
                        x.remove(msg.getSender());
                        if (x.isEmpty()) { //group is empty, so only one user actor control
                            coalition.remove(actor);
                            isCoalition.put(actor, false);
                            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                            req.addReceiver(new AID(actor, AID.ISLOCALNAME));
                            JSONObject o = new JSONObject();
                            o.put("value", String.valueOf(values.get(actor)));
                            req.setContent(o.toJSONString());
                            send(req);
                        }
                        else { //group control
                            coalition.put(actor, x);
                            coalitionToActor(actor);
                        }
                        return;
                    }

                    //cancel with deletion, because it is gui cancel
                    cancelActors(true);



                    /*ACLMessage reply = msg.createReply(ACLMessage.AGREE);
                    send(reply);
                    ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                    cancel.addReceiver(actor);
                    send(cancel);
                    values.clear();*/
                }
                else if (msg.getPerformative() == ACLMessage.REFUSE) {
                    //refuse from actor
                    if (pendingRequests.containsKey(msg.getConversationId()))
                        pendingRequests.remove(msg.getConversationId());
                    /*String[] l = msg.getContent().split(",", 2);
                    if (l[0].equals("not a leader")) { //change in leadership of coalition was probably done, try again
                        ACLMessage prop = new ACLMessage(ACLMessage.REQUEST);
                        prop.addReceiver(new AID());
                    }
                    else { //somebody else is being served on this actor (actor is sender)
                        String act = msg.getSender().getLocalName();
                        ACLMessage prop = new ACLMessage(ACLMessage.REQUEST);
                        prop.addReceiver(new AID(msg.getContent()));
                        prop.setContent("1" + act + values.get(act));
                    }*/
                    String act = msg.getSender().getLocalName();
                    ACLMessage prop = new ACLMessage(ACLMessage.REQUEST); //request to leader of actor control
                    JSONObject o1 = (JSONObject) JSONValue.parse(msg.getContent());
                    prop.addReceiver(new AID((String) o1.get("user")));
                    JSONObject o2 = new JSONObject();
                    o2.put("_type", "user");
                    o2.put("actor", act);
                    o2.put("value", String.valueOf(values.get(act)));

                    prop.setContent(o2.toJSONString());
                    //prop.setContent("1" + act + ":" + values.get(act));
                    String id = String.valueOf(System.currentTimeMillis()) + Math.floor(Math.random() *(1000 - 100 + 1) + 100);
                    pendingRequests.put(id, act);
                    prop.setConversationId(id);
                    send(prop);
                }
                else if (msg.getPerformative() == ACLMessage.INFORM) {
                    if (msg.getSender().getLocalName().equals("ams")) {
                        /*try {
                            ContentElement ce = getContentManager().extractContent(msg);
                            if (ce instanceof Done) {
                                return;
                            }
                        } catch (Codec.CodecException e) {
                            throw new RuntimeException(e);
                        } catch (OntologyException e) {
                            throw new RuntimeException(e);
                        }*/

                        return;
                    }

                    if (msg.getSender().getLocalName().equals("_comm")) { //inform about proximity change
                        JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                        String proximity = (String) o.get("proximity");
                        String actor = "_actor" + (String) o.get("location");
                        if (proximity.equals("on")) {
                            present.add(actor);
                            requestActor(actor);
                        }
                        else if (proximity.equals("off")) {
                            present.remove(actor);
                            //cancel without deletion, because value might be needed when user will be present
                            cancelActor(false, actor);
                            //cancelActors(false);
                        }
                        return;
                    }

                    JSONObject obj = (JSONObject) JSONValue.parse(msg.getContent());
                    String act = (String) obj.get("actor");
                    String inf = (String) obj.get("information");
                    //String[] l = msg.getContent().split(",");
                    //coalition cancel means leader canceled, user requests control for actor
                    if (inf.equals("coalition cancel") && values.get(act) != null) {
                        //String act = l[0];
                        //values.remove(l[0]);
                        coalition.remove(act);
                        isCoalition.put(act, false);
                        leader.remove(act);

                        ACLMessage actorReq = new ACLMessage(ACLMessage.REQUEST);
                        actorReq.addReceiver(new AID(act, AID.ISLOCALNAME));
                        JSONObject o = new JSONObject();
                        o.put("value", String.valueOf(values.get(act)));
                        actorReq.setContent(o.toJSONString());
                        //actorReq.setContent(String.valueOf(values.get(act)));

                        if (!Objects.equals(leader.get(act), new AID(getAID().getName()))) {
                            String id = String.valueOf(System.currentTimeMillis()) + Math.floor(Math.random() * (1000 - 100 + 1) + 100);
                            pendingRequests.put(id, act);
                            actorReq.setConversationId(id);
                        }

                        send(actorReq);
                    }
                }
                else if (msg.getPerformative() == ACLMessage.INFORM_REF) { //requested inform (RLAgent)
                    //System.out.println("Message from RLAgent");
                    //System.out.println(msg.getContent());
                    if (msg.getSender().getLocalName().startsWith(name + "-")) {
                        String actor = msg.getSender().getLocalName().substring(name.length() + 1);
                        if (override.contains(actor)) { //if actor in override
                            ACLMessage informRL = new ACLMessage(ACLMessage.INFORM);
                            informRL.addReceiver(msg.getSender());

                            //int interval = LocalTime.now().toSecondOfDay() / 1800;
                            //int dayIndex = LocalDate.now().getDayOfWeek().getValue() - 1;
                            JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                            String v = (String) o.get("value");
                            //o.put("day", dayIndex);
                            o.put("interval", interval);

                            //JSONObject o = new JSONObject();
                            //o.put("day", dayIndex);
                            //o.put("interval", interval);
                            //o.put("value", (String) o1.get("value"));

                            float dif = Math.abs(Float.parseFloat(v) - values.get(actor));
                            //small negative reward
                            if (dif < 0.6f && dif > 0.4f) {
                                o.put("reward", "-0.1");
                                informRL.setContent(o.toJSONString());
                                //informRL.setContent(dayIndex + "," + interval + "," + msg.getContent() + ",-0.1");

                                send(informRL);
                            }
                            //big negative reward
                            else if (!v.equals(values.get(actor).toString())) {
                                o.put("reward", "-1");
                                informRL.setContent(o.toJSONString());
                                //informRL.setContent(dayIndex + "," + interval + "," + msg.getContent() + ",-1");

                                send(informRL);
                            }
                        }
                        else { //if actor in automatic
                            JSONObject o = (JSONObject) JSONValue.parse(msg.getContent());
                            String value = (String) o.get("value");
                            values.put(actor, Float.valueOf(value));
                            if (present.contains(actor)) {
                                requestActor(actor);
                            }
                        }
                    }
                }
                else if (msg.getPerformative() == ACLMessage.AGREE) {
                    if (pendingRequests.containsKey(msg.getConversationId())) {
                        String actor = pendingRequests.get(msg.getConversationId());
                        JSONObject m = (JSONObject) JSONValue.parse(msg.getContent());
                        String t = (String) m.get("_type");
                        if (t.equals("actor")) { //solo actor control
                            leader.put(actor, new AID(getAID().getName()));
                        }
                        else if (t.equals("user")) { //group actor control
                            leader.put(actor, new AID(msg.getSender().getName()));
                            isCoalition.put(actor, true);
                        }

                        //if automatic, then inform interface of value that is being used for control
                        if (automatic.contains(actor)) {
                            ACLMessage interfaceInform = new ACLMessage(ACLMessage.INFORM);
                            interfaceInform.addReceiver(new AID("_interface", AID.ISLOCALNAME));
                            JSONObject o = new JSONObject();
                            o.put("actor", actor);
                            o.put("value", String.valueOf(values.get(actor)));
                            interfaceInform.setContent(o.toJSONString());
                            //interfaceInform.setContent(actor + "," + values.get(actor));
                            send(interfaceInform);
                        }
                        pendingRequests.remove(msg.getConversationId());
                    }
                }
                else {
                    System.out.print("unknown message: ");
                    System.out.println(msg);
                }
            }

            @Override
            public boolean done() {
                return false;
            }
        };

        /*Behaviour b2 = new SimpleBehaviour() {
            @Override
            public void action() {

            }

            @Override
            public boolean done() {
                return false;
            }
        };*/

        addBehaviour(b1);
    }
}
