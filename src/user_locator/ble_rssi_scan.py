from bluepy.btle import Scanner
import paho.mqtt.client as paho
from paho import mqtt
import time
import sys

class BluetoothLocator:
    def __init__(self, location, edge_distance, meter_rssi):
        """Inits BluetoothLocator with location identifier and edge RSSI of location."""
        self.location = location
        self.edge_distance = edge_distance
        self.meter_rssi = meter_rssi
        self.users = BluetoothLocator.getUserDevices()
        self.client = self.mqttConnect()

    @staticmethod
    def getUserDevices():
        #Loads user names and user devices names from file.
        file = open("users.txt", "r")
        users = file.readlines()
        file.close()

        user_dict = {}
        for user in users:
            user_name, user_device = user.strip().split(',')
            user_dict[user_device] = user_name

        #print(user_dict)

        return user_dict

    def mqttConnect(self):
        #Connects to MQTT server.
        client = paho.Client(client_id="rpi", userdata=None, protocol=paho.MQTTv5)
        #client.on_connect = on_connect

        client.tls_set(tls_version=mqtt.client.ssl.PROTOCOL_TLS)
        client.username_pw_set("user1", "user12345")
        client.connect("1113004078a64e9ca046977925d355ee.s2.eu.hivemq.cloud", 8883)

        return client

    def scanLoop(self):
        """Infinite loop periodicaly scanning available devices and informing MQTT server.

        Periodicaly scans BLE devices, and for those that identify users,
        publishes a MQTT message if their availability changed.
        """
        near = {}
        last_near = {}
        for device in self.users:
            last_near[self.users[device]] = False

        scanner = Scanner()

        while True:

            devices = scanner.scan(10.0)
            for device in devices:
                device_name = device.getValueText(9)
                distance = BluetoothLocator.getDistance(self.meter_rssi, device.rssi)
                #print(device_name)
    
                if device_name in self.users:
                    user_name = self.users[device_name]
                    #if device.rssi > self.edge_rssi:
                    if distance < self.edge_distance:
                        near[user_name] = True
    
                        if not last_near[user_name]:
                            self.client.publish(self.location + "/proximity/" + user_name, "on", qos=0)
    
            for name in near:
                if not near[name] and last_near[name]:
                    self.client.publish(self.location + "/proximity/" + name, "off", qos=0)
                last_near[name] = near[name]
                near[name] = False
    
            time.sleep(60)

    @staticmethod
    def getDistance(meter_rssi=-69, rssi, n=2.4):
        """Computes distance in meters from RSSI.

        Computes distance in mters from RSSI based on measured current RSSI and static RSSI with one meter distance.
        """
        distance = pow(10, (meter_rssi - rssi)/(10*n))
        return distance


if __name__ == "__main__":
    if len(sys.argv) == 1:
        print('Should be executed like "sudo python ble_rssi_scan.py <location> <edge_distance> <meter_rssi>".')
        exit(0)
    if len(sys.argv) != 4:
        print('Invalid number of arguments, should be 2', file=sys.stderr)
        exit(-1)
    location = sys.argv[1]
    try:
        int(sys.argv[2])
    except ValueError:
        print('Second argument should be an integer', file=sys.stderr)
        exit(-2)

    try:
        int(sys.argv[3])
    except ValueError:
        print('Second argument should be an integer', file=sys.stderr)
        exit(-2)

    edge_distance = int(sys.argv[2])
    meter_rssi = int(sys.argv[3])

    locator = BluetoothLocator(location, edge_distance, meter_rssi)
    try:
        locator.scanLoop()
    except KeyboardInterrupt:
        print("Exiting...")
        exit(0)
