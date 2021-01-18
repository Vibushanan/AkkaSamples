package org.vinu.BigPrime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vinu.BigPrime.WorkerBehavior.calculatePrime;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import scala.util.Using.Manager;

public class ManagerBehavior extends AbstractBehavior<ManagerBehavior.Command> {
    int count = 0;
    Set<BigInteger> results = new HashSet<BigInteger>();
    List<ActorRef<WorkerBehavior.Command>> all_ref = new ArrayList<>();

    public interface Command extends Serializable {
    }

    public static class CalculatePrimeNumbers implements Command {
        private static final long serialVersionUID = 1L;
    }

    public static class BigPrimeResults implements Command {
        private static final long serialVersionUID = 1L;
        private BigInteger prime;
        private ActorRef<WorkerBehavior.Command> from;

        public BigInteger getPrime() {
            return prime;
        }

        public ActorRef<WorkerBehavior.Command> getFrom() {
            return from;
        }

        public BigPrimeResults(BigInteger prime, ActorRef<org.vinu.BigPrime.WorkerBehavior.Command> from) {
            this.prime = prime;
            this.from = from;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ManagerBehavior::new);
    }

    private ManagerBehavior(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().onMessage(CalculatePrimeNumbers.class, msg -> {

            for (int i = 0; i < 10; i++) {

                ActorRef<WorkerBehavior.Command> ref = getContext().spawn(WorkerBehavior.create(), "Worker-" + i);
                all_ref.add(ref);
                ref.tell(new calculatePrime(getContext().getSelf()));

            }

            return this;
        })

                .onMessage(BigPrimeResults.class, result -> {
                    results.add(result.getPrime());
                    if (results.size() == 10) {

                        System.out.println("All PRIMES ==> (" + count + ")" + results);
                        return Behaviors.stopped();

                    }

                    return Behaviors.same();
                })

                .build();
    }

}
