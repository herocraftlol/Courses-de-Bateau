# ⛵ CourseDeBateau

> **CourseDeBateau** est un plugin **Paper 1.21** qui transforme votre serveur Minecraft en un véritable arène de **courses de bateaux**. Créez vos circuits, invitez vos joueurs, donnez le top départ… et regardez-les s'affronter sur l'eau dans des courses multijoueurs où chaque centième compte.

Conçu sur le modèle de minijeux bien connus (HikaBrain, etc.), CourseDeBateau est un plugin **sans dépendance externe**, qui se configure **à 100 % en jeu** par commandes admin. Il propose plusieurs arènes simultanées, une GUI de sélection, des checkpoints **anti-triche** à valider dans l'ordre, un **lobby d'attente** avec compte à rebours, un **départ figé** dans le bateau, une **zone spectateur** optionnelle pour suivre la fin de course, et un **sidebar en direct** avec chronos, records personnels et records du serveur.

---

## 🌊 Ce que fait le plugin

CourseDeBateau ajoute à votre serveur un **mode de jeu course de bateaux** complet et clé en main. Un administrateur définit une ou plusieurs **arènes** (appelées *courses*) avec des commandes en jeu, puis les joueurs les rejoignent via une commande ou via une **interface graphique**, et s'affrontent dans des bateaux Minecraft sur des circuits personnalisés.

Chaque course peut être :

- 🗺️ **unique** (arène de sprint) ou coexister avec d'autres sur le même serveur,
- 🛶 **hautement paramétrable** : nombre de tours, min/max de joueurs, durées des comptes à rebours, ligne de départ/arrivée distincte, zone spectateur, etc.,
- 🔁 **indépendante** : plusieurs courses peuvent tourner en parallèle sans se gêner.

---

## ✨ Fonctionnalités principales

- 🏟️ **Arènes multiples et indépendantes** — Créez autant de courses que vous voulez (*anneau1*, *grand-prix*, *kart-vs-kart*…) : chacune vit sa propre vie, avec ses propres réglages et sa propre session.
- 🚦 **Ligne de départ/arrivée distincte des checkpoints** — Impossible de valider un tour en faisant l'aller-retour sur la ligne de départ : il faut avoir franchi **tous les points de passage intermédiaires** avant que la zone de départ/arrivée ne compte comme une nouvelle validation.
- 🛡️ **Points de passage anti-triche** — Les checkpoints doivent être franchis **dans l'ordre** ; le premier non-atteint est remis à zéro si on revient en arrière. Une course non configurable dans cet ordre est refusée par le plugin.
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

1. Téléchargez la dernière version du `.jar` depuis la page **[Releases](../../releases)** (par exemple `CourseDeBateau-1.1.0.jar`).
2. Déposez-le dans le dossier `plugins/` de votre serveur **Paper 1.21+**.
3. Démarrez (ou redémarrez) le serveur — le plugin se charge automatiquement.
4. (Optionnel) Personnalisez les valeurs par défaut dans `plugins/CourseDeBateau/config.yml`, créé au premier démarrage.

> Aucune dépendance externe n'est requise : le plugin utilise uniquement l'API Paper standard (y compris `Score#numberFormat` pour masquer les nombres du sidebar, disponible depuis 1.20.3).

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
| `/cdb delete <course>` | Supprime définitivement une course (et ses records). |
| `/cdb reload` | Recharge `config.yml` + toutes les courses. |

### ⚙️ Configuration d'une course (`/cdb <course> ...`)

