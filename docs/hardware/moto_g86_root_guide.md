# Ghid de Rootare: Motorola Moto g86 Power 5G (XT2527 / MT6878)

Acest document descrie procedura completă, reproductibilă și sigură de deblocare a bootloader-ului și obținere a accesului **Root (Magisk)** pe modelul **Motorola Moto g86 Power 5G** (nume de cod `nice`, chipset MediaTek Dimensity MT6878 / Dimensity 7300, arhitectură A/B cu Android 14 / 15 / 16 GKI).

Ghidul rezolvă specific eroarea critică **`(bootloader) Preflash validation failed`** întâlnită frecvent pe bootloader-ele moderne Motorola.

---

## 1. Arhitectura Dispozitivului și Specificații Tehnice

* **Model:** Motorola Moto g86 Power 5G (Seria `XT2527-x`, ex. `XT2527-7`)
* **Nume de cod intern:** `nice`
* **SoC:** MediaTek MT6878 (Dimensity 7300 / 7025)
* **Partiționare:** A/B Virtual A/B (Dynamic Partitions în `super`, sloturi active `_a` și `_b`)
* **Kernel & Ramdisk:** Linux Kernel 6.1 (GKI Architecture, Boot Header v4). Ramdisk-ul de inițializare se află în partiția **`init_boot`**, nu în `boot`!
* **Securitate:** Android Verified Boot (AVB 2.0), dm-verity, Motorola MBM (Motorola Boot Manager).

---

## 2. Cauza Erorii `(bootloader) Preflash validation failed`

Pe dispozitivele Motorola recente, bootloader-ul MBM verifică integritatea și structura ramdisk-ului înainte de scriere.
1. **Incompatibilitatea Magisk v28+ / v31:** Versiunile recente de Magisk au rescris modul de împachetare a imaginilor și footer-ului AVB, generând nepotriviri de padding și descriptori pe care MBM le respinge direct cu `Preflash validation failed`.
2. **Discrepanța AVB Footer:** Când imaginea `init_boot` este reîmpachetată de patcher, dimensiunea noului ramdisk comprimat este mai mică decât dimensiunea originală a imaginii stoc. Dacă footer-ul AVB păstrează vechea valoare `Image Size` sau descriptorul SHA-256 nu este aliniat, MBM consideră imaginea trunchiată/coruptă.
3. **Rolul Magisk v27.0:** Folosirea **Magisk v27.0** împreună cu alinierea corectă a descriptorului AVB Hash permite scrierea cu succes a imaginii pe sloturile `init_boot_a` și `init_boot_b`.

---

## 3. Cerințe Preliminare

* Computer cu Windows / Linux / macOS.
* **Android SDK Platform-Tools** (versiunea oficială Google cu `adb` și `fastboot`).
* Drivere oficiale USB Motorola (pe Windows: `Motorola Mobile Drivers`).
* Cablu USB de calitate conectat la un port USB 3.0 direct (evitați hub-urile pasive).
* Backup complet al datelor personale din telefon.
* Pachetul oficial de firmware fastboot corespunzător versiunii curente de soft (poate fi descărcat prin Motorola Rescue and Smart Assistant - RSA / LMSA sau mirror-ul Lolinet).

---

## 4. Pasul 1: Deblocarea Oficială a Bootloader-ului

> [!WARNING]
> Deblocarea bootloader-ului declanșează ștergerea completă a datelor (Factory Reset) conform politicilor de securitate Android.

1. Activați **Developer Options**:
   * *Settings > About Phone > Atingeți de 7 ori pe „Build Number”*.
2. Activați opțiunile de depanare:
   * *Settings > System > Developer Options > Bifați „OEM Unlocking” și „USB Debugging”*.
3. Reporniți telefonul în modul Fastboot:
   ```bash
   adb reboot bootloader
   ```
4. Obțineți string-ul unic de deblocare:
   ```bash
   fastboot oem get_unlock_data
   ```
