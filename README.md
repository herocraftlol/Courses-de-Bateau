# ⛵ CourseDeBateau

> **CourseDeBateau v1.3.0** est un plugin **Paper 1.21** qui transforme votre serveur Minecraft en un véritable arène de **courses de bateaux**. Créez vos circuits, invitez vos joueurs, donnez le top départ… et regardez-les s'affronter sur l'eau dans des courses multijoueurs où chaque centième compte.

Conçu sur le modèle de minijeux bien connus (HikaBrain, etc.), CourseDeBateau est un plugin **sans dépendance externe**, qui se configure **à 100 % en jeu** par commandes admin. Il propose plusieurs arènes simultanées, une GUI de sélection, des checkpoints **anti-triche** à valider dans l'ordre (en silence), un **lobby d'attente** avec compte à rebours, un **départ figé** dans le bateau, une **zone spectateur** optionnelle pour suivre la fin de course, un **sidebar en direct** avec chronos, records personnels et records du serveur, et un tout nouveau **diamant admin** qui permet à un opérateur présent dans le lobby d'attente de lancer la course d'un simple clic droit.

---

## 🌊 Ce que fait le plugin

CourseDeBateau ajoute à votre serveur un **mode de jeu course de bateaux** complet et clé en main. Un administrateur définit une ou plusieurs **arènes** (appelées *courses*) avec des commandes en jeu, puis les joueurs les rejoignent via une commande ou via une **interface graphique**, et s'affrontent dans des bateaux Minecraft sur des circuits personnalisés.

Chaque course peut être :

- 🗺️ **unique** (arène de sprint) ou coexister avec d'autres sur le même serveur,
- 🛶 **hautement paramétrable** : nombre de tours, min/max de joueurs, durées des comptes à rebours, ligne de départ/arrivée distincte, zone spectateur, etc,
- 🔁 **indépendante** : plusieurs courses peuvent tourner en parallèle sans se gêner.

---

## ✨ Fonctionnalités principales

