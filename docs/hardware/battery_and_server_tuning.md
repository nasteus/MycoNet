# Ghid Configurare Nod Server 24/7 & Protecție Baterie (ARM64)

Acest ghid detaliază pașii tehnici pentru utilizarea unui dispozitiv Android modern (**MediaTek Dimensity 7300, 8 nuclee, 8 GB RAM**) în regim de **Server Linux permanent sau Super-Nod MycoNet** alimentat non-stop la rețea.

---

## 1. Avantajele Hardware ca Server Independent

Spre deosebire de un server desktop sau de o placă de dezvoltare (ex. Raspberry Pi 5):
* **UPS Integrat:** Acumulatorul de mare capacitate (5000–6000 mAh) menține serverul activ timp de 24–48 de ore în caz de pană de curent.
* **Eficiență Energetică Extremă:** Procesul de fabricație pe 4nm asigură un consum de doar **2–4 Watti** în sarcină redusă.
* **Conectivitate Multiplă:** Wi-Fi 6, Bluetooth 5.4 și modem 5G integrat pe aceeași placă de bază.

---

## 2. Protecția Acumulatorului la Alimentare Continuă (Overcharge Protection)

Menținerea unei baterii Li-Ion la 100% tensiune continuă (4.35V - 4.45V) în prezența căldurii duce la degradarea rapidă a electrolitului și umflarea celulei.

### Activarea Limitei de Încărcare la 80%

Sistemul Motorola include protecție nativă la nivel de firmware gestionată de pachetul `com.motorola.batterycare`:

1. **Interfață Utilizator:**
   * *Setări -> Baterie -> Protecție la supraîncărcare (Overcharge protection)* -> Comutare pe **Activ**.
2. **Efect Tehnic:**
   * Pragul de încărcare este plafonat la **80%**.
   * O dată la 7 zile, sistemul execută o încărcare completă până la 100% pentru auto-calibrarea senzorului de curent (coulomb counter).
   * Când protecția este activă, bara de stare afișează pictograma de baterie protejată cu scut.

---

## 3. Optimizarea Sistemului pentru Rulare Permanentă în Fundal

Pentru a asigura funcționarea neîntreruptă a proceselor server (nod MycoNet, Termux, sshd):

1. **Eliminarea restricțiilor de fundal:**
   ```bash
   cmd appops set net.myconet.mesh RUN_IN_BACKGROUND allow
   dumpsys deviceidle whitelist +net.myconet.mesh
   ```
2. **Păstrarea conexiunii radio active cu ecranul stins:**
   * Serviciul de fundal utilizează un `WakeLock` parțial (`PARTIAL_WAKE_LOCK`) și `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.
3. **Controlul direct din aplicație:**
   * Aplicația include comutatorul `Mesh Activ / Oprit` pentru a suspenda imediat scanările radio și consumul atunci când modul server nu este necesar.
