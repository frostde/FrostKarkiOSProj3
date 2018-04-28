import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;

/**
 * Created by danielfrost on 4/26/18.
 */
public class Controller implements Runnable {
    private int indexOfInitiator;
    private int indexOfFaulty1;
    private int indexOfFaulty2;
    private int numberOfFaulty;
    private int numRounds;
    private static Map<Integer, Semaphore> semaphores;
    private Vector<Process> processVector;
    private Process[] processes;
    private Map<Integer, Integer> ports;

    Controller() {

    }



    public void run() {
        processVector = new Vector<>();
        semaphores = new HashMap<>();
        ports = new HashMap<>();
        semaphores.put(0, new Semaphore(1));
        semaphores.put(1, new Semaphore(1));
        semaphores.put(2, new Semaphore(1));
        semaphores.put(3, new Semaphore(1));
        semaphores.put(4, new Semaphore(1));
        semaphores.put(5, new Semaphore(1));
        semaphores.put(6, new Semaphore(1));

        ports.put(0, 4045);
        ports.put(1, 4046);
        ports.put(2, 4047);
        ports.put(3, 4048);
        ports.put(4, 4049);
        ports.put(5, 4050);
        ports.put(6, 4051);

        Random rand = new Random();
        int numberOfFaulty = rand.nextInt(2) + 1;
        int indexOfInitiator = rand.nextInt(7);
        int indexOfFaulty1 = -1;
        int indexOfFaulty2 = -1;
        processes = new Process[7];
        if (numberOfFaulty == 1) {
            indexOfFaulty1 = rand.nextInt(7);
            numRounds = 2;
        } else {
            indexOfFaulty1 = rand.nextInt(7);
            do {
                indexOfFaulty2 = rand.nextInt(7);
            } while (indexOfFaulty2 == indexOfFaulty1);
            numRounds = 3;
        }
        System.out.println(numRounds + " " + numberOfFaulty);

        System.out.println("Algorithm starting with these 7 proccesses:");
        for (int i = 0; i < 7; i++) {
            Process m;
            if (i == indexOfInitiator) {
                if (i == indexOfFaulty1 || i == indexOfFaulty2) {
                    m = new Process(true, i, true, this);
                } else {
                    m = new Process(false, i, true,  this);
                }
            } else {
                if (i == indexOfFaulty1 || i == indexOfFaulty2) {
                    m = new Process(true, i, false,  this);
                } else {
                    m = new Process(false, i, false,  this);
                }
            }
            processes[i] = m;
        }
        try {
            for (Process p : processes) {
                Thread t = new Thread(p);
                t.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (processVector.size() < 7) { }
        processVector.clear();
        System.out.println("\n\nROUND 0 ----");
        processes[indexOfInitiator].initiate();
        while (processVector.size() < 1) { }
        for (Process p : processes) {
            p.round0();
        }
        while (processVector.size() < 6);
        for (int i = 1; i <= numRounds; i++) {
            System.out.println("\n\nROUND " + i);
            round();

        }
        processVector.clear();
        printFinalValues();
    }

    public void processfinished(Process p) {
        processVector.add(p);
    }

    public int getProcessPort(int i) {
        return ports.get(i);
    }

    public Semaphore getProcessSemaphore(int i) {
        return semaphores.get(i);
    }

    public void round() {
        try {
            processVector.clear();
            for (Process p : processes) {
                p.sendMsg();
            }
            while (processVector.size() < 6) {
            }
            processVector.clear();
            for (Process p : processes) {
                p.receiveMsg();
            }
            while (processVector.size() < 6) {
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void printFinalValues() {
        for (Process p : processes) {
            p.print();
        }
    }



}
