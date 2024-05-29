- ESP32 development board
- L298N motor driver module
- Power supply (ensure sufficient current for all fans and L298N)
- Multiple fans (compatible voltage, 3.3V or 5V depending on your Pi)
	- Operating voltage: 3V-5V
	- Current: 0.2 A
	- Brushless DC fan
	- Fan dimensions: 30mm x 30mm x 8mm
	- Wire length: 3.25" / 80mm
	- Fan weight: 6.2g / 0.22oz
- Jumper wires
- Breadboard (optional for prototyping)
- Temperature sensor (e.g., DHT22 sensor)


**Raspberry PI (other SBCs):**
On each SBC:
- Read CPU temperature data.
- Publish the temperature data to a topic on your network messaging service (e.g., MQTT broker) at regular intervals.


**Benefits of using ESP32:**

- Centralized control: The ESP32 acts as a central hub, receiving temperature data from both the Raspberry Pis and the external sensor.
- Flexibility: You can add more Raspberry Pis to the cluster and the ESP32 can handle communication and fan control seamlessly.
- Scalability: If needed, you can connect additional L298N drivers and fans to the ESP32 for a larger cooling system.

---
With a single fan rated at 0.2 Amps (0.2A) and the L298N driver capable of handling 2 Amps (2A) per channel, you can connect multiple fans to a single L298N driver. Here's the breakdown:

- **Safety margin:** It's generally advisable to leave some headroom for safety and unexpected variations. In this case, even with a 0.2A fan, running multiple fans at maximum power could push the L298N close to its limit.
    
- **Calculation:** Let's say you want to connect **X fans**. If each fan draws 0.2A, the total current draw would be:
    

```
Total current = X fans * 0.2 Amps/fan
```

- **Staying within limits:** To ensure safe operation, you'll want the total current to be well below the 2A limit of the L298N per channel. For example, let's say you target a maximum of 75% of the 2A limit:

```
Safe current limit = 2 Amps * 0.75 = 1.5 Amps
```

- **Determining the number of fans (X):** We can set the equation for total current to be less than the safe current limit and solve for X:

```
X fans * 0.2 Amps/fan < 1.5 Amps
X < 1.5 Amps / 0.2 Amps/fan
X < 7.5
```

Since you can't connect parts of fans, we round down to the nearest whole number of fans. In this scenario, you can safely connect **up to 7 fans** to a single L298N driver.

**Here are some additional considerations:**

- **Heat dissipation:** Even with a safe current draw, running multiple fans at the same time can still generate heat in the L298N driver. Consider adding a heatsink to the L298N to improve heat dissipation, especially if you're pushing close to the *7* fan limit.
    
- **Power supply:** Make sure your power supply can provide enough current to handle all the fans and the L298N driver itself. The total current draw will increase as you add more fans.
    

By following these guidelines, you can safely connect multiple 0.2A fans to your L298N driver for your Raspberry Pi cluster fan hub.
