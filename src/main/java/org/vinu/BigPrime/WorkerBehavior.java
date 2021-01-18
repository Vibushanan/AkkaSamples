package org.vinu.BigPrime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Random;

import org.vinu.BigPrime.ManagerBehavior.BigPrimeResults;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class WorkerBehavior extends AbstractBehavior<WorkerBehavior.Command> {

    public interface Command extends Serializable {
    }

    public static class calculatePrime implements Command {
        private static final long serialVersionUID = 1L;
        private ActorRef<ManagerBehavior.Command> from;

        public calculatePrime(ActorRef<org.vinu.BigPrime.ManagerBehavior.Command> from) {
            this.from = from;
        }

        public ActorRef<ManagerBehavior.Command> getFrom() {
            return from;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(WorkerBehavior::new);
    }

    private WorkerBehavior(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return beforehavingPrime();
    }

    public Receive<Command> beforehavingPrime() {

        return newReceiveBuilder().onMessage(calculatePrime.class, cmd -> {
            BigInteger bi = new BigInteger(2000, new Random());
            cmd.from.tell(new BigPrimeResults(bi, getContext().getSelf()));
            return afterhavingPrime(bi);
        }).build();
    }

    public Receive<Command> afterhavingPrime(BigInteger prime) {

        return newReceiveBuilder().onMessage(calculatePrime.class, cmd -> {
            cmd.from.tell(new BigPrimeResults(prime, getContext().getSelf()));
            return this;

        })

        .build();
    }
}
