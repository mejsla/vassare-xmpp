import sleekxmpp
import re
import random


class DaemonBot(sleekxmpp.ClientXMPP):
    def __init__(self, jid, password):
        super(DaemonBot, self).__init__(jid, password)

        self.nick = jid.split("@")[0]
        self.running = True
        self.register_plugin('xep_0030')  # Service Discovery
        self.register_plugin('xep_0045')  # Multi-User Chat
        self.register_plugin('xep_0199')  # XMPP Ping

        self.add_event_handler('session_start', self.start)

        # Will have callbacks for group chat and direct messages
        # as well as error messages
        self.add_event_handler("message", self.message)

    def start(self, event):
        print("Sending presence")
        self.send_presence()
        print("Getting roster")
        self.get_roster()

    def message(self, msg):
        type = msg['type']
        # can be "", "normal", "chat", "headline", "error", "groupchat"
        try:
            result = (True, None)
            if type == "chat":
                result = self.parse_message(self.nick, msg)

            if result[0]:
                if result[1] is not None:
                    self.send_message(
                        mto=msg['from'],
                        mbody=result[1])
            else:
                print("Disconnecting")
                self.disconnect(wait=True)
        except Exception as err:
            print(err)

    def parse_message(self, nick, message):
        input = message['body'].lower().strip()

        if re.match(r"^hej.*", input) is not None:
            return (True, self.do_greet())

        if re.match(r"^exit$", input) is not None:
            return (False, None)

        return (True, "I did not understand that")

    def do_greet(self):
        return random.choice(['Tjenixen', 'Halloj'])
