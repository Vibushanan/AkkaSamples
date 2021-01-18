package org.vinu.racesimulator;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.vinu.racesimulator.VinuRacer.raceCommand;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class VinuRaceController extends AbstractBehavior<VinuRaceController.Command> {

    Map<ActorRef<VinuRacer.Command>, Double> currentPositions = new HashMap<ActorRef<VinuRacer.Command>, Double>();
    Map<ActorRef<VinuRacer.Command>, Long> results = new HashMap<ActorRef<VinuRacer.Command>, Long>();

    private Object KEY;
    long start;

    public interface Command extends Serializable {
    }

    public static class startRace implements Command {
        private static final long serialVersionUID = 1L;
    }

    private class askForStats implements Command {
        private static final long serialVersionUID = 1L;
    }

    public static class raceCompletedMessage implements Command {
        private static final long serialVersionUID = 1L;
        private ActorRef<VinuRacer.Command> racer;
        private long runtime;

        public raceCompletedMessage(ActorRef<org.vinu.racesimulator.VinuRacer.Command> racer, long runtime) {
            this.racer = racer;
            this.runtime = runtime;
        }

        public ActorRef<VinuRacer.Command> getRacer() {
            return racer;
        }

        public long getRuntime() {
            return runtime;
        }
    }

    public static class raceStats implements Command {
        private static final long serialVersionUID = 1L;
        private double currentPosition;
        private ActorRef<VinuRacer.Command> from;

        public double getCurrentPosition() {
            return currentPosition;
        }

        public ActorRef<VinuRacer.Command> getFrom() {
            return from;
        }

        public raceStats(double currentPosition, ActorRef<org.vinu.racesimulator.VinuRacer.Command> from) {
            this.currentPosition = currentPosition;
            this.from = from;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(VinuRaceController::new);
    }

    private VinuRaceController(ActorContext<Command> context) {
        super(context);
    }

    private void displayRace() {
        int displayLength = 100;
        // System.out.println("currentPositions : " + currentPositions);
        for (int i = 0; i < 10; ++i)
            System.out.println();

        System.out.println("Race has been running for " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");
        System.out.println("    " + new String(new char[displayLength]).replace('\0', '='));
        int i = 0;
        for (ActorRef<VinuRacer.Command> racer : currentPositions.keySet()) {
            System.out
                    .println(i + " : " + new String(new char[(int) (currentPositions.get(racer) * displayLength / 100)])
                            .replace('\0', '*'));
            i++;
        }
    }

   

    @Override
    public Receive<Command> createReceive() {

        return newReceiveBuilder()

                .onMessage(startRace.class, msg -> {

                    getContext().getLog().info("Starting Race........");
                    start = System.currentTimeMillis();
                    for (int i = 0; i < 10; i++) {
                        getContext().getLog().info("Creating Racer " + i);
                        ActorRef<VinuRacer.Command> racerRef = getContext().spawn(VinuRacer.create(), "Racer" + i);
                        racerRef.tell(new raceCommand(50));
                        currentPositions.put(racerRef, 0.0);
                    }
                    return Behaviors.withTimers(timer -> {

                        getContext().getLog().info("Starting Timer...");
                        timer.startTimerAtFixedRate(KEY, new VinuRaceController.askForStats(), Duration.ofSeconds(1));

                        return this;
                    });
                })

                .onMessage(askForStats.class, msg -> {
                    Set<ActorRef<org.vinu.racesimulator.VinuRacer.Command>> allRefd = currentPositions.keySet();
                    for (ActorRef<VinuRacer.Command> ref : allRefd) {
                        ref.tell(new VinuRacer.raceStat(getContext().getSelf()));
                        //displayRace();
                    }
                    return Behaviors.same();
                })

                .onMessage(raceStats.class, stat -> {
                    currentPositions.put(stat.from, stat.currentPosition);
                    // System.out.println("currentPositions => " + currentPositions);
                    /*
                     * int sum = 0; for (ActorRef<VinuRacer.Command> ref :
                     * currentPositions.keySet()) {
                     * 
                     * sum += currentPositions.get(ref); }
                     * 
                     * if (sum / 10 == 50.0) { System.out.println("Avg =" + sum / 50.0);
                     * System.out.println("All Completed.........."); return Behaviors.same(); }
                     */

                    return this;
                })

                .onMessage(raceCompletedMessage.class, completed -> {

                    results.put(completed.getRacer(), completed.getRuntime());
                    if (results.size() == 10) {

                        getContext().getLog().info("All Recer Completed ....");
                        
                        results.keySet().stream().sorted().forEach(key -> {
                            System.out.println(key.path()+" ----- "+results.get(key));
                        });

                        
                        return Behaviors.stopped();
                    } else {
                        return Behaviors.same();
                    }

                })

                .build();
    }

}
