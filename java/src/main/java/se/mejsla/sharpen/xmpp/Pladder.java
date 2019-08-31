package se.mejsla.sharpen.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

//
// https://www.baeldung.com/xmpp-smack-chat-client
//
public class Pladder {
    private XMPPTCPConnection connection;

    public static void main(String... args) throws IOException, InterruptedException, XMPPException, SmackException {
        Pladder app = new Pladder();
        app.connectAndLogin();
        app.pladdra() ;
        app.logout() ;
    }

    private void logout() {
        this.connection.disconnect();
    }

    private final String XMPP_HOST = "cluster.zoom.nu";
    private final String MY_JID = "bot09";
    private final String MY_PWD = "bot";

    private final Pattern HEJ_PATTERN = Pattern.compile("^hej.*") ;
    private final Pattern EXIT_PATTERN = Pattern.compile("^exit$") ;

    private void connectAndLogin() throws IOException, InterruptedException, XMPPException, SmackException {
        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setHost(XMPP_HOST)
                .setUsernameAndPassword(MY_JID, MY_PWD)
                .setXmppDomain("cluster.zoom.nu")
                .setResource(UUID.randomUUID().toString())
                .build();
        this.connection = new XMPPTCPConnection(config);
        connection.connect(); //Establishes a connection to the server
        connection.login(); //Logs in
        Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
        Roster roster = Roster.getInstanceFor(connection);
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> collection) {

            }

            @Override
            public void entriesUpdated(Collection<Jid> collection) {

            }

            @Override
            public void entriesDeleted(Collection<Jid> collection) {

            }

            @Override
            public void presenceChanged(Presence presence) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, presence.getFrom() + " presence changed to: " + presence.isAway());
            }
        });    }

    private void pladdra() throws IOException, SmackException.NotConnectedException, InterruptedException {
        final ChatManager chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener((from, message, chat) -> {
            try {
                final String messageBody = message.getBody();
                System.out.println(String.format("%s: %s\n", from, messageBody));
                if (HEJ_PATTERN.matcher(messageBody).matches()) {
                    chatManager.chatWith(from).send("Hej");
                }
                if (EXIT_PATTERN.matcher(messageBody).matches()) {
                    connection.disconnect();
                    System.exit(0);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }) ;
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(System.in));
        String keyboardInput = "" ;
        do {
            System.out.println("q to quit");
            keyboardInput = reader.readLine();
        } while(! "q".equalsIgnoreCase(keyboardInput)) ;
    }
}
