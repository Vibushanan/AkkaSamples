package org.vinu.BigPrime;

import org.vinu.BigPrime.ManagerBehavior.Command;

import akka.actor.typed.ActorSystem;

public class Main {

    public static void main(String[] args) {
        ActorSystem.create(ManagerBehavior.create(), "PrimeSimulation").tell(new ManagerBehavior.CalculatePrimeNumbers());
    }
    
}
