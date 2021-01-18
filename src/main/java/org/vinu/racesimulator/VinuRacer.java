package org.vinu.racesimulator;

import java.io.Serializable;
import java.util.Random;

import org.vinu.racesimulator.VinuRaceController.raceCompletedMessage;
import org.vinu.racesimulator.VinuRaceController.raceStats;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class VinuRacer extends AbstractBehavior<VinuRacer.Command> {

    private double currentPosition = 0;
    private int raceLength;
    private final double defaultAverageSpeed = 48.2;
    private int averageSpeedAdjustmentFactor;
    private Random random;
    private double currentSpeed = 0;
    private long starttime;

    public interface Command extends Serializable {
    }

    public static class raceCommand implements Command {
        private static final long serialVersionUID = 1L;
        private int raceLength;

        public int getRaceLength() {
            return raceLength;
        }

        public raceCommand(int raceLength) {
            this.raceLength = raceLength;
        }

    }

    public static class raceStat implements Command {
        private static final long serialVersionUID = 1L;
        private ActorRef<VinuRaceController.Command> from;

        public ActorRef<VinuRaceController.Command> getFrom() {
            return from;
        }

        public raceStat(ActorRef<VinuRaceController.Command> from) {
            this.from = from;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(VinuRacer::new);
    }

    private VinuRacer(ActorContext<Command> context) {
        super(context);
    }

    private double getMaxSpeed() {
        return defaultAverageSpeed * (1 + ((double) averageSpeedAdjustmentFactor / 100));
    }

    private double getDistanceMovedPerSecond() {
        return currentSpeed * 1000 / 3600;
    }

    private void determineNextSpeed() {
        if (currentPosition < (raceLength / 4)) {
            currentSpeed = currentSpeed + (((getMaxSpeed() - currentSpeed) / 10) * random.nextDouble());
        } else {
            currentSpeed = currentSpeed * (0.5 + random.nextDouble());
        }

        if (currentSpeed > getMaxSpeed())
            currentSpeed = getMaxSpeed();

        if (currentSpeed < 5)
            currentSpeed = 5;

        if (currentPosition > (raceLength / 2) && currentSpeed < getMaxSpeed() / 2) {
            currentSpeed = getMaxSpeed() / 2;
        }
    }

    @Override
    public Receive<Command> createReceive() {

        return onRaceStartHandler();
    }

    public Receive<Command> onRaceStartHandler() {
        return newReceiveBuilder().onMessage(raceCommand.class, cmd -> {
            this.raceLength = cmd.raceLength;
            random = new Random();
            this.averageSpeedAdjustmentFactor = random.nextInt(30) - 10;
            starttime = System.currentTimeMillis();
            return onRaceRunningHandler();
        }).onMessage(raceStat.class, stat -> {
            stat.from.tell(new raceStats(currentPosition, getContext().getSelf()));
            return this;
        }).build();
    }

    public Receive<Command> onRaceRunningHandler() {
        return newReceiveBuilder().onMessage(raceStat.class, cmd -> {
            determineNextSpeed();
            currentPosition += getDistanceMovedPerSecond();
            if (currentPosition > raceLength) {
                currentPosition = raceLength;
            }

            cmd.from.tell(new raceStats(currentPosition, getContext().getSelf()));

            if (currentPosition == raceLength) {
                getContext().getLog().info("Completed :" + getContext().getSelf().path());
                cmd.from.tell(new raceCompletedMessage(getContext().getSelf(), System.currentTimeMillis() - starttime));
                return Behaviors.ignore();
            } else {
                // getContext().getLog().info("Not Completed : "+getContext().getSelf().path());
                return Behaviors.same();
            }

        }).build();
    }

    public Receive<Command> onRaceCompleted() {
        return newReceiveBuilder().onMessage(raceStat.class, stat -> {
            getContext().getLog().info("Am Already Done.............." + getContext().getSelf().path());
            stat.from.tell(new raceStats(currentPosition, getContext().getSelf()));
            stat.from.tell(new raceCompletedMessage(getContext().getSelf(), System.currentTimeMillis() - starttime));
            return Behaviors.ignore();
        })

                .build();
    }

}
