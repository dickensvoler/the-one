/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import javafx.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Implementation of Spray and wait router as depicted in
 * <I>Spray and Wait: An Efficient Routing Scheme for Intermittently
 * Connected Mobile Networks</I> by Thrasyvoulos Spyropoulus et al.
 */
public class QLearningSprayAndWaitRouter extends ActiveRouter {
    	//** identifier for the initial number of copies setting ({@value})*//*

    /** delivery predictability initialization constant*/
    public static final double P_INIT = 0.75;
    /** delivery predictability transitivity scaling constant default value */
    public static final double DEFAULT_BETA = 0.25;
    /** delivery predictability aging constant */
    public static final double DEFAULT_GAMMA = 0.98;
    /**
     * Number of seconds in time unit -setting id ({@value}).
     * How many seconds one time unit is when calculating aging of
     * delivery predictions. Should be tweaked for the scenario.*/
    public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
    /**
     * Transitivity scaling constant (beta) -setting id ({@value}).
     * Default value for setting is {@link #DEFAULT_BETA}.
     */
    public static final String BETA_S = "beta_s";
    /**
     * Predictability aging constant (gamma) -setting id ({@value}).
     * Default value for setting is {@link #DEFAULT_GAMMA}.
     */
    public static final String GAMMA_S = "gamma_s";
    /** the value of nrof seconds in time unit -setting */
    private int secondsInTimeUnit;
    /** value of beta setting */
    private double beta_s;
    /** value of gamma setting */
    private double gamma_s;
    /** delivery predictabilities */
    private Map<DTNHost, Double> preds;
    /** last delivery predictability update (sim)time */
    private double lastAgeUpdate;

    private double lastAgeUpdateQ;



    public static final String NROF_COPIES = "nrofCopies";
	//** identifier for the binary-mode setting ({@value})*//*
	//public static final String BINARY_MODE = "binaryMode";*/
    /**
     * identifier for the q-mode setting ({@value})
     */
    //public static final String Q_MODE = "qMode";
    /**
     * Q-LearningSprayAndWait router's settings name space ({@value})
     */
    public static final String Q_LEARNINGSPRAYANDWAIT_NS = "QLearningSprayAndWaitRouter";
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
    public HashSet<DTNHost> ENS = new HashSet<>();


    private List<DTNHost> actions = new ArrayList<DTNHost>();
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
    public int nrofCopies;

    public HashSet<DTNHost> destinations = new HashSet<>();

    public List<DTNHost> getActions() {
        return actions;
    }

    public List<DTNHost> states = new ArrayList();
    private HashMap<Pair<DTNHost, DTNHost>, Double> Qtable = new HashMap<Pair<DTNHost, DTNHost>, Double>();

    /**
     * Constructor
     * @param s
     */
    public QLearningSprayAndWaitRouter(Settings s) {
        super(s);
        Settings snwSettings = new Settings(Q_LEARNINGSPRAYANDWAIT_NS);
        //int numOfActions = Integer.parseInt(NR_OF_INTERFACES);
        actions = new ArrayList<DTNHost>();
        actions.add(getHost());
        this.learningRate = snwSettings.getDouble(LEARNING_RATE, 0.1);
        this.gamma = snwSettings.getDouble(GAMMA, 0.9);
        this.epsilon = snwSettings.getDouble(EPSILON, 0.8);
        this.threshold = snwSettings.getDouble(THRESHOLD, 0.5);
        this.nrOfInterfaces = snwSettings.getInt(NR_OF_INTERFACES, 4);
        this.nrOfHosts = snwSettings.getInt(NROF_HOSTS, 200);
        this.beta = snwSettings.getDouble(BETA, 0.5);
        this.nrofCopies = snwSettings.getInt(NROF_COPIES);
        secondsInTimeUnit = snwSettings.getInt(SECONDS_IN_UNIT_S);
        if (snwSettings.contains(BETA_S)) {
            beta_s = snwSettings.getDouble(BETA_S);
        }
        else {
            beta_s = DEFAULT_BETA;
        }

        if (snwSettings.contains(GAMMA_S)) {
            gamma_s = snwSettings.getDouble(GAMMA_S);
        }
        else {
            gamma_s = DEFAULT_GAMMA;
        }
        DTNHost self = getHost();
        this.ENS.add(self);

        initPreds();
        //Qtable = new HashMap<>();

    }


    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected QLearningSprayAndWaitRouter(QLearningSprayAndWaitRouter r) {
        super(r);
        actions = new ArrayList<DTNHost>();
        actions.add(getHost());
        this.learningRate = r.learningRate;
        this.gamma = r.gamma;
        this.epsilon = r.epsilon;
        this.threshold = r.threshold;
        this.nrOfInterfaces = r.nrOfInterfaces;
        this.nrOfHosts = r.nrOfHosts;
        this.beta = r.beta;
        this.ENS = r.ENS;
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.beta_s = r.beta_s;
        this.gamma_s = r.gamma_s;

        initPreds();

        //this.initialNrofCopies = r.initialNrofCopies;
        //this.isBinary = r.isBinary;
    }
    public double getLastAgeUpdateQ() {
        return lastAgeUpdateQ;
    }

