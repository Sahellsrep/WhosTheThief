package com.project.game1;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiplayerManager {

    public static final int PORT = 8888;
    public static final int DISCOVERY_PORT = 8889;

    // Set MIN_PLAYERS to 2 temporarily if you only have two phones to test with
    public static final int MIN_PLAYERS = 4;
    public static final int MAX_PLAYERS = 13;

    public interface Listener {
        void onMessage(String from, String msg);
    }

    public interface JoinCallback {
        void onResult(boolean success, String error);
    }

    public interface RoomCallback {
        void onRoom(Room room);
    }

    public static class Room {
        public String ip;
        public String hostName;
        public int joined;
        public int max;
        public long lastSeen;
    }

    private static class Conn {
        String name;
        Socket socket;
        PrintWriter out;
    }

    private static MultiplayerManager instance;

    public static synchronized MultiplayerManager get() {
        if (instance == null) {
            instance = new MultiplayerManager();
        }
        return instance;
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private ExecutorService sender = Executors.newSingleThreadExecutor();
    private volatile int session = 0;

    public boolean isHost = false;
    public String myName = "";
    public volatile int maxPlayers = MIN_PLAYERS;
    public volatile boolean gameStarted = false;

    // Roles chosen by the host (one entry per player)
    public ArrayList<String> roleNames = new ArrayList<>();
    public ArrayList<Integer> rolePoints = new ArrayList<>();

    private final ArrayList<String> playerNames = new ArrayList<>();
    private final ArrayList<Boolean> readyFlags = new ArrayList<>();
    private final ArrayList<Conn> conns = new ArrayList<>();
    private ServerSocket server;
    private Socket clientSocket;
    private PrintWriter clientOut;
    private volatile boolean closed = true;

    private Listener listener;
    private final ArrayList<String[]> pending = new ArrayList<>();

    // ---------- helpers ----------

    public static String clean(String s) {
        if (s == null) return "";
        return s.replace("|", "").replace("~", "").trim();
    }

    public static String joinNames(ArrayList<String> list) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) b.append("|");
            b.append(list.get(i));
        }
        return b.toString();
    }

    // ---------- players / lobby ----------

    public synchronized ArrayList<String> getPlayers() {
        return new ArrayList<>(playerNames);
    }

    public synchronized boolean isReady(String name) {
        int i = playerNames.indexOf(name);
        return i >= 0 && readyFlags.get(i);
    }

    public synchronized boolean allReady() {
        for (boolean r : readyFlags) {
            if (!r) return false;
        }
        return true;
    }

    private synchronized void setReady(String name, boolean ready) {
        int i = playerNames.indexOf(name);
        if (i >= 0) readyFlags.set(i, ready);
    }

    public synchronized void markStarted() {
        gameStarted = true;
    }

    private synchronized String lobbyString() {
        StringBuilder b = new StringBuilder("LOBBY|").append(maxPlayers);
        for (int i = 0; i < playerNames.size(); i++) {
            b.append("|").append(playerNames.get(i)).append("~")
                    .append(readyFlags.get(i) ? "1" : "0");
        }
        return b.toString();
    }

    private void broadcastLobby() {
        broadcast(lobbyString());
    }

    private synchronized void parseLobby(String payload) {
        String[] parts = payload.split("\\|");

        try {
            maxPlayers = Integer.parseInt(parts[0]);
        } catch (NumberFormatException ignored) {
        }

        playerNames.clear();
        readyFlags.clear();

        for (int i = 1; i < parts.length; i++) {
            String[] e = parts[i].split("~");
            if (e.length < 2) continue;
            playerNames.add(e[0]);
            readyFlags.add(e[1].equals("1"));
        }
    }

    private synchronized void parseStart(String payload) {
        String[] parts = payload.split("\\|");

        playerNames.clear();
        readyFlags.clear();

        for (String p : parts) {
            if (p.isEmpty()) continue;
            playerNames.add(p);
            readyFlags.add(true);
        }
    }

    // ---------- listener (main thread only) ----------

    public void setListener(Listener l) {
        listener = l;
        if (l == null) return;

        ArrayList<String[]> copy = new ArrayList<>(pending);
        pending.clear();

        for (String[] p : copy) {
            if (listener == null) {
                pending.add(p);
            } else {
                listener.onMessage(p[0], p[1]);
            }
        }
    }

    public void clearListener(Listener l) {
        if (listener == l) listener = null;
    }

    private void deliver(final String from, final String msg) {
        main.post(() -> {
            if (listener != null) {
                listener.onMessage(from, msg);
            } else {
                pending.add(new String[]{from, msg});
            }
        });
    }

    // ---------- start / stop ----------

    private synchronized void resetState() {
        closeInternal();
        isHost = false;
        gameStarted = false;
        playerNames.clear();
        readyFlags.clear();
        pending.clear();
        sender = Executors.newSingleThreadExecutor();
        closed = false;
    }

    public synchronized void close() {
        closeInternal();
    }

    private void closeInternal() {
        closed = true;
        session++;

        final ServerSocket ss = server;
        final Socket cl = clientSocket;
        final ArrayList<Conn> cs = new ArrayList<>(conns);

        server = null;
        clientSocket = null;
        clientOut = null;
        conns.clear();

        Runnable r = () -> {
            try { if (ss != null) ss.close(); } catch (IOException ignored) { }
            try { if (cl != null) cl.close(); } catch (IOException ignored) { }
            for (Conn c : cs) {
                try { c.socket.close(); } catch (IOException ignored) { }
            }
        };

        try {
            sender.execute(r);
            sender.shutdown();
        } catch (RuntimeException e) {
            new Thread(r).start();
        }
    }

    // ---------- host ----------

    public void startHost(final String name, final int max, final JoinCallback cb) {
        resetState();
        isHost = true;
        myName = clean(name);
        maxPlayers = max;

        final int mySession = session;

        synchronized (this) {
            playerNames.add(myName);
            readyFlags.add(true);
        }

        new Thread(() -> {
            ServerSocket ss;

            try {
                ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(PORT));
            } catch (IOException e) {
                main.post(() -> cb.onResult(false, "Could not start the room. Please try again."));
                return;
            }

            if (session != mySession) {
                try { ss.close(); } catch (IOException ignored) { }
                return;
            }

            synchronized (this) {
                server = ss;
            }

            main.post(() -> cb.onResult(true, null));

            startAdvertising(mySession);
            acceptLoop(ss, mySession);
        }).start();
    }

    private void acceptLoop(ServerSocket ss, int mySession) {
        while (session == mySession && !ss.isClosed()) {
            try {
                final Socket s = ss.accept();
                new Thread(() -> handleClient(s)).start();
            } catch (IOException e) {
                break;
            }
        }
    }

    // Broadcasts the room on the local network so other phones can find it
    private void startAdvertising(final int mySession) {
        new Thread(() -> {
            DatagramSocket ds = null;

            try {
                ds = new DatagramSocket();
                ds.setBroadcast(true);

                while (session == mySession && !gameStarted) {

                    String msg = "WTT1|" + myName + "|" + getPlayers().size() + "|" + maxPlayers;
                    byte[] data = msg.getBytes("UTF-8");

                    for (InetAddress addr : getBroadcastAddresses()) {
                        try {
                            ds.send(new DatagramPacket(data, data.length, addr, DISCOVERY_PORT));
                        } catch (IOException ignored) {
                        }
                    }

                    Thread.sleep(1000);
                }
            } catch (Exception ignored) {
            } finally {
                if (ds != null) ds.close();
            }
        }).start();
    }

    private static ArrayList<InetAddress> getBroadcastAddresses() {
        ArrayList<InetAddress> list = new ArrayList<>();

        try {
            list.add(InetAddress.getByName("255.255.255.255"));
        } catch (IOException ignored) {
        }

        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();

            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();

                if (!ni.isUp() || ni.isLoopback()) continue;

                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress b = ia.getBroadcast();
                    if (b != null && !list.contains(b)) list.add(b);
                }
            }
        } catch (Exception ignored) {
        }

        return list;
    }

    private void handleClient(Socket s) {
        Conn conn = null;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);

            String first = in.readLine();

            if (first == null || !first.startsWith("JOIN|")) {
                s.close();
                return;
            }

            String name = clean(first.substring(5));
            String error = null;

            synchronized (this) {
                if (gameStarted) {
                    error = "The game has already started";
                } else if (playerNames.size() >= maxPlayers) {
                    error = "The room is full";
                } else if (name.isEmpty()) {
                    error = "Invalid username";
                } else {
                    for (String p : playerNames) {
                        if (p.equalsIgnoreCase(name)) {
                            error = "That username is already in the room";
                        }
                    }
                }

                if (error == null) {
                    conn = new Conn();
                    conn.name = name;
                    conn.socket = s;
                    conn.out = out;
                    conns.add(conn);
                    playerNames.add(name);
                    readyFlags.add(false);
                }
            }

            if (error != null) {
                out.println("ERROR|" + error);
                s.close();
                return;
            }

            out.println("OK");
            broadcastLobby();
            deliver("system", "LOBBY");

            String line;
            while ((line = in.readLine()) != null) {

                if (line.startsWith("READY|")) {
                    setReady(name, line.endsWith("1"));
                    broadcastLobby();
                    deliver("system", "LOBBY");
                } else {
                    deliver(name, line);
                }
            }

        } catch (IOException ignored) {
        } finally {
            if (conn != null) removeConn(conn);
            try { s.close(); } catch (IOException ignored) { }
        }
    }

    private void removeConn(Conn c) {
        if (closed) return;

        boolean started;

        synchronized (this) {
            conns.remove(c);
            started = gameStarted;

            if (!gameStarted) {
                int i = playerNames.indexOf(c.name);
                if (i >= 0) {
                    playerNames.remove(i);
                    readyFlags.remove(i);
                }
            }
        }

        if (!started) {
            broadcastLobby();
        }

        deliver("system", "LEFT|" + c.name);
    }

    // ---------- client ----------

    public void join(final String ip, final String name, final JoinCallback cb) {
        resetState();
        isHost = false;
        myName = clean(name);

        new Thread(() -> {
            final boolean[] joined = {false};
            Socket s = null;

            try {
                s = new Socket();
                s.connect(new InetSocketAddress(ip, PORT), 5000);

                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);

                out.println("JOIN|" + myName);

                String resp = in.readLine();

                if (resp == null || !resp.equals("OK")) {
                    final String err = (resp != null && resp.startsWith("ERROR|"))
                            ? resp.substring(6)
                            : "Could not join the room";
                    s.close();
                    main.post(() -> cb.onResult(false, err));
                    return;
                }

                synchronized (this) {
                    clientSocket = s;
                    clientOut = out;
                }

                joined[0] = true;
                main.post(() -> cb.onResult(true, null));

                String line;
                while ((line = in.readLine()) != null) {
                    handleClientLine(line);
                }

            } catch (Exception e) {
                if (!joined[0]) {
                    main.post(() -> cb.onResult(false,
                            "Could not connect. Make sure you are on the same Wi-Fi or hotspot."));
                }
            } finally {
                if (joined[0] && !closed) {
                    deliver("system", "DISCONNECTED");
                }
            }
        }).start();
    }

    private void handleClientLine(String line) {
        if (line.startsWith("LOBBY|")) {
            parseLobby(line.substring(6));
            deliver("host", "LOBBY");
        } else if (line.startsWith("START|")) {
            parseStart(line.substring(6));
            deliver("host", line);
        } else {
            deliver("host", line);
        }
    }

    // ---------- sending ----------

    public void broadcast(final String msg) {
        final ArrayList<Conn> copy;
        synchronized (this) {
            copy = new ArrayList<>(conns);
        }
        sendAsync(() -> {
            for (Conn c : copy) c.out.println(msg);
        });
    }

    public void sendTo(String name, final String msg) {
        Conn target = null;
        synchronized (this) {
            for (Conn c : conns) {
                if (c.name.equals(name)) target = c;
            }
        }
        if (target == null) return;

        final Conn t = target;
        sendAsync(() -> t.out.println(msg));
    }

    public void sendToHost(final String msg) {
        final PrintWriter o;
        synchronized (this) {
            o = clientOut;
        }
        if (o == null) return;

        sendAsync(() -> o.println(msg));
    }

    private void sendAsync(Runnable r) {
        try {
            sender.execute(r);
        } catch (RuntimeException ignored) {
        }
    }

    // ---------- room discovery (for the Join screen) ----------

    public static class Discovery {

        private volatile boolean running = false;
        private DatagramSocket socket;
        private final Handler handler = new Handler(Looper.getMainLooper());

        public void start(final RoomCallback cb) {
            running = true;

            new Thread(() -> {
                DatagramSocket ds = null;

                try {
                    ds = new DatagramSocket(null);
                    ds.setReuseAddress(true);
                    ds.setBroadcast(true);
                    ds.bind(new InetSocketAddress(DISCOVERY_PORT));
                    ds.setSoTimeout(1000);

                    synchronized (this) {
                        socket = ds;
                    }

                    byte[] buf = new byte[512];

                    while (running) {

                        DatagramPacket p = new DatagramPacket(buf, buf.length);

                        try {
                            ds.receive(p);
                        } catch (SocketTimeoutException e) {
                            continue;
                        }

                        String msg = new String(p.getData(), 0, p.getLength(), "UTF-8");
                        String[] parts = msg.split("\\|");

                        if (parts.length < 4 || !parts[0].equals("WTT1")) continue;

                        final Room r = new Room();

                        try {
                            r.ip = p.getAddress().getHostAddress();
                            r.hostName = parts[1];
                            r.joined = Integer.parseInt(parts[2]);
                            r.max = Integer.parseInt(parts[3]);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        handler.post(() -> {
                            if (running) cb.onRoom(r);
                        });
                    }
                } catch (Exception ignored) {
                } finally {
                    if (ds != null) ds.close();
                }
            }).start();
        }

        public void stop() {
            running = false;

            synchronized (this) {
                if (socket != null) socket.close();
            }
        }
    }

    // ---------- network info ----------

    public static String getLocalIp() {
        String fallback = null;

        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();

            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();

                if (!ni.isUp() || ni.isLoopback()) continue;

                String niName = ni.getName().toLowerCase();

                Enumeration<InetAddress> addrs = ni.getInetAddresses();

                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();

                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {

                        if (niName.contains("wlan") || niName.startsWith("ap")) {
                            return a.getHostAddress();
                        }

                        if (fallback == null) fallback = a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }
}