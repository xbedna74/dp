from bluepy.btle import Scanner
import time
import sys

if __name__ == "__main__":
	name = sys.argv[1]

	scanner = Scanner()

	i = 0
	while i < 10:
		i = i+1
		devices = scanner.scan(10.0)
		for device in devices:
			device_name = device.getValueText(9)
			if device_name == name:
				print(device.rssi)