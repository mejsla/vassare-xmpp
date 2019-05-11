import pladdrare.bots

jid = "bot01@zoom.nu"
password = "bot"
host = "cluster.zoom.nu"
port = 5222

daemon_bot = pladdrare.bots.DaemonBot(jid, password)
print("Connecting")
connected = daemon_bot.connect((host, port), False)

if connected:
    print("Connected")
    daemon_bot.process(block=True)
else:
    print("Did not connect")
