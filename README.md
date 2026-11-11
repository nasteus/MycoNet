# MycoNet

Rețea de comunicare descentralizată, peer-to-peer (P2P) și off-grid pentru dispozitive Android, capabilă să funcționeze fără conexiune la internet, fără turnuri de telefonie mobilă și fără servere centrale.

---

## 🌟 Caracteristici Cheie

* **Comunicație Off-Grid Reală:** Utilizează **Wi-Fi Aware (NAN)** și **UDP Multicast** pentru a crea rețele ad-hoc mesh spontane între dispozitive apropiate.
* **Securitate End-to-End:** Criptare pe fiecare pachet și rutare distribuită inspirată din rețelele fungice naturale (miceliu).
* **Interfață Modernă:** Aplicație bazată pe Web UI (SvelteKit) integrată într-un container nativ Android (Capacitor/WebView).
* **Compatibilitate Extinsă:** Funcționează nativ pe Android 8.0+ fără a necesita root. Pentru cercetare avansată hardware și inspecție de pachete de joasă frecvență, dispozitivele rootate oferă acces la straturile inferioare de rețea.

---

## 📚 Documentație

* [**Ghid de Rootare Motorola Moto g86 Power 5G (XT2527 / MT6878)**](docs/hardware/moto_g86_root_guide.md)  
  *Procedură detaliată pentru deblocare bootloader, rezolvarea erorii `Preflash validation failed` și patch-uire AVB 2.0 cu Magisk v27.*
* [**Ghid de Restaurare Stoc și Re-blocare Bootloader (XT2527)**](docs/hardware/moto_g86_stock_restore_guide.md)  
  *Procedură completă de eliminare Magisk, restaurare firmware original și blocare bootloader pentru compatibilitate bancară și vânzare.*
* [**Configurare Nod Server 24/7 & Protecție Baterie**](docs/hardware/battery_and_server_tuning.md)  
  *Ghid de utilizare ca server autonom ARM64 și activare a plafonului de încărcare de 80% (Overcharge Protection).*
* [**Arhitectură: Wi-Fi Mesh vs. Comunicații Celulare Directe (D2D)**](docs/architecture/cellular_vs_wifi_mesh.md)  
  *Analiză tehnică privind limitările modemurilor celulare de consum și modul în care MycoNet realizează comunicația directă off-grid.*

---

## 🛠️ Structura Proiectului

* `android/` - Proiectul nativ Android (stiva Java/Kotlin, permisiuni Wi-Fi Aware, servicii de fundal).
* `web/` - Interfața grafică utilizator (Svelte / SvelteKit).
* `proto/` - Definițiile Protobuf pentru pachetele de date transmise prin rețeaua mesh.
* `docs/` - Documentație tehnică, ghiduri hardware și diagrame de arhitectură.

---

## 🚀 Compilare și Instalare

Pentru a compila aplicația și a o instala pe un dispozitiv conectat prin ADB:
```cmd
BUILD_AND_DEPLOY.bat
```