- 🏟️ **Arènes multiples et indépendantes** — Créez autant de courses que vous voulez (*anneau1*, *grand-prix*, *kart-vs-kart*…) : chacune vit sa propre vie, avec ses propres réglages et sa propre session.
- 🚦 **Ligne de départ/arrivée distincte des checkpoints** — Impossible de valider un tour en faisant l'aller-retour sur la ligne de départ : il faut avoir franchi **tous les points de passage intermédiaires** avant que la zone de départ/arrivée ne compte comme une nouvelle validation.
- 🛡️ **Points de passage anti-triche** — Les checkpoints doivent être franchis **dans l'ordre** ; le premier non-atteint est remis à zéro si on revient en arrière. Une course non configurable dans cet ordre est refusée par le plugin.
- 🤫 **Validation silencieuse des points de passage** — Plus aucun message de chat parasite lorsque vous franchissez un checkpoint : la progression est affichée discrètement dans le sidebar, pour une expérience de course plus immersive et plus juste (les autres joueurs ne sont pas informés du checkpoint que vous avez passé).
- 💎 **Diamant admin de lancement instantané** *(nouveauté v1.3.0)* — Tout administrateur (`cdb.admin`) qui rejoint le lobby d'attente d'une course reçoit automatiquement un **diamant** dans le premier slot de sa hotbar, nommé *« Lancer la course »*. Un clic droit dessus force le départ immédiat de la course, comme `/cdb <course> forcestart`, en sautant le compte à rebours du lobby. Le diamant ne peut pas être jeté au sol, et il disparaît automatiquement (en restaurant l'objet qui occupait le slot) dès que la course démarre ou que l'admin quitte le lobby. Plus besoin d'ouvrir un terminal pour tester vos circuits !
- 🕒 **Lobby d'attente avec compte à rebours** — Une fois le nombre minimum de joueurs atteint, un compte à rebours se lance avant la téléportation ; s'il manque du monde, il s'annule automatiquement.
- 🧊 **Départ figé dans le bateau** — Juste avant le top départ, les joueurs sont assis dans leur bateau et **totalement immobilisés** pendant quelques secondes (durée configurable). Aucune manœuvre n'est possible avant le feu vert.
- 🖥️ **GUI de sélection** — La commande `/cdb gui` ouvre un menu visuel listant toutes les courses disponibles avec leur état, leur nombre de joueurs et leur configuration. Un clic pour rejoindre ou quitter.
- 🎯 **Sélection de zones à distance** — Toutes les zones (départ/arrivée, checkpoints, zone spectateur) se définissent en **visant un bloc** depuis votre position — jusqu'à 150 blocs de distance. Pas besoin d'être collé au bloc.
- 🛶 **Spawns bateau individuels** — L'administrateur définit un **point de spawn de bateau par joueur** (un par emplacement disponible), garantissant que chaque coureur a sa propre embarcation au départ, avec l'orientation enregistrée.
- 🔒 **Verrouillage en course** — Les joueurs **ne peuvent pas sortir** de leur bateau pendant la course. Seule la commande `/cdb leave` (ou la fin de la course) permet de quitter l'embarcation.
- 🛡️ **Protection contre les dégâts** — Les dégâts sont **annulés** pendant les phases *lobby*, *départ* et *course*, pour éviter toute mort accidentelle au milieu du circuit.
- 👻 **Zone spectateur optionnelle** — Une fois la course terminée, les finishers passent en mode spectateur **confinés dans une zone dédiée** (avec un point d'apparition précis possible). S'ils essaient d'en sortir, ils sont automatiquement ramenés au centre de la zone. Sans zone spectateur configurée, ils sont renvoyés à leur position d'origine.
- 🏆 **Sidebar en direct avec chronos et records** — Pendant la course, chaque joueur dispose d'un **scoreboard privé** affichant : nom de la course, tour en cours, temps du tour, temps total, **record personnel** et **record du serveur** (avec le nom du détenteur).
- 💾 **Persistance des records** — Records personnels par joueur et meilleur temps global du serveur, **sauvegardés sur disque** et mis à jour automatiquement à chaque arrivée.
- 🔌 **Reconnexion / déconnexion** — Un joueur qui se déconnecte est automatiquement retiré de la course (comme s'il avait tapé `/cdb leave`).
- 🥇 **Classement final** — À la fin, le classement des finishers est annoncé dans le chat, suivi d'un reset automatique.
- 🧰 **Configuration 100 % en jeu** — Tout se configure par commandes, à votre position. Aucun fichier YAML à éditer à la main.

---

## 🔁 Cycle de vie d'une course

```
WAITING              → en attente du minimum de joueurs
LOBBY_COUNTDOWN      → compte à rebours dans le lobby d'attente
STARTING             → joueurs téléportés dans leur bateau, immobilisés, compte à rebours de départ
RUNNING              → course en cours (checkpoints à valider dans l'ordre, sidebar en direct)
ENDING               → classement affiché, reset automatique
```

---

## 📦 Installation

1. Téléchargez la dernière version du `.jar` depuis la page **[Releases](../../releases)** (par exemple `CourseDeBateau-1.3.0.jar`).
2. Déposez-le dans le dossier `plugins/` de votre serveur **Paper 1.21+**.
3. Démarrez (ou redémarrez) le serveur — le plugin se charge automatiquement.
4. (Optionnel) Personnalisez les valeurs par défaut dans `plugins/CourseDeBateau/config.yml`, créé au premier démarrage.

> Aucune dépendance externe n'est requise : le plugin utilise uniquement l'API Paper standard (y compris `Score#numberFormat` pour masquer les nombres du sidebar, disponible depuis 1.20.3).

### Compiler depuis les sources

```bash
mvn clean package
```

Le jar compilé se trouve dans `target/CourseDeBateau-1.3.0.jar`. Adaptez la version de `paper-api` dans `pom.xml` (`1.21.4-R0.1-SNAPSHOT`) si votre serveur tourne sur un autre patch de la 1.21 — seule la version majeure `api-version: 1.21` compte vraiment pour la compatibilité en jeu.

---

## 🎮 Commandes joueur

| Commande | Description |
|---|---|
| `/cdb gui` | Ouvre le menu visuel des courses disponibles. |
| `/cdb list` | Liste toutes les courses et leur état actuel. |
| `/cdb join <course>` | Rejoindre une course spécifique. |
| `/cdb leave` | Quitter la course en cours (seul moyen de sortir du bateau). |

> Alias de la commande : `/coursedebateau` et `/boatrace`.

---

## 🛠️ Commandes admin (permission `cdb.admin`, op par défaut)

### 🗂️ Gestion globale des courses

| Commande | Description |
|---|---|
| `/cdb create <course>` | Crée une nouvelle course vide. |
| `/cdb delete <course>` | Supprime une course (et ses records). |
| `/cdb reload` | Recharge `config.yml` et toutes les courses. |

### ⚙️ Configuration d'une course

Toutes ces commandes prennent la forme `/cdb <course> <sous-commande>`.

| Commande | Description |
|---|---|
| `/cdb <course> info` | Affiche l'état de configuration complet de la course. |
| `/cdb <course> setlobby` | Définit le point de lobby d'attente à votre position. |
| `/cdb <course> addboatspawn` | Ajoute un spawn de bateau (un par joueur) à votre position. |
| `/cdb <course> removeboatspawn <index>` | Retire un spawn de bateau. |
| `/cdb <course> setstartzone pos1` / `pos2` | Définit les deux coins de la **zone de départ/arrivée** (bloc visé, jusqu'à 150 blocs). |
| `/cdb <course> setcheckpoint <index> pos1` / `pos2` | Définit un point de passage intermédiaire (1, 2, 3…) dans l'ordre du circuit. |
| `/cdb <course> removecheckpoint <index>` | Retire un point de passage. |
| `/cdb <course> setspectatorzone pos1` / `pos2` | Définit la zone spectateur (optionnelle) où les coureurs terminés sont confinés. |
| `/cdb <course> setspectatorspawn` | Définit le point d'apparition en spectateur à votre position. |
| `/cdb <course> laps <n>` | Définit le nombre de tours à effectuer. |
| `/cdb <course> maxplayers <n>` | Nombre maximum de joueurs **pour cette course** (`-1` = valeur globale). |
| `/cdb <course> minplayers <n>` | Nombre minimum de joueurs **pour cette course** (`-1` = valeur globale). |
| `/cdb <course> lobbycountdown <secondes>` | Durée du compte à rebours dans le lobby (`-1` = global). |
| `/cdb <course> startcountdown <secondes>` | Durée du compte à rebours figé dans le bateau (`-1` = global). |
| `/cdb <course> forcestart` | Force le départ immédiat de la course. |
| `/cdb <course> stop` | Arrête la course en cours et la réinitialise. |

> 💎 **Astuce v1.3.0** : au lieu de taper `/cdb <course> forcestart`, rejoignez simplement le lobby d'attente d'une course en tant qu'admin : un diamant *« Lancer la course »* apparaît dans votre premier slot de hotbar. Un clic droit dessus démarre instantanément la course.

### 🚀 Mise en place rapide d'une course

1. `/cdb create anneau1`
2. Placez-vous au point d'attente, puis `/cdb anneau1 setlobby`.
3. Pour chaque emplacement de départ (un par joueur potentiel), positionnez-vous sur la dalle et tapez `/cdb anneau1 addboatspawn` — l'ordre d'ajout détermine l'ordre d'attribution aux joueurs.
4. Visez un coin de la ligne de départ/arrivée puis `/cdb anneau1 setstartzone pos1` ; visez le coin opposé et tapez `/cdb anneau1 setstartzone pos2`.
5. Faites de même pour chaque checkpoint intermédiaire (1, 2, 3…) qui forme le circuit : `/cdb anneau1 setcheckpoint 1 pos1` puis `pos2`, etc.
6. *(Optionnel mais recommandé)* Définissez la zone spectateur : visez un coin puis `/cdb anneau1 setspectatorzone pos1`/`pos2`, et éventuellement `/cdb anneau1 setspectatorspawn` pour un point d'apparition précis.
7. `/cdb anneau1 laps 3` pour un circuit en 3 tours.
8. `/cdb anneau1 maxplayers 6` et `/cdb anneau1 minplayers 2` selon vos besoins.
9. `/cdb anneau1 info` pour vérifier que tout est bien configuré (doit afficher « oui » partout).

Les joueurs peuvent ensuite rejoindre via `/cdb join anneau1` ou via la GUI `/cdb gui`.

---

## 📝 Notes techniques

- **Aucune dépendance externe** : le plugin utilise uniquement l'API Paper/Bukkit standard (y compris `Score#numberFormat` pour masquer les nombres du sidebar, disponible depuis 1.20.3).
- **Persistance** : les courses sont sauvegardées dans `plugins/CourseDeBateau/races/<nom>.yml` (+ `races.yml` qui liste les courses existantes) et les records dans `plugins/CourseDeBateau/records/<nom>.yml` (record personnel par joueur + record global avec le nom du détenteur).
- **Verrouillage du bateau pendant `STARTING`** : combinaison de deux mécanismes — une correction **instantanée** à chaque `VehicleMoveEvent` (dès que le bateau bouge, il est immédiatement retéléporté à sa position assignée et sa vitesse annulée) et un filet de sécurité qui revérifie chaque tick. Une légère prédiction côté client est inhérente au réseau de Minecraft pour les véhicules et ne peut pas être totalement supprimée côté serveur, mais le joueur reste bloqué sur place.
- **Annulation des dégâts** : pendant `LOBBY_COUNTDOWN` / `STARTING` / `RUNNING`, les dégâts sont annulés pour éviter toute mort accidentelle en course.
- **Déconnexion** : si un joueur se déconnecte pendant qu'il est inscrit, il est automatiquement retiré de la course (`remove-on-disconnect: true` dans `config.yml`).
- **Sidebar privé** : chaque joueur a son propre `Scoreboard` (n'affecte pas le scoreboard du serveur), assigné au départ de la course et restauré automatiquement à la sortie.
- **Diamant admin** : implémenté grâce à la *persistent data container* de Bukkit (`PersistentDataType.BYTE` sur une `NamespacedKey` propre au plugin) afin que le diamant ne soit reconnu que s'il a été placé par CourseDeBateau. Il est marqué *indroppable* via `PlayerDropItemEvent` pour éviter de le perdre ou de le dupliquer.

---

## 🔄 Migration depuis une version antérieure

Si vous aviez déjà configuré des courses avec une version précédente du plugin (ou si le checkpoint #1 servait aussi de ligne de départ/arrivée), il faut redéfinir la ligne de départ/arrivée séparément avec `/cdb <course> setstartzone pos1` / `pos2` : elle n'est plus déduite automatiquement du checkpoint #1.

Pour les versions antérieures à 1.2.0 : la validation des points de passage envoyait un message de chat (« Point de passage X validé ! »). Depuis la 1.2.0, cette validation est **silencieuse** et l'information n'apparaît plus que dans le sidebar — il n'y a rien à migrer côté configuration, le comportement change simplement.

Depuis la v1.3.0, aucune migration n'est requise : le diamant admin est purement additif et n'altère aucune commande existante.

---

## 📜 Changelog

### v1.3.0 — Diamant admin de lancement instantané
- 💎 **Diamant admin « Lancer la course »** *(nouveauté)* : tout admin (`cdb.admin`) qui rejoint le lobby d'attente d'une course reçoit automatiquement un diamant nommé dans le premier slot de sa hotbar. Un clic droit force le départ immédiat (équivalent de `/cdb <course> forcestart`), en sautant le compte à rebours du lobby. Le diamant ne peut pas être jeté et disparaît dès que la course démarre réellement ou que l'admin quitte le lobby, en restaurant l'objet qui occupait le slot auparavant. Implémenté via *persistent data container* et détection du clic droit.
- 🛠️ **Mise à jour de la liste des sous-commandes** dans `plugin.yml` : ajout de `setstartzone`, `setspectatorzone`, `setspectatorspawn` dans la `usage` de la commande `cdb`.
- 🔖 **Version bump** : `1.2.0` → `1.3.0` (`pom.xml`, `plugin.yml`).

### v1.2.0 — Validation silencieuse & nettoyage d'API
- 🤫 **Validation silencieuse des points de passage** : suppression des messages de chat parasites lors du franchissement d'un checkpoint. Toute la progression reste visible dans le sidebar en direct.
- 🧹 **Nettoyage de l'import `NumberFormat`** : alignement sur le package Paper standard pour le masquage des nombres du sidebar.
- 🔖 **Version bump** : `1.1.0` → `1.2.0` (`pom.xml`, `plugin.yml`).

### v1.1.0 — Ligne de départ/arrivée distincte, zone spectateur, sidebar et records
- 🚦 Ligne de départ/arrivée distincte des checkpoints.
- 👻 Zone spectateur optionnelle.
- 🏆 Sidebar en direct avec chronos et records persistés.
- 🛡️ Points de passage anti-triche dans l'ordre.

### v1.0.0 — Première publication
- Plugin Paper 1.21 fonctionnel : arènes multiples, lobby, GUI, comptes à rebours, classement final.

---

## 📄 Licence

Ce plugin est distribué tel quel, sans garantie d'aucune sorte. Utilisez-le librement sur votre serveur Minecraft.