| Commande | Description |
|---|---|
| `/cdb <course> info` | Affiche l'état complet de la configuration. |
| `/cdb <course> setlobby` | Définit le point de lobby à votre position. |
| `/cdb <course> addboatspawn` | Ajoute un spawn de bateau à votre position (dans l'ordre d'arrivée des joueurs). |
| `/cdb <course> removeboatspawn <index>` | Retire un spawn de bateau. |
| `/cdb <course> setstartzone pos1` | 1er coin de la **zone de départ/arrivée** (vous visez un bloc, jusqu'à ~150 blocs). |
| `/cdb <course> setstartzone pos2` | 2e coin (finalise la zone). |
| `/cdb <course> setcheckpoint <index> pos1` | 1er coin d'un **point de passage** intermédiaire (vous visez un bloc). |
| `/cdb <course> setcheckpoint <index> pos2` | 2e coin (finalise la zone). |
| `/cdb <course> removecheckpoint <index>` | Retire un point de passage. |
| `/cdb <course> setspectatorzone pos1` | 1er coin de la **zone spectateur** (vous visez un bloc). |
| `/cdb <course> setspectatorzone pos2` | 2e coin (finalise la zone). |
| `/cdb <course> setspectatorspawn` | Point d'apparition spectateur, à votre position. |
| `/cdb <course> laps <n>` | Nombre de tours à effectuer. |
| `/cdb <course> maxplayers <n>` | Nombre maximum de joueurs pour cette course. |
| `/cdb <course> minplayers <n>` | Nombre minimum de joueurs pour cette course. |
| `/cdb <course> lobbycountdown <secondes>` | Durée du compte à rebours dans le lobby. |
| `/cdb <course> startcountdown <secondes>` | Durée du compte à rebours **figé dans le bateau** avant le départ. |
| `/cdb <course> forcestart` | Force le départ immédiat (saute le compte à rebours du lobby). |
| `/cdb <course> stop` | Arrête et réinitialise la course. |

---

## 🏁 Mise en place rapide d'une course

Voici comment créer une course appelée `anneau1` en quelques minutes :

1. `/cdb create anneau1`
2. Placez-vous au point d'attente souhaité puis tapez `/cdb anneau1 setlobby`.
3. Pour chaque emplacement de départ (un par coureur potentiel), positionnez-vous sur l'emplacement et tapez `/cdb anneau1 addboatspawn`. L'ordre d'ajout définit l'ordre d'attribution des bateaux.
4. Définissez la **zone de départ/arrivée** (distincte des checkpoints) : visez un coin et tapez `/cdb anneau1 setstartzone pos1`, puis visez le coin opposé et tapez `/cdb anneau1 setstartzone pos2`.
5. Recommencez pour chaque **point de passage intermédiaire** (1, 2, 3, …) qui forme le circuit : `/cdb anneau1 setcheckpoint 1 pos1` puis `pos2`, etc.
6. *(Optionnel mais recommandé)* Définissez la **zone spectateur** : `/cdb anneau1 setspectatorzone pos1` / `pos2`, et éventuellement `/cdb anneau1 setspectatorspawn` pour un point d'apparition précis.
7. `/cdb anneau1 laps 3` pour un circuit en 3 tours.
8. `/cdb anneau1 maxplayers 6` et `/cdb anneau1 minplayers 2` pour surcharger les valeurs par défaut.
9. `/cdb anneau1 info` pour vérifier que tout est bien configuré (tout doit afficher *oui*).

Les joueurs peuvent ensuite rejoindre via `/cdb join anneau1` ou via la **GUI** (`/cdb gui`).

---

## 🆕 Nouveautés de la v1.1.0

Cette version apporte un grand nombre d'évolutions par rapport à la v1.0.0 :

- 🆕 **Ligne de départ/arrivée distincte des checkpoints** — Fini le *checkpoint #1 = ligne de départ*. La zone de validation de tour se configure maintenant séparément avec `setstartzone pos1` / `pos2`, et les checkpoints sont purement intermédiaires.
- 🆕 **Zone spectateur optionnelle** — Les joueurs ayant terminé peuvent rester en mode spectateur dans une zone confinée (`setspectatorzone`), avec un point d'apparition précis (`setspectatorspawn`).
- 🆕 **Sidebar en direct** — Pendant la course, chaque joueur voit son tour, son temps de tour, son temps total, son record personnel et le record du serveur avec le nom du détenteur.
- 🆕 **Records persistés** — Records personnels par joueur et meilleur temps global du serveur, sauvegardés dans `plugins/CourseDeBateau/records/<course>.yml` et mis à jour à chaque arrivée.
- 🆕 **Verrouillage instantané du bateau** — Pendant la phase `STARTING`, le bateau est recadré **instantanément** à chaque `VehicleMoveEvent` (correction immédiate + filet de sécurité chaque tick), garantissant un blocage parfait avant le top départ.
- 🆕 **Sélection de zones à distance** — Visez un bloc jusqu'à 150 blocs pour définir les coins des zones : fini d'avoir à se coller au bloc.
- 🆕 **Spawns bateau avec orientation** — Le yaw et le pitch du bateau au moment de `addboatspawn` sont enregistrés pour que le bateau pointe dans la bonne direction au départ.
- 🆕 **`/cdb <course> forcestart`** — Permet de sauter le compte à rebours du lobby pour lancer une course immédiatement.
- 🆕 **Persistance plus propre** — `races.yml` est recréé proprement au démarrage, sans fichiers vides parasites.

---

## 🧱 Compilation depuis les sources

Le projet est un module Maven standard :

```bash
mvn clean package
```

Le `.jar` final est généré dans `target/CourseDeBateau-1.1.0.jar`.

> Si votre serveur tourne sur un autre patch de la 1.21, ajustez la dépendance `io.papermc.paper:paper-api` dans `pom.xml` (par défaut `1.21.4-R0.1-SNAPSHOT`). Seule la valeur `api-version: 1.21` du `plugin.yml` compte vraiment pour la compatibilité en jeu.

---

## 🔧 Notes techniques

- **Aucune dépendance externe** : uniquement l'API Paper standard.
- **Persistance** :
  - `plugins/CourseDeBateau/races/<nom>.yml` — configuration de chaque course
  - `plugins/CourseDeBateau/races/races.yml` — registre des courses existantes
  - `plugins/CourseDeBateau/records/<nom>.yml` — record personnel par joueur + record global du serveur avec le nom du détenteur
- **Verrouillage du bateau pendant `STARTING`** — combinaison de deux mécanismes :
  - une correction **instantanée** à chaque `VehicleMoveEvent` (le bateau est immédiatement retéléporté à sa position assignée, vitesse annulée),
  - un filet de sécurité qui revérifie chaque tick.
  Une légère prédiction côté client est inhérente au réseau de Minecraft pour les véhicules et ne peut pas être totalement supprimée côté serveur, mais le joueur reste **effectivement bloqué** sur place.
- **Verrouillage pendant `RUNNING`** — annulation de `VehicleExitEvent` ; une exception est faite via un *jeton à usage unique* posé par `RaceSession` juste avant un éjectement programmatique (sortie via `/cdb leave` ou fin de course).
- **Annulation des dégâts** — `EntityDamageEvent` annulé pendant `LOBBY_COUNTDOWN`/`STARTING`/`RUNNING` pour éviter toute mort accidentelle en course.
- **Déconnexion** — `PlayerQuitEvent` retiré proprement de la session (réglé par `remove-on-disconnect: true` dans `config.yml`).
- **Sidebar privé** — scoreboard privé par joueur (n'affecte pas le scoreboard serveur), assigné au départ de la course et restauré automatiquement à la sortie.
- **Spawns bateau plafonnés** — la capacité maximale effective d'une course est `min(maxplayers configuré, nombre de spawns bateaux réellement définis)`.

---

## 🔄 Migration depuis la v1.0.0

Si vous aviez déjà configuré des courses avec la **v1.0.0** (où le *checkpoint #1* servait aussi de ligne de départ/arrivée), il faut **redéfinir** la ligne de départ/arrivée séparément avec `/cdb <course> setstartzone pos1` / `pos2` : elle n'est plus déduite automatiquement du checkpoint #1. Vos anciens checkpoints restent valides en tant que points de passage intermédiaires.

---

## 📜 Licence

Ce plugin est fourni tel quel, sans garantie. Vous êtes libre de l'utiliser, le modifier et le redistribuer sur votre serveur.