    public void setLastAgeUpdateQ(double lastAgeUpdateQ) {
        this.lastAgeUpdateQ = lastAgeUpdateQ;
    }
    /**
     * Initializes predictability hash
     */
    private void initPreds() {
        this.preds = new HashMap<DTNHost, Double>();
    }
    /**
     * Updates delivery predictions for a host.
     * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
     * @param host The host we just met
     */
    private void updateDeliveryPredFor(DTNHost host) {
        double oldValue = getPredFor(host);
        double newValue = oldValue + (1 - oldValue) * P_INIT;
        preds.put(host, newValue);
    }
    /**
     * Returns the current prediction (P) value for a host or 0 if entry for
     * the host doesn't exist.
     * @param host The host to look the P for
     * @return the current P value
     */
    public double getPredFor(DTNHost host) {
        ageDeliveryPreds(); // make sure preds are updated before getting
        if (preds.containsKey(host)) {
            return preds.get(host);
        }
        else {
            return 0;
        }
    }
    /**
     * Returns a map of this router's delivery predictions
     * @return a map of this router's delivery predictions
     */
    private Map<DTNHost, Double> getDeliveryPreds() {
        ageDeliveryPreds(); // make sure the aging is done
        return this.preds;
    }
    /**
     * Updates transitive (A->B->C) delivery predictions.
     * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
     * </CODE>
     * @param host The B host who we just met
     */
    private void updateTransitivePreds(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof QLearningSprayAndWaitRouter : "PRoPHET only works " +
                " with other routers of same type";

        double pForHost = getPredFor(host); // P(a,b)
        Map<DTNHost, Double> othersPreds =
                ((QLearningSprayAndWaitRouter)otherRouter).getDeliveryPreds();

        for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
            if (e.getKey() == getHost()) {
                continue; // don't add yourself
            }

            double pOld = getPredFor(e.getKey()); // P(a,c)_old
            double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta_s;
            preds.put(e.getKey(), pNew);
        }
    }
    /**
     * Ages all entries in the delivery predictions.
     * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
     * time units that have elapsed since the last time the metric was aged.
     * @see #SECONDS_IN_UNIT_S
     */
    private void ageDeliveryPreds() {
        double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
                secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(gamma_s, timeDiff);
        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            e.setValue(e.getValue()*mult);
        }

        this.lastAgeUpdate = SimClock.getTime();
    }

    private void ageQValue(Pair<DTNHost, DTNHost> key){
        double timeDiff = (SimClock.getTime()-this.lastAgeUpdateQ) / secondsInTimeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(beta, timeDiff);
        Qtable.put(key, Qtable.get(key)*mult);

        this.lastAgeUpdateQ = SimClock.getTime();
    }


    /**
     * Select an action depends on the Q-value or randomly
     * @param dest destination of the message
     * @return selected connection
     */
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

    /**
     * check if the destination(state) is in the list of states
     * @param state destination of message
     */
    public void checkStateExist(DTNHost state) {
        if (!states.contains(state)) {
            states.add(state);
            for (DTNHost action :
                    actions) {
                if (action == null){
                    continue;
                }
                Pair<DTNHost, DTNHost> Qkey = new Pair<>(state, action);
                Qtable.put(Qkey, 0.0);
            }
        }
    }

    /**
     * update states when new message created
     */
    public void updateStates(){
        Collection<Message> msgCollection = getMessageCollection();
        Set<DTNHost> destSet = new HashSet<>();
        for (Message m : msgCollection) {
            destSet.add(m.getTo());
            if (!states.contains(m.getTo())){
                states.add(m.getTo());
            }
        }
        Iterator<DTNHost> it = states.iterator();
        while(it.hasNext()){
            if (!destSet.contains(it.next())){
                it.remove();
            }
        }
    }

    /**
     * update Q-value when state changed or connection changed
     * @param state
     * @param action
     * @param reward
     */
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
        Double prophetProbability = getPredFor(action);

        double qPredict = Qtable.get(Qkey);
        double qTarget = reward + gamma * prophetProbability * maxQ;
        double qValue = learningRate*qTarget-(1-learningRate)*qPredict;
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

    public void updateActions(Connection con){
        if (con.isUp()){
            actions.add(con.getOtherNode(getHost()));
        }else {
            actions.remove(con.getOtherNode(getHost()));
        }

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
        QLearningSprayAndWaitRouter r = (QLearningSprayAndWaitRouter) other.getRouter();
        this.ENS.addAll(r.ENS);
    }

    public void removeReplicas(List list){
        HashSet<DTNHost> hashSet = new HashSet<>(list);
    }

    public List<Double> getQValueFromOther(DTNHost other, DTNHost dest){
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        HashMap<Pair<DTNHost, DTNHost>, Double> otherQtable = router.getQ_table();
        List<DTNHost> otherActions = router.getActions();
        List<Double> qValueList = new ArrayList<>();
        if (otherActions.size()==0||otherQtable.size()==0){
            qValueList.add(0.0);
            return qValueList;
        }
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

    public void setReward(DTNHost other, DTNHost d) {
        QLearningSprayAndWaitRouter otherRouter = (QLearningSprayAndWaitRouter) other.getRouter();


            if (otherRouter.ENS.contains(d)){
                reward = 1;
            } else if (otherRouter.actions.contains(d)) {
                reward = 100;
            } else {
                reward = -1;
            }


    }

    public void updateQValueWhenDisconnected(Connection con){
        DTNHost other = con.getOtherNode(getHost());
        System.out.println(con);
        for (DTNHost state:
             states) {
            Double qValue = Qtable.get(new Pair<>(state, other));
            qValue = qValue*beta;
            Qtable.put(new Pair<>(state, other), qValue);
        }
    }

    public void updateDestinationSet(DTNHost other){
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        this.destinations.addAll(router.destinations);
    }

    public void initializeQtable(){
        if (destinations.size()==0)
            return;
        for (DTNHost d:
             destinations) {
            for (DTNHost e:
                 ENS) {
                if (!Qtable.containsKey(new Pair<>(d,e))){
                    Qtable.put(new Pair<>(d, e), 0.0);
                }
            }
        }
    }

    public void updateQtableWhenConnected(DTNHost other){
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        Pair<DTNHost, DTNHost> Qkey;
        for (DTNHost d:
             destinations) {
            Qkey = new Pair<>(d,other);
            if (Qtable.containsKey(Qkey)){
                setReward(other, d);
                ageQValue(Qkey);
                learn(d, other, reward);
            }
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



        List<Message> msgList = new ArrayList<Message>(getMessageCollection());
        msgList = sortByQueueMode(msgList);

        if (con.isUp()) {
            DTNHost other = con.getOtherNode(getHost());
            updateDeliveryPredFor(other);
            updateTransitivePreds(other);
            updateDestinationSet(other);
            updateActions(con);


            if (!ENS.contains(other))
                updateENS(con);
            //exchangeENS(con);

            initializeQtable();
            this.nodeCount++;

            //setReward(other, );
            updateQtableWhenConnected(other);
            /*else {
                learn(getHost(),other,reward);
            }*/

        }

        if (!con.isUp()){
            updateActions(con);
            //ageQValue();
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
        msg.addProperty(MSG_COUNT_PROPERTY, new Integer(nrofCopies));
        addToMessages(msg, true);
        updateStates();
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
    public QLearningSprayAndWaitRouter replicate() {
        return new QLearningSprayAndWaitRouter(this);
    }
}
