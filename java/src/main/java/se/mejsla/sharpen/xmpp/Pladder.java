package se.mejsla.sharpen.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Random;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
// https://www.baeldung.com/xmpp-smack-chat-client
//
public class Pladder {

    private static final String XMPP_DOMAIN = "cluster.zoom.nu";
    private static final String XMPP_HOST = "cluster.zoom.nu";

    private final String MY_JID = "bot09";
    private final String MY_PWD = "bot";

    private final Pattern MY_HELLO_PATTERN = Pattern.compile("^hej.*");
    private final Pattern MY_START_GAME_PATTERN = Pattern.compile("^spela$");
    private final Pattern MY_PLAY_WITH_PATTERN = Pattern.compile("^spela med (.*)");
    private final Pattern MY_GUESS_PATTERN = Pattern.compile("^[0-9]+$");
    private final Pattern MY_EXIT_PATTERN = Pattern.compile("^exit$");

    private static final String THEIR_START_GAME_MSG = "nytt spel";

    private XMPPTCPConnection connection;

    private ConcurrentHashMap<String, GameInfo> map = new ConcurrentHashMap<>();

    private static class GameInfo {

        private final int secret;
        private int numberOfGuesses;

        private GameInfo(int secret, int numberOfGuesses) {
            this.secret = secret;
            this.numberOfGuesses = numberOfGuesses;
        }

        public int getSecret() {
            return secret;
        }

        public int getNumberOfGuesses() {
            return numberOfGuesses;
        }

        public void incNumberOfGuesses() {
            numberOfGuesses++;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", GameInfo.class.getSimpleName() + "[", "]")
                    .add("secret=" + secret)
                    .add("numberOfGuesses=" + numberOfGuesses)
                    .toString();
        }
    }

    private void logout() {
        this.connection.disconnect();
    }

    private void connectAndLogin() throws IOException, InterruptedException, XMPPException, SmackException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setHost(XMPP_HOST)
                .setUsernameAndPassword(MY_JID, MY_PWD)
                .setXmppDomain(XMPP_DOMAIN)
                .setResource(UUID.randomUUID().toString())
                .build();
        connection = new XMPPTCPConnection(config);
        connection.connect(); //Establishes a connection to the server
        connection.login(); //Logs in
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
        Roster roster = Roster.getInstanceFor(connection);
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> collection) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "added: " + collection);
            }

            @Override
            public void entriesUpdated(Collection<Jid> collection) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "updated: " + collection);
            }

            @Override
            public void entriesDeleted(Collection<Jid> collection) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "deleted: " + collection);
            }

            @Override
            public void presenceChanged(Presence presence) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, presence.getFrom() + " presence changed to: " + presence.isAway());
            }
        });
    }

    private void pladdra() throws IOException, SmackException.NotConnectedException, InterruptedException {
        final ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener((from, message, chat) -> {
            try {
                final String messageBody = message.getBody();
                System.out.printf("%s: %s%n", from, messageBody);
                if (MY_HELLO_PATTERN.matcher(messageBody).matches()) {
                    chatManager.chatWith(from).send("Hej");
                } else if (MY_START_GAME_PATTERN.matcher(messageBody).matches()) {
                    chatManager.chatWith(from).send("Gissa ett tal mellan 1 och 100!");
                    int secret = new Random().nextInt(100) + 1;
                    map.put(from.toString(), new GameInfo(secret, 0));
                    System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "Secret number is " + secret);
                    System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "Ongoing games: " + map);
                } else if (MY_GUESS_PATTERN.matcher(messageBody).matches()) {
                    int guess = Integer.parseInt(messageBody);
                    GameInfo gameInfo = map.get(from.toString());
                    if (gameInfo == null) {
                        chatManager.chatWith(from).send("Du måste starta ett spel först med 'spela'!");
                    } else {
                        gameInfo.incNumberOfGuesses();
                        if (guess > gameInfo.getSecret()) {
                            chatManager.chatWith(from).send("För högt!");
                        } else if (guess < gameInfo.getSecret()) {
                            chatManager.chatWith(from).send("För lågt!");
                        } else {
                            chatManager.chatWith(from).send("Rätt!");
                            chatManager.chatWith(from).send("Du behövde " + gameInfo.getNumberOfGuesses() + " gissningar.");
                            map.remove(from.toString());
                            System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "Remaining games: " + map);
                        }
                    }
                } else if (MY_PLAY_WITH_PATTERN.matcher(messageBody).matches()) {
                    Matcher matcher = MY_PLAY_WITH_PATTERN.matcher(messageBody);
                    if (matcher.matches()) {
                        String theirName = matcher.group(1);
                        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "their name: '" + theirName + "'");
                        EntityBareJid theirJid = JidCreate.entityBareFrom(theirName);
                        // Start new remote game
                        chatManager.chatWith(theirJid).send(THEIR_START_GAME_MSG);

                        // TODO: Add new clauses to the already complicated if statement to parse the responses from the remote bot.
                    }
                } else if (MY_EXIT_PATTERN.matcher(messageBody).matches()) {
                    connection.disconnect();
                    System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
        String keyboardInput = "";
        do {
            System.out.println("q to quit");
            keyboardInput = reader.readLine();
        } while (!"q".equalsIgnoreCase(keyboardInput));
    }

    public static void main(String... args) throws IOException, InterruptedException, XMPPException, SmackException {
        Pladder app = new Pladder();
        app.connectAndLogin();
        app.pladdra();
        app.logout();
    }
}
