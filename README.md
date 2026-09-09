# ⛵ CourseDeBateau

> Plugin **Paper 1.21** qui transforme votre serveur Minecraft en véritable arène de **courses de bateaux** : créez vos circuits, invitez vos joueurs, donnez le top départ, et regardez-les s'affronter sur l'eau !

CourseDeBateau est un minijeu clé en main, sans dépendance externe, qui ajoute à votre serveur un système complet de courses multijoueurs en bateau, avec arènes multiples, lobby d'attente, comptes à rebours, points de passage anti-triche et interface graphique de sélection.

---

## ✨ Ce que fait le plugin

CourseDeBateau ajoute à votre serveur un **mode de jeu course de bateaux** complet, inspiré du fonctionnement général de HikaBrain : un administrateur conﬁgure une ou plusieurs **arènes** (appelées *courses*) à l'aide de commandes en jeu, puis les joueurs peuvent les rejoindre via une commande ou une GUI, et s'affronter dans des bateaux Minecraft le long de circuits personnalisés.

Chaque course peut être :
- 🗺️ **unique** (arène de sprint) ou partagée avec d'autres sur le même serveur,
- 🛶 **paramétrable** (nombre de tours, min/max de joueurs, durées des comptes à rebours, etc.),
- 🔁 **indépendante** (plusieurs courses peuvent coexister, même si elles sont vides).

---

## 🚀 Fonctionnalités principales

