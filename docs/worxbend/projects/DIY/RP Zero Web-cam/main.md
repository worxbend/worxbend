# Wiring diagram

```
5V Rail - [Main 3A Fuse] ──┬── [1000µF Cap to GND] (input smoothing)
│
├── [LED + 1kΩ Resistor to GND] (power indicator)
│
├── [1A Fuse] ── [100µF Cap to GND] ── [Pi GPIO 5V Pin 2/4]
│
├── [2A Fuse] ── [470µF Cap to GND] ── [LED Ring Output]
│
├── [1A Fuse] ── [100µF Cap to GND] ── [Servo 1 Output]
│
└── [1A Fuse] ── [100µF Cap to GND] ── [Servo 2 Output]

Common GND ────────────────────────── [X]
```

**Capacitors**:

- 1× 1000µF/16V (input smoothing)
- 4× 100µF/16V (one for each output including Pi)
- Add the 100µF cap to Pi line for these reasons:
  - Consistency: All outputs have local filtering
  - Future-proofing: Works with both power methods
  - Safety: Extra filtering never hurts
- 1× 470µF/16V (LED ring - can use 220µF if 470µF unavailable)
