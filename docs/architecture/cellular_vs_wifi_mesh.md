# Arhitectură de Comunicație Off-Grid: Wi-Fi Aware (NAN) vs. Comunicații Celulare Directe (D2D)

Acest document analizează fundamentele tehnice ale rețelei descentralizate **MycoNet** pe dispozitive Android moderne, comparând abordarea software bazată pe **Wi-Fi Aware / UDP Multicast** cu limitările fizice și de securitate ale modemurilor celulare (2G/3G/4G/5G).

---

## 1. De ce NU este posibilă emisia celulară directă telefon-la-telefon (fără turn)

Dispozitivele comerciale (User Equipment - UE) precum telefoanele inteligente sunt proiectate strict ca receptoare/emițătoare slave coordonate de o rețea de bază:
1. **Lipsa modului eNodeB/gNodeB în firmware:** Modemul de bandă de bază (MediaTek, Qualcomm, Samsung) rulează un sistem de operare separat (RTOS în timp real) stocat în partiția `modem.img` / `md1img`. Niciun producător nu include stiva de stație de bază (BTS) în firmware-ul de consum.
2. **Sincronizarea de timp și frecvență:** Comunicația LTE/5G necesită cadre de sincronizare rigide (PSS/SSS - Primary/Secondary Synchronization Signals) transmise continuu de turnul de celulă. Fără acest semnal de referință, două telefoane UE nu își pot sincroniza oscilatoarele radio.
3. **Restricții 3GPP Sidelink (PC5):** Deși standardul 5G permite teoretic comunicații Sidelink (V2X / ProSe), aceste funcții sunt dezactivate la nivel de NVRAM/firmware pe 99.9% din telefoanele comerciale și necesită alocări de spectru licențiat.

Pentru emisie de celulă celulară reală independentă, este obligatorie utilizarea unui modul radio definit software (**SDR - Software Defined Radio**) extern (cum ar fi **HackRF One**, **LimeSDR**, **USRP**) conectat la un nucleu open-source (ex. `srsRAN` sau `Open5GS`).

---

## 2. Soluția Nativă MycoNet: Wi-Fi Aware (NAN) și UDP Multicast

Rețeaua **MycoNet** ocolește complet nevoia de turnuri celulare sau routere intermediare prin utilizarea tehnologiilor radio Wi-Fi descentralizate permise nativ în Android (începând cu Android 8.0 Oreo):

* **Wi-Fi Aware (Neighbor Awareness Networking - NAN):** Permite descoperirea automată și bidirecțională a dispozitivelor vecine fără conectare la un punct de acces Wi-Fi comun.
* **UDP Multicast / Wi-Fi Direct:** Asigură transferul de pachete de mesagerie securizată, rute de dispersie și sincronizare distribuită prin topologie de plasă (mesh).
* **Zero Costuri / Zero Dependențe de Infrastructură:** Funcționează complet în afara rețelei (off-grid), în scenarii de dezastre naturale, lipsă de semnal GSM sau cenzură.

---

## 3. Diagrama Arhitecturii Hardware & Modem

Diagrama de mai jos ilustrează separarea fizică și logică dintre Procesorul de Aplicații (AP - unde rulează Android și MycoNet) și Procesorul de Bandă de Bază (BP - unde este izolat modemul celular):

![Arhitectură Hardware Modem](../../mermaid-diagram.svg)