- **Arènes multiples et indépendantes** — Créez autant de courses que vous voulez : *anneau1*, *grand-prix*, *kart-vs-kart*… chacune avec ses propres réglages.
- **Points de passage (checkpoints) anti-triche** — Les joueurs doivent valider les checkpoints **dans l'ordre** : impossible de valider un tour en faisant l'aller-retour sur la ligne de départ. Le *checkpoint #1* sert à la fois de ligne de **départ** et de ligne d'**arrivée**.
- **Lobby d'attente avec compte à rebours** — Une fois le nombre minimum de joueurs atteint, un compte à rebours se lance avant la téléportation. S'il manque du monde, il s'annule automatiquement.
- **Départ figé dans le bateau** — Juste avant le top départ, les joueurs sont assis dans leur bateau et gelés quelques secondes (durée configurable) avant que la course ne démarre vraiment.
- **GUI de sélection** — Une commande `/cdb gui` ouvre un menu visuel listant toutes les courses disponibles avec leur état, leur nombre de joueurs et leur configuration. Un clic pour rejoindre ou quitter.
- **Spawns bateau individuels** — L'administrateur définit un **point de spawn de bateau par joueur** (un par emplacement disponible), garantissant que chaque coureur a sa propre embarcation au départ.
- **Verrouillage en course** — Les joueurs **ne peuvent pas sortir** de leur bateau pendant la course. Seule la commande `/cdb leave` (ou la fin de la course) permet de quitter son embarcation.
- **Protection pendant la course** — Les dégâts sont **annulés** pendant les phases lobby/départ/course pour éviter toute mort accidentelle au milieu du circuit.
- **Reconnexion / déconnexion** — Un joueur qui se déconnecte est automatiquement retiré de la course (comme s'il avait tapé `/cdb leave`).
- **Classement final** — À la fin, le classement des finishers est annoncé dans le chat, suivi d'un reset automatique.
- **Persistance simple** — Chaque course est sauvegardée dans son propre fichier YAML dans `plugins/CourseDeBateau/races/<nom>.yml`.
- **Commandes admin complètes** — Toute la configuration se fait en jeu, à la position de l'administrateur, sans jamais toucher à la main.

---

## 🧩 Cycle de vie d'une course

```
WAITING (en attente du min de joueurs)
   ↓ (min atteint)
LOBBY_COUNTDOWN (compte à rebours dans le lobby d'attente)
   ↓ (compte à rebours terminé)
STARTING (joueurs téléportés dans leur bateau, gelés, compte à rebours de départ)
   ↓ (top départ)
RUNNING (course en cours, checkpoints à valider dans l'ordre)
   ↓ (dernier coureur a terminé)
ENDING (classement affiché, reset automatique)
   ↓
WAITING (prêt pour la prochaine manche)
```

---

## 📦 Installation

1. Téléchargez la dernière version du `.jar` depuis la page **[Releases](../../releases)**.
2. Déposez-le dans le dossier `plugins/` de votre serveur **Paper 1.21+**.
3. Démarrez (ou redémarrez) le serveur — le plugin se charge automatiquement.
4. (Optionnel) Personnalisez les valeurs par défaut dans le fichier `plugins/CourseDeBateau/config.yml` qui sera créé au premier démarrage.

> Aucune dépendance externe n'est requise : le plugin utilise uniquement l'API Paper standard.

---

## 🎮 Commandes joueur

| Commande | Description |
|---|---|
| `/cdb gui` | Ouvre le menu visuel des courses disponibles. |
| `/cdb list` | Liste toutes les courses et leur état actuel. |
| `/cdb join <course>` | Rejoindre une course spécifique. |
| `/cdb leave` | Quitter la course en cours (seul moyen de sortir du bateau). |
| `/cdb help` | Affiche l'aide complète (inclut les commandes admin si vous avez la permission). |

---

## 🛠️ Commandes admin (permission `cdb.admin`, op par défaut)

### Gestion des courses

| Commande | Description |
|---|---|
| `/cdb create <course>` | Crée une nouvelle course vide. |
| `/cdb delete <course>` | Supprime définitivement une course. |
| `/cdb reload` | Recharge `config.yml` + toutes les courses. |

### Configuration d'une course (`/cdb <course> ...`)

| Commande | Description |
|---|---|
| `/cdb <course> info` | Affiche l'état de configuration de la course. |
| `/cdb <course> setlobby` | Définit le point de lobby à votre position. |
| `/cdb <course> addboatspawn` | Ajoute un spawn de bateau à votre position (dans l'ordre). |
| `/cdb <course> removeboatspawn <index>` | Retire un spawn de bateau. |
| `/cdb <course> setcheckpoint <index> pos1` | Définit le 1ᵉʳ coin du checkpoint *(vous visez un bloc, distance max ~5 blocs)*. |
| `/cdb <course> setcheckpoint <index> pos2` | Définit le 2ᵉ coin (finalise la zone). |
| `/cdb <course> removecheckpoint <index>` | Retire un checkpoint. |
| `/cdb <course> laps <n>` | Nombre de tours à effectuer. |
| `/cdb <course> maxplayers <n>` | Nombre maximum de joueurs pour cette course. |
| `/cdb <course> minplayers <n>` | Nombre minimum de joueurs pour cette course. |
| `/cdb <course> lobbycountdown <secondes>` | Durée du compte à rebours du lobby. |
| `/cdb <course> startcountdown <secondes>` | Durée du compte à rebours avant le départ. |
| `/cdb <course> forcestart` | Force le départ immédiat (saute le compte à rebours du lobby). |
| `/cdb <course> stop` | Arrête et réinitialise la course. |

> Alias de la commande : `/coursedebateau` et `/boatrace`.

---

## 🏁 Mise en place rapide d'une course

Voici comment créer une course appelée `anneau1` en moins d'une minute :

1. `/cdb create anneau1`
2. Placez-vous au point d'attente souhaité puis tapez `/cdb anneau1 setlobby`.
3. Pour chaque emplacement de départ (un par coureur potentiel), positionnez-vous sur l'emplacement et tapez `/cdb anneau1 addboatspawn`. L'ordre d'ajout définit l'ordre d'attribution des bateaux aux joueurs (premier arrivé, premier servi).
4. Définissez le **checkpoint #1** (ligne de départ/arrivée) : visez un coin de la zone et tapez `/cdb anneau1 setcheckpoint 1 pos1`, puis visez le coin opposé et tapez `/cdb anneau1 setcheckpoint 1 pos2`.
5. Recommencez pour tous les **checkpoints suivants** (2, 3, …) qui formeront le circuit.
6. `/cdb anneau1 laps 3` pour un circuit en 3 tours.
7. `/cdb anneau1 maxplayers 6` et `/cdb anneau1 minplayers 2` si vous voulez surcharger les valeurs par défaut.
8. `/cdb anneau1 info` pour vérifier que tout est bien configuré.

Les joueurs peuvent ensuite rejoindre via `/cdb join anneau1` ou via la GUI (`/cdb gui`).

---

## 🧱 Compilation depuis les sources

Le projet est un module Maven standard :

```bash
mvn clean package
```

Le `.jar` final est généré dans `target/CourseDeBateau-1.0.0.jar`.

> Si votre serveur tourne sur un autre patch de la 1.21, ajustez la dépendance `io.papermc.paper:paper-api` dans `pom.xml` (par défaut `1.21.4-R0.1-SNAPSHOT`). Seule la valeur `api-version: 1.21` du `plugin.yml` importe vraiment pour la compatibilité en jeu.

---

## 📝 Notes techniques

- **Aucune dépendance externe** : uniquement l'API Paper standard.
- **Persistance** : `plugins/CourseDeBateau/races/<nom>.yml` (+ `races.yml` qui liste les courses existantes).
- **Verrouillage du bateau** : annulation de `VehicleExitEvent` pendant `STARTING`/`RUNNING`. Une exception est faite via un *jeton à usage unique* posé par `RaceSession` juste avant un ejectement programmatique (sortie via `/cdb leave` ou fin de course).
- **Annulation des dégâts** : `EntityDamageEvent` annulé pendant `LOBBY_COUNTDOWN`/`STARTING`/`RUNNING` pour éviter toute mort accidentelle en course.
- **Déconnexion** : `PlayerQuitEvent` retiré proprement de la session (réglé par `remove-on-disconnect: true` dans `config.yml`).
- **Spawns bateau plafonnés** : la capacité maximale effective d'une course est `min(maxplayers configuré, nombre de spawns bateaux réellement définis)`.

---

## 📜 Licence

Ce plugin est fourni tel quel, sans garantie. Vous êtes libre de l'utiliser, le modifier et le redistribuer sur votre serveur.
