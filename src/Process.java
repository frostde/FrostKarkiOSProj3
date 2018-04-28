import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Semaphore;


public class Process implements Runnable {
    private boolean initiator;
    private int name;
    private boolean faulty;
    private Random rand = new Random();
    private Map<Integer, Socket> sockets;
    private int indexOfInitiator;
    private static Controller controller;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int[] values;

    Process(boolean Faulty, int Name, boolean Initiator,  Controller c ) {
        this.faulty = Faulty;
        this.name = Name;
        this.initiator = Initiator;
        sockets = new HashMap<>();
        controller = c;
        values = new int[7];
    }

    public void run() {
        if (initiator) {
            if (faulty) {
                System.out.println("Process " +name + " (initiator + faulty)");
            } else {
                System.out.println( "Process " +name + " (initiator)");
            }
        } else {
            if (faulty) {
                System.out.println("Process " + name + " (faulty)");
            } else {
                System.out.println("Process " +name);
            }
        }
        setupSockets();
    }

    public void initiate() {
        try {
            BufferedWriter writer;
            if (initiator) {
                Semaphore processSemaphore = controller.getProcessSemaphore(name);
                processSemaphore.acquire();
                values[name] = rand.nextInt(2);
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    if (faulty) {
                        values[name] = rand.nextInt(2);
                        writer.write(values[name] + "\n");
                    } else {
                        writer.write(values[name]+ "\n");
                    }
                    writer.flush();
                    System.out.println("Process " + name + " is sending " + values[name] + " to Process " + entry.getKey());
                }
                controller.processfinished(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void round0() {
        try {
            if (!initiator) {
                BufferedReader reader;
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    if (controller.getProcessSemaphore(entry.getKey()).availablePermits() == 0) {
                        indexOfInitiator = entry.getKey();
                        reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                        values[indexOfInitiator] = Integer.parseInt(reader.readLine());
                        if (faulty) values[name] = (values[indexOfInitiator] == 0) ? 1 : 0;
                        else values[name] = values[indexOfInitiator];
                    }
                }
                controller.processfinished(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMsg() throws IOException {
        if (!initiator) {
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != indexOfInitiator && entry.getKey() != name) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    writer.write(values[name] + "\n");
                    writer.flush();
                    System.out.println("Process " + name + " is sending " + values[name] + " to Process " + entry.getKey());
                }
            }
            System.out.println("\n");
            controller.processfinished(this);
        }
    }

    public void receiveMsg() throws IOException {
        if (!initiator) {
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != indexOfInitiator) {
                    reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                    values[entry.getKey()] = Integer.parseInt(reader.readLine());
                    System.out.println("Process " + name + " received a value of " + values[entry.getKey()] + " from Process " + entry.getKey());
                }
            }
            System.out.println("\nProcess " + name + " thinks the majority is " + majorityVote() + "\n");
            values[name] = majorityVote();
            if (faulty) {
                boolean flip = rand.nextBoolean();
                if (flip) values[name] = (values[name] == 0) ? 1 : 0;
            }
            controller.processfinished(this);
        }
    }

    private int majorityVote() {
        Map<Integer, Integer> map = new HashMap<>();
        ArrayList<Integer> receivedValues = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (i != indexOfInitiator) {
                receivedValues.add(values[i]);
            }
        }
        for (Integer i : receivedValues) {
            Integer count = map.get(i);
            map.put(i, count != null ? count+1 : 0);
        }
        Integer popular = Collections.max(map.entrySet(),
                new Comparator<Map.Entry<Integer, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                        return o1.getValue().compareTo(o2.getValue());
                    }
                }).getKey();
        return popular;
    }

    public int getValue() {
        return values[name];
    }

    public void print()
    {
        if (initiator) {
            if (faulty) {
                System.out.println("Process " +name + " (initiator + faulty)    " + values[name]);
            } else {
                System.out.println( "Process " +name + " (initiator)    "  + values[name]);
            }
        } else {
            if (faulty) {
                System.out.println("Process " + name + " (faulty)    " + values[name]);
            } else {
                System.out.println("Process " +name + "    " + values[name]);
            }
        }
    }

    public int getName() {
        return name;
    }


    /*Private methods to establish 'distributed system' connections between this threads and the others threads*/
    private void setupSockets() {
        try {
            ServerSocket server = new ServerSocket(controller.getProcessPort(name));
            switch (name) {
                case 0: {
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 1: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    while (sockets.size() < 6 ) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 2: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 3: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 4: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 5: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    s = new Socket("localhost", controller.getProcessPort(4));
                    identifySelf(s);
                    sockets.put(4, s);
                    acknowledgeConnection(server);
                    break;
                }
                case 6: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    s = new Socket("localhost", controller.getProcessPort(4));
                    identifySelf(s);
                    sockets.put(4, s);
                    s = new Socket("localhost", controller.getProcessPort(5));
                    identifySelf(s);
                    sockets.put(5, s);
                    break;
                }
                default: {
                    System.out.println("defaulting");
                }
            }
            controller.processfinished(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void acknowledgeConnection(ServerSocket server) {
        try {
            Socket s = server.accept();
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String processName = reader.readLine();
            sockets.put(Integer.parseInt(processName), s);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void identifySelf(Socket s) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            writer.write(name + "\n");
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
