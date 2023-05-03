package agents;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.ContainerID;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.CreateAgent;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Agent for easy setup of multi-agent system
 */
public class StartAgent extends Agent {


    /**
     * Creates in order needed agents, and agents for actor and user
     * that were given via agent arguments.
     */
    public void setup() {
        Object[] args = getArguments();
        int numOfLocations, numOfUsers;
        try {
            numOfLocations = Integer.parseInt(args[0].toString());
            numOfUsers = Integer.parseInt(args[1].toString());
        }
        catch (NumberFormatException ex){
            ex.printStackTrace();
            System.out.println("First and second argument should be number of locations and users, and it is not an integer.");
            doDelete();
            return;
        }

        if (numOfUsers + numOfLocations != args.length - 2) {
            System.out.println("Number of arguments does not align with first two arguments.");
            doDelete();
            return;
        }

        Set<String> locs = new HashSet<>();
        Set<String> users = new HashSet<>();

        //getting names of locations
        for (int i = 0; i < numOfLocations; i++) {
            String loc = args[i+2].toString();
            if (!loc.matches("^[a-zA-Z0-9]*$")) {
                System.out.println("Name of location can be only alphanumeric.");
                doDelete();
                return;
            }
            if (!locs.add(loc)) {
                System.out.println("Two locations share the same name.");
                doDelete();
                return;
            }
        }

        //getting names of users
        for (int i = 0; i < numOfUsers; i++) {
            String user = args[i+numOfLocations+2].toString();
            if (!user.matches("^[a-zA-Z0-9]*$")) {
                System.out.println("Name of user can be only alphanumeric.");
                doDelete();
                return;
            }
            if (!users.add(user)) {
                System.out.println("Two users share the same name.");
                doDelete();
                return;
            }
        }

        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
        getContentManager().registerOntology(JADEManagementOntology.getInstance());
        //creating sensors for locations
        for (String loc : locs) {
            this.getContainerController();

            CreateAgent ca = new CreateAgent();
            ca.setAgentName("_sensor" + loc);
            ca.setClassName("agents.SensorAgent");
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

        //creating users
        for (String user : users) {
            this.getContainerController();

            CreateAgent ca = new CreateAgent();
            ca.setAgentName(user);
            ca.setClassName("agents.UserAgent");
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

        //creating actors for locations
        for (String loc : locs) {
            this.getContainerController();

            CreateAgent ca = new CreateAgent();
            ca.setAgentName("_actor" + loc);
            ca.setClassName("agents.ActorAgent");
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

        this.getContainerController();

        //creating communication agent
        CreateAgent cac = new CreateAgent();
        cac.setAgentName("_comm");
        cac.setClassName("agents.CommunicationAgent");
        cac.setContainer(new ContainerID("Main-Container", null));
        Action actExprc = new Action(getAMS(), cac);

        ACLMessage requestc = new ACLMessage(ACLMessage.REQUEST);
        requestc.addReceiver(getAMS());
        requestc.setOntology(JADEManagementOntology.getInstance().getName());
        requestc.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        requestc.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        try {
            getContentManager().fillContent(requestc, actExprc);
            addBehaviour(new AchieveREInitiator(this, requestc) {
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

        this.getContainerController();

        //creating interface agent
        CreateAgent cai = new CreateAgent();
        cai.setAgentName("_interface");
        cai.setClassName("agents.InterfaceAgent");
        cai.setContainer(new ContainerID("Main-Container", null));
        Action actExpri = new Action(getAMS(), cai);

        ACLMessage requesti = new ACLMessage(ACLMessage.REQUEST);
        requesti.addReceiver(getAMS());
        requesti.setOntology(JADEManagementOntology.getInstance().getName());
        requesti.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        requesti.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        try {
            getContentManager().fillContent(requesti, actExpri);
            addBehaviour(new AchieveREInitiator(this, requesti) {
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
}