5. Conectați-vă pe [Portalul Oficial Motorola Bootloader Unlock](https://en-us.support.motorola.com/app/utils/welcome?p_next_page=standalone%2Fbootloader%2Funlock-your-device-b), introduceți string-ul generat și solicitați cheia de deblocare prin email.
6. Aplicați cheia de deblocare pe telefon:
   ```bash
   fastboot oem unlock <CHEIA_PRIMITA_PE_EMAIL>
   ```
7. Confirmați pe ecranul telefonului cu tastele de volum și butonul Power. Telefonul se va reseta și va afișa la pornire starea de securitate *Orange State* (`unlocked: yes`).

---

## 5. Pasul 2: Extragerea și Pregătirea Partiției `init_boot`

Din arhiva oficială de firmware stock descărcată (fișier `.zip`), extrageți fișierele esențiale:
* `init_boot.img` (aproximativ 8 MB)
* `boot.img` (aproximativ 64 MB)
* `vbmeta.img` (aproximativ 8 KB)
* `flashfile.xml`

Transferați fișierul original `init_boot.img` în memoria internă a telefonului:
```bash
adb push init_boot.img /sdcard/Download/init_boot.img
```

---

## 6. Pasul 3: Patch-uirea cu Magisk v27.0

1. Instalați pe telefon **Magisk v27.0** (APK-ul oficial `Magisk-v27.0.apk`):
   ```bash
   adb install -r Magisk-v27.0.apk
   ```
2. Deschideți aplicația **Magisk** pe telefon.
3. În secțiunea **Magisk** din partea de sus, apăsați pe **Install**.
4. Selectați opțiunea: **„Select and Patch a File”** (Selectează și corectează un fișier).
5. Navigați în folderul `Download` și alegeți **`init_boot.img`**.
6. Apăsați **LET'S GO** și așteptați mesajul `- All done!`.
7. Descărcați fișierul generat (ex. `magisk_patched-27000_xxxxx.img`) înapoi pe computer:
   ```bash
   adb pull /sdcard/Download/magisk_patched-27000_*.img ./magisk_patched_init_boot.img
   ```

---

## 7. Pasul 4: Alinierea Structurii AVB (Dacă este necesar)

Dacă bootloader-ul respinge scrierea cu `Preflash validation failed`, asigurați-vă că footer-ul AVB este recreat folosind `avbtool.py` cu dimensiunea reală și sarea de hash originală:

```python
import subprocess

avbtool = 'avbtool.py'
image = 'magisk_patched_init_boot.img'

# 1. Ștergere footer vechi nealiniat
subprocess.run(['python', avbtool, 'erase_footer', '--image', image], check=True)

# 2. Adăugare hash footer compatibil cu partiția de 8MB
subprocess.run([
    'python', avbtool, 'add_hash_footer',
    '--image', image,
    '--partition_name', 'init_boot',
    '--partition_size', '8388608',
    '--salt', 'd3243ffe30e83a21b82eed2e0532284e11881599b73553ddfe9075680962bc03'
], check=True)
```

---

## 8. Pasul 5: Flashing prin Fastboot

1. Reporniți telefonul în bootloader:
   ```bash
   adb reboot bootloader
   ```
2. Asigurați-vă că dispozitivul este detectat:
   ```bash
   fastboot devices
   ```
3. Asigurați-vă că `vbmeta` rămâne cel oficial de fabrică:
   ```bash
   fastboot flash vbmeta_a vbmeta.img
   fastboot flash vbmeta_b vbmeta.img
   ```
4. Flash-uiți imaginea modificată pe ambele sloturi pentru consistență:
   ```bash
   fastboot flash init_boot_a magisk_patched_init_boot.img
   fastboot flash init_boot_b magisk_patched_init_boot.img
   ```
   *Răspunsul așteptat:*
   ```text
   Sending 'init_boot_a' (8192 KB)                    OKAY [  0.220s]
   Writing 'init_boot_a'                              OKAY [  0.077s]
   Finished. Total time: 0.300s
   ```
5. Reporniți în sistem:
   ```bash
   fastboot reboot
   ```

---

## 9. Pasul 6: Verificare și Acordare Acces Superuser

1. După repornire, deblocați ecranul și deschideți aplicația **Magisk**.
2. În aplicație va fi afișat:
   * **Magisk:** `Installed: 27.0 (27000)`
   * **Ramdisk:** `Yes`
3. În setările Magisk (*Settings* - rotița din dreapta sus):
   * La **Superuser Access**, asigurați-vă că este selectat **Apps and ADB**.
   * La **Automatic Response**, puteți alege **Grant** pentru acordare automată în sesiunile de depanare.
4. Verificați accesul root din linia de comandă:
   ```bash
   adb shell su -v
   # Ieșire: 27.0:MAGISKSU

   adb shell "su -c id"
   # Ieșire: uid=0(root) gid=0(root) groups=0(root) context=u:r:magisk:s0
   ```

---

## 10. Acces Hardware Extins: Modem MediaTek CCCI & TTY

Odată ce dispozitivul este rootat, procesele cu privilegii depline pot interacționa direct cu magistrala dintre Procesorul de Aplicații (AP Linux) și Procesorul de Bandă de Bază (BP MediaTek):
* `/dev/ttyC0` – `/dev/ttyC6`: Canale de comunicație serială virtuală (comenzi AT, date celulare).
* `/dev/ccci_*`: Interfețe directe de memorie partajată Cross-Core Communication Interface.
* Aplicațiile MycoNet pot rula servicii avansate la nivel de nucleu pentru comunicații mesh ad-hoc și Wi-Fi Aware complet descentralizate.
