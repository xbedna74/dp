# MAS for Smart home (heating)

## Creating custom thermostatic valve
Needed parts:

Needed parts:
1. Digital radiator valve eQ-3 N, https://www.amazon.de/-/cs/dp/B085LW2K1M/ref=sr_1_6?dchild=1&keywords=eq3+n&qid=1620651957&sr=8-6
2. DC motor driver L298N, https://www.amazon.de/-/cs/dp/B07DK6Q8F9/ref=sr_1_6?dchild=1&keywords=l298n&qid=1620652036&sr=8-6
3. NodeMCU, https://www.amazon.de/-/cs/dp/B08F7RBLB9/ref=sr_1_8?dchild=1&keywords=nodemcu&qid=1620652064&sr=8-8
4. DHT22, https://www.amazon.de/-/cs/dp/B078SVZB1X/ref=sxin_6_ac_d_rm?ac_md=0-0-ZGh0MjI%3D-ac_d_rm&cv_ct_cx=dht22&dchild=1&keywords=dht22&pd_rd_i=B078SVZB1X&pd_rd_r=c8ab428a-8de6-4b79-ab7c-468ed82d9d46&pd_rd_w=izPyg&pd_rd_wg=xlQGi&pf_rd_p=d7022e5d-d98a-48af-b540-ab9d3c0ad7eb&pf_rd_r=7MM014YAD9S7BQJ0A0GK&psc=1&qid=1620652101&sr=1-1-fe323411-17bb-433b-b2f8-c44f2e1370d4

Firstly you need to open the valve head. From there remove the board and the carrier for batteries.
Then you are left with only dc motor with parts that press the valve of radiator.
Take L298N and dc motor and connect black wire from dc motor to OUT1 on L298N, and red wire on dc motor with OUT2 on L298N.
Connect NodeMCU Vin with L298N 12V and L298N GND with NodeMCU GND. Then connect D1/D2 on NodeMCU with IN1/IN2 on L298N.
Connect DHT22 Vcc pin with 3v3 on NodeMCU, connect grounds between them, and connect DHT22s data pin with D7 on NodeMCU.
Lastly connect data pin and Vcc pin on DHT22 via resistor (10kΩ or 4.7kΩ).

After this you have modified radiator valve. It can be powered via usb connected to NodeMCU, from normal phone charger adapter with 5V.
