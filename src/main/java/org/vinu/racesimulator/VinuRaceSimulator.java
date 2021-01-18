package org.vinu.racesimulator;

import org.vinu.racesimulator.VinuRaceController.Command;

import akka.actor.typed.ActorSystem;

public class VinuRaceSimulator {

    public static void main(String[] args) {

        ActorSystem.create(VinuRaceController.create(), "Simulator").tell(new VinuRaceController.startRace());

      
    }
    
}
