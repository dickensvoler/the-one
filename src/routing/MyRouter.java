/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import javafx.util.Pair;

import util.Tuple;

import java.util.*;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 */
public class MyRouter extends ActiveRouter {
    /*	*//** identifier for the initial number of copies setting ({@value})*//*
	public static final String NROF_COPIES = "nrofCopies";
	*//** identifier for the binary-mode setting ({@value})*//*
	public static final String BINARY_MODE = "binaryMode";*/
    /**
     * identifier for the q-mode setting ({@value})
     */
    public static final String Q_MODE = "qMode";
    /**
     * Q-LearningSprayAndWait router's settings name space ({@value})
     */
    public static final String Q_LEARNINGSPRAYANDWAIT_NS = "Q-LearningSprayAndWaitRouter";
    /**
     * Message property key
     */
    public static final String MSG_COUNT_PROPERTY = Q_LEARNINGSPRAYANDWAIT_NS + "." +
            "copies";
    public static final String NROF_HOSTS = "nrofHosts";

    public double activity;

    public static final String BETA = "beta";

    public int nodeCount = 0;
    public double Popularity;
    public static final String THRESHOLD = "threshold";
    public Set<DTNHost> ENS = null;


    private List<DTNHost> actions;
    public static final String NR_OF_INTERFACES = "nrOfInterface";
    public static final String ALPHA = "alpha";
    public static final String LEARNING_RATE = "learningRate";
    public static final String GAMMA = "gamma";
    public static final String EPSILON = "epsilon";
    public double beta;
    public double learningRate;
    public double gamma;
    public double epsilon;
    public double threshold;
    public int nrOfInterfaces;
    public int nrOfHosts;
    public double reward;

    public List<DTNHost> getActions() {
        return actions;
    }

    public List<DTNHost> states = new ArrayList();
    private HashMap<Pair<DTNHost, DTNHost>, Double> Qtable;

    /**
     * Constructor
     * @param s
     */
    public MyRouter(Settings s) {
        super(s);
        Settings snwSettings = new Settings(Q_LEARNINGSPRAYANDWAIT_NS);
        int numOfActions = Integer.parseInt(NR_OF_INTERFACES);
        actions = new ArrayList<DTNHost>();
        actions.add(getHost());
        this.learningRate = snwSettings.getDouble(LEARNING_RATE, 0.1);
        this.gamma = snwSettings.getDouble(GAMMA, 0.9);
        this.epsilon = snwSettings.getDouble(EPSILON, 0.8);
        this.threshold = snwSettings.getDouble(THRESHOLD, 0.5);
        this.nrOfInterfaces = snwSettings.getInt(NR_OF_INTERFACES, 4);
        this.nrOfHosts = snwSettings.getInt(NROF_HOSTS, 200);
        this.beta = snwSettings.getDouble(BETA, 0.5);
        Qtable = new HashMap<>();

    }


    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected MyRouter(MyRouter r) {
        super(r);


        //this.initialNrofCopies = r.initialNrofCopies;
        //this.isBinary = r.isBinary;
    }

    public Connection chooseAction(DTNHost dest) {
        Collection<Message> msgCollection = getMessageCollection();
        List<Connection> cons = getConnections();
        DTNHost action = null;
        List<Pair<DTNHost, DTNHost>> keySet = new ArrayList<>();
        Map<DTNHost, Connection> actionConnectionMap = new HashMap<>();
        for (Connection con :
                cons) {
            actions.add(con.getOtherNode(getHost()));
            actionConnectionMap.put(con.getOtherNode(getHost()), con);
        }
        for (Message m :
                msgCollection) {
            DTNHost state = m.getTo();
            checkStateExist(state);
        }

        Connection selectedConnection = null;
        if (Math.random() > epsilon) {
            int index = (int) (Math.random() * actions.size());
            action = actions.get(index);
            selectedConnection = actionConnectionMap.get(action);
        } else {
            double maxQ = 0;
            for (DTNHost act :
                    actions) {
                Pair<DTNHost, DTNHost> keyPair = new Pair<>(dest, act);
                keySet.add(keyPair);
            }
            for (Pair<DTNHost, DTNHost> key : keySet) {

                if (Qtable.get(key) > maxQ) {
                    action = key.getValue();
                    maxQ = Qtable.get(key);
                    selectedConnection = actionConnectionMap.get(action);
                }
            }
        }
        return selectedConnection;
    }

