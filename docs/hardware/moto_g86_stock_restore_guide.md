# Ghid de Restaurare Stoc și Re-blocare Bootloader: Motorola Moto g86 Power 5G

Acest document descrie procedura completă și sigură pentru readucerea dispozitivului **Motorola Moto g86 Power 5G (XT2527-7, codename `nice`, MediaTek Dimensity 7300 / MT6878)** la starea 100% originală din fabrică, eliminarea Magisk și re-blocarea bootloader-ului (*OEM Lock*).

Această procedură este necesară pentru a asigura trecerea completă a verificărilor hardware **Google Play Integrity** (`MEETS_DEVICE_INTEGRITY`) și funcționarea fără erori a aplicațiilor bancare (BT Pay, George BCR, Raiffeisen, Revolut, CEC, ING) și Google Wallet.

---

## 1. De ce o simplă „Resetare din Fabrică” (Factory Reset) nu este suficientă

1. **Magisk rezidă în partiția `init_boot`:** Resetarea din meniul Android șterge doar datele din partiția `/data`. Ramdisk-ul modificat cu `magiskinit` rămâne neschimbat în partițiile de boot.
2. **Bootloader-ul rămâne deblocat (`State: Unlocked` / Starea portocalie):** Chiar și fără fișiere de root vizibile, sistemul TEE/StrongBox raportează starea deblocată a bootloader-ului, ducând la eșecul validării Play Integrity.

---

## 2. Cerințe Prealabile

* Unelte Android Platform-Tools oficiale (`fastboot.exe`, `adb.exe`).
* Imaginile stock originale extrase din pachetul oficial de firmware Motorola corespunzător versiunii software curente:
  * `init_boot.img` (ramdisk original curat, dimensiune 8.388.608 octeți).
  * `boot.img` (kernel generic Linux GKI 6.1 semnat de producător).
  * `vbmeta.img` (descriptorii criptografici originali AVB 2.0, dimensiune 8.192 octeți).
* Cablu USB de calitate conectat la un port direct al PC-ului (USB 2.0 / 3.0).

> [!IMPORTANT]
> **Dezactivarea protecției FRP (Factory Reset Protection):**  
> Înainte de vânzare sau predare către alt utilizator, intrați în *Setări -> Parole și conturi -> Contul Google -> Eliminare cont*. Acest pas asigură că dispozitivul nu va cere autentificarea cu vechiul cont Google la prima configurare.

---

## 3. Scenariul A: Eliminare Root FĂRĂ Pierderea Datelor

Dacă utilizatorul dorește să păstreze toate aplicațiile, pozele și mesajele, dar să elimine permisiunile Magisk:

1. Porniți dispozitivul în modul **Fastboot**:
   ```cmd
   adb reboot bootloader
   ```
   *(sau opriți telefonul și țineți apăsat `Volume Down` + `Power`).*
2. Rescrieți imaginile curate pe ambele sloturi de boot:
   ```cmd
   fastboot flash init_boot_a init_boot_stock.img
   fastboot flash init_boot_b init_boot_stock.img
   fastboot flash vbmeta_a vbmeta_stock.img
   fastboot flash vbmeta_b vbmeta_stock.img
   ```
3. Reporniți dispozitivul:
   ```cmd
   fastboot reboot
   ```

*Rezultat:* Dispozitivul pornește curat, Magisk este inactiv, iar toate datele din `/data` rămân intacte.

---

## 4. Scenariul B: Revenire Completă în Stare de Fabrică (Pregătire Vânzare / Banking)

> [!CAUTION]
> **Această procedură șterge ireversibil toate datele utilizatorului!**  
> Realizați o copie de siguranță a tuturor fișierelor importante înainte de a executa comenzile.

1. Porniți dispozitivul în modul **Fastboot**.
2. Rescrieți imaginile originale pe ambele sloturi pentru a garanta integritatea lanțului de încredere (AVB 2.0):
   ```cmd
   fastboot flash init_boot_a init_boot_stock.img
   fastboot flash init_boot_b init_boot_stock.img
   fastboot flash boot_a boot_stock.img
   fastboot flash boot_b boot_stock.img
   fastboot flash vbmeta_a vbmeta_stock.img
   fastboot flash vbmeta_b vbmeta_stock.img
   ```
3. Trimiteți comanda de re-blocare a bootloader-ului:
   ```cmd
   fastboot oem lock
   ```
4. **Confirmare pe ecranul dispozitivului:**
   * Pe ecranul telefonului va apărea un meniu de avertizare Motorola Bootloader.
   * Utilizați tasta **Volume Down** pentru a selecta opțiunea **"LOCK THE BOOTLOADER"**.
   * Confirmați prin apăsarea scurtă a tastei **Power**.
5. Dispozitivul va formata automat partițiile `/data` și `/metadata` și va reporni în starea nativă securizată (**State: LOCKED / Green**).

---

## 5. Verificare Post-Restaurare

După finalizarea Scenariului B:
* Ecranul de avertizare galben/portocaliu la pornire a dispărut complet.
* Dispozitivul afișează ecranul inițial de bun venit (*Hello Moto*).
* În Magazinul Google Play: *Setări -> Despre -> Certificarea dispozitivului* indică **„Dispozitivul este certificat”**.
* Aplicațiile de online banking și portofelele digitale (Google Wallet) se instalează și funcționează fără nicio restricție.
