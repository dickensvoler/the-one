/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import core.*;
import javafx.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import util.Tuple;

import javax.swing.text.html.HTMLDocument;
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

    private double lastAgeUpdateA;




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
    public static final String NROF_COPIES = "nrofCopies";
    public static final String NROF_HOSTS = "nrofHosts";

    public double activity;

    public double socialProperty;



    public static final String BETA = "beta";

    public int nodeCount = 0;
    public double Popularity;
    public static final String THRESHOLD = "threshold";
    public HashSet<DTNHost> ENS = new HashSet<>();
    public HashSet<DTNHost> RENS = new HashSet<>();


    private Set<DTNHost> actions = new HashSet<>();
    public static final String NR_OF_INTERFACES = "nrOfInterface";
    public static final String ALPHA = "alpha";
    public static final String LEARNING_RATE = "learningRate";
    public static final String GAMMA = "gamma";
    public static final String EPSILON = "epsilon";
    public static final String REWARD_TUNE_PARAMETER = "rewardTuneParameter";
    public double alpha;
    public double beta;
    public double learningRate;
    public double gamma;
    public double epsilon;
    public double threshold;
    public int nrOfInterfaces;
    public int nrOfHosts;
    public double reward;
    public int nrofCopies;
    private int nodeCountIn200;
    private double rewardTune;

    public int DeliveredMessages = 0;

    public HashSet<DTNHost> destinations = new HashSet<>();
    private Double Q_THRESHOLD = 0.1;

    private int timeUnit = 200;

    public Set<DTNHost> getActions() {
        return actions;
    }

    public DTNHost getElementFromActions(int index){
        List<DTNHost> actionList = new ArrayList<>(actions);
        return actionList.get(index);
    }

    //public List<DTNHost> states = new ArrayList();
    private HashMap<Pair<DTNHost, DTNHost>, Double> Qtable = new HashMap<Pair<DTNHost, DTNHost>, Double>();

    /**
     * Constructor
     * @param s
     */
    public QLearningSprayAndWaitRouter(Settings s) {
        super(s);
        Settings snwSettings = new Settings(Q_LEARNINGSPRAYANDWAIT_NS);
        //int numOfActions = Integer.parseInt(NR_OF_INTERFACES);

        actions.add(getHost());
        actions.remove(null);
        this.learningRate = snwSettings.getDouble(LEARNING_RATE, 0.1);
        this.gamma = snwSettings.getDouble(GAMMA, 0.9);
        this.epsilon = snwSettings.getDouble(EPSILON, 0.8);
        this.threshold = snwSettings.getDouble(THRESHOLD, 50);
        this.nrOfInterfaces = snwSettings.getInt(NR_OF_INTERFACES, 4);
        this.nrOfHosts = snwSettings.getInt(NROF_HOSTS,100);
        this.beta = snwSettings.getDouble(BETA, 0.5);
        this.nrofCopies = snwSettings.getInt(NROF_COPIES, 6);
        this.rewardTune = snwSettings.getDouble(REWARD_TUNE_PARAMETER, 0.5);
        this.alpha = snwSettings.getDouble(ALPHA, 0.5);
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

        this.actions = r.actions;
        this.learningRate = r.learningRate;
        this.gamma = r.gamma;
        this.epsilon = r.epsilon;
        this.threshold = r.threshold;
        this.nrOfInterfaces = r.nrOfInterfaces;
        this.nrOfHosts = r.nrOfHosts;
        this.nrofCopies = r.nrofCopies;
        this.beta = r.beta;
        this.ENS = r.ENS;
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.beta_s = r.beta_s;
        this.gamma_s = r.gamma_s;
        this.rewardTune = r.rewardTune;
        this.alpha = r.alpha;


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

    public int getNodeCountIn200() {
        return nodeCountIn200;
    }

    public void setNodeCountIn200(int nodeCountIn200) {
        this.nodeCountIn200 = nodeCountIn200;
    }

    public double getSocialProperty() {
        return socialProperty;
    }

    public void setSocialProperty() {
        this.socialProperty = this.activity+this.Popularity;
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

    private void ageQValue(DTNHost other){
        double timeDiff = (SimClock.getTime()-this.lastAgeUpdateQ) / timeUnit;

        if (timeDiff == 0) {
            return;
        }

        double mult = Math.pow(beta, timeDiff);
        for (Map.Entry<Pair<DTNHost,DTNHost>,Double> e: Qtable.entrySet()
             ) {
            if (actions.contains(e.getKey().getValue())&&e.getKey().getValue()!=other) {
                continue;
            }else{
                e.setValue(e.getValue()*mult);}
        }


        this.lastAgeUpdateQ = SimClock.getTime();
    }


    private void ageActivity(){
        double timeDiff = (SimClock.getTime()-this.lastAgeUpdateA);

        if (timeDiff <= 200) {
            return;
        }else {
           activity = nodeCountIn200 / threshold;
           nodeCountIn200 = 0;
        }

        this.lastAgeUpdateA = SimClock.getTime();
    }


    /**
     * Select an action depends on the Q-value or randomly
     * @param dest destination of the message
     * @return selected connection
     */
    public Connection chooseAction(DTNHost dest) {
        updateActions();
        if (actions.contains(dest)) {
            return getConnectionByOtherHost(dest);
        }
//        Collection<Message> msgCollection = getMessageCollection();
        List<Connection> connections = getConnections();
        Set<Connection> cons = new HashSet<>(connections);
        DTNHost action = null;
        List<Pair<DTNHost, DTNHost>> keySet = new ArrayList<>();
        Map<DTNHost, Connection> actionConnectionMap = new HashMap<>();

/*        for (Connection con :
                cons) {
            actions.add(con.getOtherNode(getHost()));
            actionConnectionMap.put(con.getOtherNode(getHost()), con);
        }*/
        checkQvalueStateExist();

        Connection selectedConnection = null;
        DTNHost selectedHost = null;

        if (Math.random() > epsilon) {
            //double i = Math.random();
            int index = (int) (Math.random() * (double) actions.size());
            action = getElementFromActions(index);
            selectedConnection = getConnectionByOtherHost(action);
        } else {
            double maxQ = 0;

            for (DTNHost act :
                    actions) {
               //learn(dest,act);
                Pair<DTNHost, DTNHost> keyPair = new Pair<>(dest, act);
                keySet.add(keyPair);

            }
            for (Pair<DTNHost, DTNHost> key : keySet) {

                if (Qtable.get(key) > maxQ) {
                    action = key.getValue();
                    maxQ = Qtable.get(key);
                    selectedConnection = getConnectionByOtherHost(action);
                    selectedHost = action;
                }
            }
        }
        if (selectedConnection == null || selectedHost == getHost()) {
            return null;
        }
        return selectedConnection;
    }

    private void updateQtable(DTNHost host) {

        for (DTNHost d:
             destinations) {
            if (!Qtable.containsKey(new Pair<>(d, host))) {
                Qtable.put(new Pair<>(d,host), 0.0);
            }
            double Q = Qtable.get(new Pair<>(d,host));
            Q = Q+0.3*DeliveredMessages;
            Qtable.put(new Pair<>(d,host), Q);
        }

    }

    private Connection getConnectionByOtherHost(DTNHost dest) {
        Set<Connection> connections = new HashSet<>(getConnections());
        for (Connection con:
             connections) {
            if (con.getOtherNode(getHost()) == dest) {
                return con;
            }
        }
        return null;
    }

    /**
     * check if the destination(state) is in the list of states
     */
    public void checkQvalueStateExist() {
        for (DTNHost d:
             destinations) {
            for (DTNHost a:
                 actions) {
                if (!Qtable.containsKey(new Pair<>(d, a))) {
                    Qtable.put(new Pair<>(d, a), 0.0);
                }
            }
        }
    }


/*    *//**
     * update states when new message created
     *//*
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
    }*/

    /**
     * update Q-value when state changed or connection changed
     * @param state
     * @param action
     *
     */
    public void learn(DTNHost state, DTNHost action) {
        checkQvalueStateExist();
        Pair<DTNHost, DTNHost> Qkey = new Pair<DTNHost, DTNHost>(state, action);
        double maxQ = 0;
        List<Double> qValueList = getQValueFromOther(action, state);
        for (Double q :
                qValueList) {
            if (q > maxQ)
                maxQ = q;
        }
        Double prophetProbability = getPredFor(action);

        setReward(action, state);
        double qPredict = Qtable.get(Qkey);
        double qTarget = reward + gamma * prophetProbability * maxQ;
        double qValue = learningRate*qTarget+qPredict;
        Qtable.put(new Pair<>(state, action), qValue);
    }

    /**
     * calculate activity of node i
     */
    public void updateActivity() {
        this.activity = nodeCountIn200 / threshold;
        nodeCountIn200 = 0;
    }

/*    public void runTask() {
        Timer timer = new Timer();
        MyTask task = new MyTask();
        //run every 200 seconds
        timer.schedule(task, 200000, 200000);
    }
    class MyTask extends TimerTask{

        //在run方法中的语句就是定时任务执行时运行的语句。
        public void run() {
            updateActivity();
        }
    }*/




    public double getActivity() {
        return activity;
    }

    /**
     * update actions when connection changed
     *
     */
    public void updateActions(){
        List<Connection> conList = getConnections();
        actions.clear();
        DTNHost host = getHost();
        actions.add(host);
        for (Connection con : conList) {
            actions.add(con.getOtherNode(getHost()));
        }

    }

    public HashMap<Pair<DTNHost, DTNHost>, Double> getQ_table() {
        return Qtable;
    }


    public void updateENS(Connection con){
        ENS.add(con.getOtherNode(getHost()));
        updatePopularity();
    }

    /**
     * exchange and update ENS of each node.
     *
     * @param con
     */
    public void exchangeRENS(Connection con) {
        DTNHost other = con.getOtherNode(getHost());
        QLearningSprayAndWaitRouter r = (QLearningSprayAndWaitRouter) other.getRouter();
        for (DTNHost encounter:
             r.ENS) {
            if (!ENS.contains(encounter)){
                RENS.add(encounter);
            }
        }
        for (DTNHost encounter :
                r.RENS) {
            if (!ENS.contains(encounter)){
                RENS.add(encounter);
            }
        }
    }



    public List<Double> getQValueFromOther(DTNHost other, DTNHost dest){
        List<Double> qValueList = new ArrayList<>();

        if (other.equals(getHost())) {
            qValueList.add(0.0);
            return qValueList;
        }
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        HashMap<Pair<DTNHost, DTNHost>, Double> otherQtable = router.getQ_table();

        //test
        List<Connection> con = router.getConnections();

        Set<DTNHost> otherActions = router.actions;
        if (otherActions.size()==0||otherQtable.size()==0){
            qValueList.add(0.0);
            return qValueList;
        }
        for (DTNHost action:
             otherActions) {
            if (!otherQtable.containsKey(new Pair<>(dest, action))) {
                qValueList.add(0.0);
            } else {
                qValueList.add(otherQtable.get(new Pair<>(dest, action)));
            }

        }
        return qValueList;
    }

    /**
     * calculate node density
     *
     * @return node density
     */
    public double calculateDensity() {
        return (double) ENS.size() / (double) nrOfHosts;
    }

    /**
     * calculate the number of copies
     *
     * @param m
     * @return
     */
    public int calculateNROfCopies(Message m) {

        List<Connection> curConnections = getConnections();
        Set<Connection> connections = new HashSet<>(curConnections);
        if (connections.contains(m.getTo())) {
            //TODO:activate transfer function
            return 1;
        }


        int maxL = 6;
        double density = calculateDensity();
        double NROfCopies = maxL*density;
        return (int) NROfCopies;
    }

    public void setPopularity() {
        this.Popularity = this.ENS.size() / threshold;
    }



    public void updatePopularity() {
        double p = Popularity;
        setPopularity();
        this.Popularity = (1 - alpha) * this.Popularity + alpha * p;
    }

    public void setReward(DTNHost other, DTNHost d) {
        QLearningSprayAndWaitRouter otherRouter = (QLearningSprayAndWaitRouter) other.getRouter();


            if (otherRouter.ENS.contains(d)&&!otherRouter.actions.contains(d)){
                reward = 1 + rewardTune * otherRouter.activity +
                        (1 - rewardTune) * otherRouter.Popularity;
            } /*else if (otherRouter.actions.contains(d)) {
                reward = 10 + rewardTune * otherRouter.activity +
                        (1 - rewardTune) * otherRouter.Popularity;
            }*/ else {
                reward = -1;
            }
/*        if (otherRouter.ENS.contains(d)){
            reward = 1 ;
        } else if (otherRouter.actions.contains(d)) {
            reward = 10 ;
        } else {
            reward = -1;
        }*/

    }

/*    public void updateQValueWhenDisconnected(Connection con){
        DTNHost other = con.getOtherNode(getHost());
        System.out.println(con);
        for (DTNHost state:
             states) {
            Double qValue = Qtable.get(new Pair<>(state, other));
            qValue = qValue*beta;
            Qtable.put(new Pair<>(state, other), qValue);
        }
    }*/

    /**
     * should be called when message transferred
     */
    public void updateDestinationSet(){
        destinations.clear();
        Collection<Message> msgs = getMessageCollection();
        for (Message m :
                msgs) {
            destinations.add(m.getTo());
        }
    }

    public void updateDestinationSetAfterNewMsgAdd(Message message) {
        destinations.add(message.getTo());
    }

    /**
     * initialize q-table when connection changed, put new item in q-table
     * if not exist
     */
    public void initializeQtable(){
        if (destinations.size()==0)
            return;

        for (DTNHost d:
             destinations) {
            for (DTNHost a:
                 actions) {
                if (!Qtable.containsKey(new Pair<>(d,a))){
                    Qtable.put(new Pair<>(d, a), 0.0);
                }
            }
        }
    }

    public void updateQtableTransferredDone(DTNHost other, Message m){
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        Pair<DTNHost, DTNHost> Qkey;
        DTNHost d = m.getTo();
            Qkey = new Pair<>(d,other);
            if (Qtable.containsKey(Qkey)){
                //setReward(other, d);

                learn(d, other);
            }
        }

    public void updateQtableWhenConnected(DTNHost other){
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        Pair<DTNHost, DTNHost> Qkey;
        for (DTNHost d:
             destinations) {
            Qkey = new Pair<>(d,other);
            if (Qtable.containsKey(Qkey)){
                //setReward(other, d);
                ageQValue(other);
                learn(d, other);
            }
        }

    }

    public void removeZeroFromQtable() {
        List<Pair> removeList = new ArrayList<>();
        for (Map.Entry<Pair<DTNHost,DTNHost>,Double> e: Qtable.entrySet()
             ) {
            if (e.getValue() < Q_THRESHOLD&&e.getValue()>-0.1) {
                removeList.add(e.getKey());
            }
        }
        for (int i = 0, n=removeList.size();
        i<n;i++) {
            Qtable.remove(removeList.get(i));
        }
    }


    public int setNewMsgNrofCopies(DTNHost host) {
        //TODO:calculate the number of copies
        return 0;
    }

    @Override
    protected List sortByQueueMode(List list) {
        Collections.sort(list, new Comparator(){

            @Override
            public int compare(Object o1, Object o2) {
                double diff1, diff2;
                Message m1,m2;

                if (o1 instanceof Tuple) {
                    m1 = ((Tuple<Message, Connection>)o1).getKey();
                    m2 = ((Tuple<Message, Connection>)o2).getKey();
                }
                else if (o1 instanceof Message) {
                    m1 = (Message)o1;
                    m2 = (Message)o2;
                }
                else {
                    throw new SimError("Invalid type of objects in " +
                            "the list");
                }

                diff1 = m1.getTtl()-m2.getTtl();
                diff2 = m1.getHopCount()-m2.getHopCount();
                if (diff1 == 0 && diff2 == 0) {
                    return 0;
                } else if (diff1 == 0) {
                    return (diff2 < 0 ? 1 : -1);

                } else {
                    return (diff1 < 0 ? -1 : 1);
                }
            }
        });
        return list;
    }

    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);



        List<Message> msgList = new ArrayList<Message>(getMessageCollection());
        msgList = sortByQueueMode(msgList);

        if (con.isUp()) {
            DTNHost other = con.getOtherNode(getHost());
            //update prophet probability
            updateDeliveryPredFor(other);
            updateTransitivePreds(other);

            checkMessageDelivered(other);
            //updateDestinationSet(other);
            updateActions();
            initializeQtable();

            ageActivity();

            //exchangeRENS(con);

            updateENS(con);
            //exchangeENS(con);

            //
            //initializeQtable();
            if (con.getOtherNode(getHost())!=getHost()) {
                this.nodeCount++;
                this.nodeCountIn200++;
            }
            ageActivity();
            setSocialProperty();
            //setReward(other, );
            updateQtableWhenConnected(other);
            /*else {
                learn(getHost(),other,reward);
            }*/


        }

        if (!con.isUp()){
            updateActions();
            removeZeroFromQtable();
            //ageQValue();
        }
    }




    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message msg = super.messageTransferred(id, from);
        updateDestinationSet();
        Integer nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);



        assert nrofCopies != null : "Not a SnW message: " + msg;
        //nrofCopies = (int) Math.floor(nrofCopies / 2.0);
        nrofCopies = 1;
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) from.getRouter();
        if (router.socialProperty<this.socialProperty) {
             //in binary S'n'W the receiving node gets floor(n/2) copies
            nrofCopies = (int) Math.floor(nrofCopies / 2.0);
        } else {
             //in standard S'n'W the receiving node gets only single copy
            nrofCopies = 1;
        }
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
        updateDestinationSetAfterNewMsgAdd(msg);
        initializeQtable();
        return true;
    }




    @Override
    public void update() {
        super.update();

        if (!canStartTransfer() || isTransferring()) {
            return; // nothing to transfer or is currently transferring
        }

        /* try messages that could be delivered to final recipient */
        Connection con = exchangeDeliverableMessages();
        if (null != con) {
            DeliveredMessages++;
            updateQtable(getHost());
            removeFromMessages(con.getMessage().getId());
        }

        if (actions.size()<2)
            return;
        /* create a list of SAWMessages that have copies left to distribute */
        @SuppressWarnings(value = "unchecked")
        List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());


        initializeQtable();

        if (copiesLeft.size() > 0) {
            /* try to send those messages */
            for (Message m : copiesLeft) {
                DTNHost dest = m.getTo();

                Connection connection = chooseAction(dest);
                if (connection!=null){
                    int retVal = startTransfer(m, connection);
/*                    if (retVal == RCV_OK) {
                        return;
                    }*/
                }
            }
        }
    }

    /**
     * check the message in buffer is delivered message to connected node
     * if so, delete message from buffer
     * @param other
     */
    private void checkMessageDelivered(DTNHost other) {
        Collection<Message> msgs = new ArrayList<>(getMessageCollection());

/*            Iterator it = msgs.iterator();
            while (it.hasNext()) {
                Message m = (Message) it.next();
                if (act.getRouter().isDeliveredMessage(m)) {
                    deleteMessage(m.getId(), false);
                }
            }*/
            for (Message m : msgs
            ) {
                if (other.getRouter().isDeliveredMessage(m)) {
                    deleteMessage(m.getId(), false);
                }
            }
    }


   /* @Override
    protected Connection tryAllMessagesToAllConnections() {
        return super.tryAllMessagesToAllConnections();
    }

    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

		*//* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host *//*
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            QLearningSprayAndWaitRouter othRouter = (QLearningSprayAndWaitRouter) other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
                    // the other node has higher probability of delivery
                    messages.add(new Tuple<Message, Connection>(m, con));
                }
            }
        }
        if (messages.size() == 0) {
            return null;
        }

        // sort the message-connection tuples
        Collections.sort(messages, new ProphetRouter.TupleComparator());
        return tryMessagesForConnected(messages);	// try to send messages
    }*/
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
        nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROPERTY);
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) con.getOtherNode(getHost()).getRouter();
        if (router.socialProperty > this.socialProperty) {
            nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        } else {
            nrofCopies--;
        }

        /* reduce the amount of copies left */

        //

        msg.updateProperty(MSG_COUNT_PROPERTY, nrofCopies);

        updateQtableAfterTrans(con);
    }

    private void updateQtableAfterTrans(Connection con) {
        DTNHost other = con.getOtherNode(getHost());
        QLearningSprayAndWaitRouter router = (QLearningSprayAndWaitRouter) other.getRouter();
        for (Map.Entry<Pair<DTNHost, DTNHost>, Double> e : router.getQ_table().entrySet()) {
            if (this.Qtable.containsKey(e.getKey())) {
                if (e.getValue() > Qtable.get(e.getKey())) {
                    Qtable.put(e.getKey(), e.getValue());
                } else if (e.getValue() < Qtable.get(e.getKey())) {
                    e.setValue(Qtable.get(e.getKey()));
                }
            } else {
                Qtable.put(e.getKey(), e.getValue());
            }
        }
    }

    @Override
    public QLearningSprayAndWaitRouter replicate() {
        return new QLearningSprayAndWaitRouter(this);
    }
}