    public void checkStateExist(DTNHost state) {
        if (!states.contains(state)) {
            states.add(state);
            for (DTNHost action :
                    actions) {
                Pair<DTNHost, DTNHost> Qkey = new Pair<>(state, action);
                Qtable.put(Qkey, 0.0);
            }
        }
    }

    public void updateStates(){
        Collection<Message> msgCollection = getMessageCollection();
        Set<DTNHost> destSet = new TreeSet<>();
        for (Message m : msgCollection) {
            destSet.add(m.getTo());
            if (!states.contains(m.getTo())){
                states.add(m.getTo());
            }
        }
        for (DTNHost state:
             states) {
            if (!destSet.contains(state)){
                states.remove(state);
            }
        }
    }

    public void learn(DTNHost state, DTNHost action, double reward) {
        checkStateExist(state);
        Pair<DTNHost, DTNHost> Qkey = new Pair<DTNHost, DTNHost>(state, action);
        double maxQ = 0;
        List<Double> qValueList = getQValueFromOther(action, state);
        for (Double q :
                qValueList) {
            if (q > maxQ)
                maxQ = q;
        }


        double qPredict = Qtable.get(Qkey);
        double qTarget = reward + gamma * maxQ;
        double qValue = qPredict + learningRate * (qTarget - qPredict);
        Qtable.put(new Pair<>(state, action), qValue);
    }

    /**
     * calculate activity of node i
     */
    public void setActivity() {
        this.activity = nodeCount / nrOfHosts;
    }

    public double getActivity() {
        return activity;
    }

    public HashMap<Pair<DTNHost, DTNHost>, Double> getQ_table() {
        return Qtable;
    }


    public void updateENS(Connection con){
        ENS.add(con.getOtherNode(getHost()));
    }

    /**
     * exchange and update ENS of each node.
     *
     * @param con
     */
    public void exchangeENS(Connection con) {
        DTNHost other = con.getOtherNode(getHost());
        MyRouter r = (MyRouter) other.getRouter();
        this.ENS.addAll(r.ENS);
    }

    public List<Double> getQValueFromOther(DTNHost other, DTNHost dest){
        MyRouter router = (MyRouter) other.getRouter();
        HashMap<Pair<DTNHost, DTNHost>, Double> otherQtable = router.getQ_table();
        List<DTNHost> otherActions = router.getActions();
        List<Double> qValueList = new ArrayList<>();
        for (DTNHost action:
             otherActions) {
            qValueList.add(otherQtable.get(new Pair<>(dest, action)));
        }
        return qValueList;
    }

    /**
     * calculate node density
     *
     * @return node density
     */
    public double calculateDensity() {
        return (ENS.size()) / nrOfHosts;
    }

    /**
     * calculate the number of copies
     *
     * @param m
     * @return
     */
    public int calculateNROfCopies(Message m) {

        List<Connection> curConnections = getConnections();
        if (curConnections.contains(m.getTo())) {
            //TODO:activate transfer function
            return 1;
        }


        int maxL = nodeCount;
        double density = calculateDensity();
        double NROfCopies = maxL * density / 0.5;
        return (int) NROfCopies;
    }

    public void setPopularity() {
        this.Popularity = this.ENS.size() / threshold;
    }

    public void updatePopularity() {
        double alpha = Double.parseDouble(this.ALPHA);
        this.Popularity = (1 - alpha) * this.Popularity + alpha * this.Popularity;
    }

    public void setReward(Connection con, Message m) {
        MyRouter otherRouter = (MyRouter) con.getOtherNode(getHost()).getRouter();
        reward = otherRouter.ENS.contains(m.getTo()) ? 1 : -1;
    }

    public void updateQValueWhenDisconnected(Connection con){
        DTNHost other = con.getOtherNode(getHost());
        for (DTNHost state:
             states) {
            Double qValue = Qtable.get(new Pair<>(state, other));
            qValue = qValue*beta;
            Qtable.put(new Pair<>(state, other), qValue);
        }
    }

/*    private class UpdateByTimer extends TimerTask{
        @Override
        public void run(){
            updateQValueWhenDisconnected(con);
        }
    }*/


    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);

        DTNHost other = con.getOtherNode(getHost());
        updateStates();
        List<Message> msgList = sortByQueueMode((List) getMessageCollection());

        if (con.isUp()) {
            updateENS(con);
            exchangeENS(con);
            this.nodeCount++;
            setReward(con, msgList.get(0));
            learn(msgList.get(0).getTo(), other, reward);
        }

        if (!con.isUp()){
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        updateQValueWhenDisconnected(con);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0,600*1000);
        }
    }


    @Override
    public int receiveMessage(Message m, DTNHost from) {
        return super.receiveMessage(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);
        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);

        assert nrofCopies != null : "Not a SnW message: " + msg;
        nrofCopies = (int) Math.floor(nrofCopies / 2.0);
        /*if (isBinary) {
             //in binary S'n'W the receiving node gets floor(n/2) copies
            nrofCopies = (int) Math.floor(nrofCopies / 2.0);
        } else {
             //in standard S'n'W the receiving node gets only single copy
            nrofCopies = 1;
        }*/
/*        if (Qtable.get(new Pair<>(msg.getTo(), getHost()))>threshold){
            nrofCopies = calculateNROfCopies(msg);
        } else {
            nrofCopies = 1;
        }*/


        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
        return msg;
    }

    @Override
    public boolean createNewMessage(Message msg) {
        makeRoomForNewMessage(msg.getSize());

        msg.setTtl(this.msgTtl);
        msg.addProperty(MSG_COUNT_PROPERTY, calculateNROfCopies(msg));
        addToMessages(msg, true);
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        /* try messages that could be delivered to final recipient */
        if (exchangeDeliverableMessages() != null) {
            return;
        }

        /* create a list of SAWMessages that have copies left to distribute */
        @SuppressWarnings(value = "unchecked")
        List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());

        if (copiesLeft.size() > 0) {
            /* try to send those messages */
            for (Message m : copiesLeft) {
                DTNHost dest = m.getTo();
                Connection connection = chooseAction(dest);
                startTransfer(m, connection);
            }
        }
    }

    /**
     * Creates and returns a list of messages this router is currently
     * carrying and still has copies left to distribute (nrof copies > 1).
     *
     * @return A list of messages that have copies left
     */
    protected List<Message> getMessagesWithCopiesLeft() {
        List<Message> list = new ArrayList<Message>();

        for (Message m : getMessageCollection()) {
            Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
            assert nrofCopies != null : "SnW message " + m + " didn't have " +
                    "nrof copies property!";
            if (nrofCopies > 1) {
                list.add(m);
            }
        }

        return list;
    }

    /**
     * Called just before a transfer is finalized (by
     * {@link ActiveRouter#update()}).
     * Reduces the number of copies we have left for a message.
     * In binary Spray and Wait, sending host is left with floor(n/2) copies,
     * but in standard mode, nrof copies left is reduced by one.
     */
    @Override
    protected void transferDone(Connection con) {
        Integer nrofCopies;
        String msgId = con.getMessage().getId();
        /* get this router's copy of the message */
        Message msg = getMessage(msgId);

        if (msg == null) { // message has been dropped from the buffer after..
            return; // ..start of transfer -> no need to reduce amount of copies
        }

        /* reduce the amount of copies left */
        nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
        nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);
    }

    @Override
    public MyRouter replicate() {
        return new MyRouter(this);
    }
}
