# Guide d'installation : récupérer et lancer QA Reporting

Ce guide explique, étape par étape, comment récupérer le projet depuis GitHub et le lancer sur votre PC Windows. Il ne demande aucune connaissance préalable de WildFly.

Temps estimé : **20 à 30 minutes** la première fois, surtout pour les téléchargements. Ensuite, un lancement prend environ 1 minute.

---

## Étape 0 : installer les outils (une seule fois)

Il vous faut quatre outils. Si vous en avez déjà un, vérifiez seulement sa version avec la commande indiquée.

### 1. Git

- Téléchargez-le sur https://git-scm.com/download/win et installez-le en laissant les options par défaut.
- Vérifiez l'installation : `git --version`

### 2. Java JDK 17 (ou plus récent)

- Téléchargez-le sur https://adoptium.net (Temurin 17 ou 21, installeur `.msi`).
- **Pendant l'installation, activez les options « Set JAVA_HOME variable » et « Add to PATH ».**
- Vérifiez l'installation : `java -version` doit afficher `17` ou plus.

### 3. Maven

- Téléchargez le fichier `apache-maven-3.x.x-bin.zip` sur https://maven.apache.org/download.cgi.
- Décompressez-le, par exemple dans `C:\maven`.
- Ajoutez `C:\maven\bin` au **PATH** :
  1. Menu Démarrer : tapez « variables d'environnement ».
  2. Cliquez sur **Variables d'environnement**.
  3. Sélectionnez `Path`, puis **Modifier**, **Nouveau**, et collez `C:\maven\bin`.
- **Fermez puis rouvrez le terminal**, puis vérifiez : `mvn -version`

### 4. XAMPP (pour la base de données MariaDB)

- Téléchargez-le sur https://www.apachefriends.org et installez-le dans `C:\xampp` (valeur par défaut).
- Vous n'utiliserez que **MySQL** (qui est en réalité MariaDB). Apache n'est pas nécessaire.

> Pour tester chaque commande de vérification, ouvrez **un nouveau terminal** (PowerShell ou Invite de commandes) après chaque installation. Sinon, Windows ne voit pas les nouveaux programmes.

---

## Étape 1 : télécharger le projet

Ouvrez un terminal dans le dossier où vous voulez mettre le projet, par exemple le Bureau :

```bash
cd %USERPROFILE%\Desktop
git clone https://github.com/<compte>/qa-reporting-j2ee.git
cd qa-reporting-j2ee
```

Remplacez `<compte>` par le nom du compte GitHub qui a partagé le projet.

*Sans Git* : sur la page GitHub, cliquez sur **Code**, puis **Download ZIP**, puis décompressez l'archive.

---

## Étape 2 : démarrer la base de données

1. Ouvrez le **XAMPP Control Panel**.
2. Sur la ligne **MySQL**, cliquez sur **Start**. La ligne doit devenir verte.

## Étape 3 : créer la base (une seule fois)

1. Dans XAMPP, cliquez sur **Shell**, ou ouvrez un terminal dans `C:\xampp\mysql\bin`.
2. Tapez :

```bash
mysql -u root
```

3. Puis, dans l'invite MariaDB :

```sql
CREATE DATABASE qa_reporting_j2ee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

*Autre méthode* : démarrez aussi Apache dans XAMPP, allez sur http://localhost/phpmyadmin, cliquez sur **Nouvelle base de données**, saisissez le nom `qa_reporting_j2ee` et choisissez l'interclassement `utf8mb4_unicode_ci`.

Vous n'avez **rien d'autre à faire** : au premier lancement, l'application crée les tables (migrations Flyway, voir « Pour aller plus loin ») puis les données de départ.

> Si votre utilisateur `root` MySQL a un mot de passe, indiquez-le dans le fichier `src/main/webapp/WEB-INF/qa-reporting-ds.xml`, dans la balise `<password></password>`.

---

## Étape 4 : lancer l'application

Dans le terminal, placez-vous dans le dossier du projet (celui qui contient `pom.xml`) :

```bash
mvn wildfly:run
```

- **Le premier lancement est long (5 à 15 minutes)** : Maven télécharge le serveur WildFly et toutes les dépendances. Il faut une connexion internet.
- Attendez que le terminal affiche une ligne qui contient `Deployed "qa-reporting-j2ee.war"` ou `WildFly ... started`.
- **Ne fermez pas ce terminal** : le site fonctionne tant qu'il reste ouvert. Pour arrêter le site, appuyez sur `Ctrl + C`.

Pour les lancements suivants, vous pouvez travailler sans internet, ce qui est plus rapide :

```bash
mvn -o wildfly:run
```

---

## Étape 5 : ouvrir le site

Ouvrez votre navigateur à l'adresse **http://127.0.0.1:8080/qa-reporting-j2ee/**

Connectez-vous avec le compte administrateur créé automatiquement :

| Email | Mot de passe |
|---|---|
| `admin@qa-reporting-j2ee.local` | `password` |

**À la première connexion, l'application vous demande de choisir un nouveau mot de passe** : `password` n'est qu'un mot de passe provisoire. Toutes les pages restent bloquées tant qu'il n'est pas changé. Le nouveau mot de passe doit faire au moins 10 caractères, avec au moins une lettre et un chiffre.

Depuis ce compte, vous pouvez créer d'autres utilisateurs (QA, QA Lead, Manager) pour tester les différents rôles. Le mot de passe que vous leur donnez est lui aussi **provisoire** : chacun le remplace à sa première connexion.

La langue se change avec le sélecteur **FR | EN**, en haut à droite (et sur la page de connexion). Le choix est mémorisé.

---

## Étape 6 : préparer l'application (première utilisation)

L'application démarre vide, avec seulement le compte admin.

> **Pour une démonstration**, inutile de tout saisir : en admin, **Administration → Paramètres généraux → Charger les données de démonstration** crée une équipe complète avec deux semaines d'historique. Les comptes et le déroulé sont dans [SCENARIO-DEMO.md](SCENARIO-DEMO.md).

Pour une vraie équipe, faites les étapes suivantes **dans cet ordre** (les suivantes ont besoin des précédentes) :

| # | Qui | Où | Quoi |
|---|---|---|---|
| 1 | Admin | Administration → **Équipes** | Créer l'équipe QA (ex. « Équipe Paiement »). |
| 2 | Admin | Administration → **Utilisateurs** | Créer les comptes : un **QA Lead**, un ou plusieurs **QA**, un **Manager**. Rattacher le lead et les QA à l'équipe. |
| 3 | Admin | Administration → **Équipes** | Modifier l'équipe pour lui donner son responsable (le QA Lead). |
| 4 | Admin | Administration → **Projets** | Créer le projet et le rattacher à l'équipe. **Sans équipe, le projet n'apparaît pas chez le QA Lead.** Cocher aussi les **testeurs affectés** : un QA peut être affecté à plusieurs projets, y compris ceux d'une autre équipe. |
| 5 | QA Lead | **Campagnes** → Nouvelle campagne | Créer la campagne du sprint / de la version, puis **Démarrer**. Les tests d'une campagne ne s'exécutent que lorsqu'elle est **en cours**. |
| 6 | QA Lead | **Tests** → Nouveau test | Créer les cas de test et les **assigner** aux testeurs. |

Les environnements (DEV, QA, PREPROD, PROD) et les paramètres généraux sont créés automatiquement au premier lancement.

---

## Utiliser l'application selon son rôle

Chaque rôle ne voit que les menus et les boutons qu'il a le droit d'utiliser. Une page interdite affiche « Accès refusé » (403) ; un élément qui n'est pas dans votre périmètre affiche « Introuvable » (404).

### QA (testeur)

Voit ses tests, ses anomalies et son activité.

1. **Tests** : ouvrir un test qui vous est assigné (la barre de recherche filtre par titre, projet, campagne ou statut), choisir le **Résultat** (réussi, échoué, bloqué, non exécuté), puis **Enregistrer le résultat**. Si la campagne n'est pas démarrée, est bloquée ou est terminée, l'exécution est refusée avec l'explication.
2. **Si le test échoue** : un encadré rouge propose **Déclarer l'anomalie**. Le formulaire est déjà rempli (projet, étapes, résultat attendu, résultat obtenu) : complétez puis **Créer**.
3. **Anomalies** → ouvrir l'anomalie → **Pièces jointes** : ajouter une capture, une vidéo ou un journal (10 Mo maximum).
4. **Nouveau test** : vous pouvez rédiger vos propres cas de test. Ils vous sont assignés automatiquement.
5. En fin de journée : **Mon activité** → **Nouvelle activité**. La somme réussis + échoués + bloqués + non exécutés doit être égale au nombre de tests exécutés, et un motif est obligatoire s'il y a des tests bloqués. Une fois validée par le QA Lead, la déclaration n'est plus modifiable. Dans **Paramètres**, le **rappel de saisie** vous envoie une notification à l'heure choisie si vous n'avez rien déclaré dans la journée.
6. Cycle de vie d'une anomalie, depuis sa fiche :

   | Statut | Bouton | Nouveau statut |
   |---|---|---|
   | Ouverte / Rouverte | **Prendre en charge** | En cours |
   | Ouverte / En cours / Rouverte | **Marquer corrigée** (le développeur a livré le correctif) | Corrigée |
   | Corrigée | **Commencer la revérification** | À revérifier |
   | Corrigée / À revérifier | **Revérification : réussie** | Fermée |
   | Corrigée / À revérifier | **Revérification : échouée** | Rouverte |
   | Tout statut sauf Fermée | **Fermer**, avec un **motif obligatoire** dans le commentaire (doublon, non reproductible…) | Fermée |
   | Fermée | **Rouvrir** | Rouverte |

### QA Lead (chef d'équipe)

Voit tout ce qui concerne son équipe.

- **Campagnes** : créer, démarrer, bloquer, reprendre, terminer. L'équipe reçoit une notification à chaque changement.
- **Tests** : créer les cas de test et les répartir entre testeurs (champ « Assigné à »).
- **Anomalies** : les assigner à un testeur (bloc **Assigner à** sur la fiche). Seuls les testeurs de l'équipe du projet sont proposés.
- **Activité de l'équipe** : valider les déclarations des testeurs (bouton **Valider**). La tuile jaune **Activités à valider** du tableau de bord en donne le nombre et ouvre la liste filtrée.
- **Rapports** : activité de l'équipe par jour / semaine / mois, avec le détail **Par testeur**.
- **Mon équipe** : anomalies assignées et créées par membre.

### Manager

Consultation seulement, sur tous les projets : **Vue d'ensemble** (indicateurs et graphiques) et **Rapports** (activité et anomalies, export CSV). Il ne crée ni ne modifie rien.

### Admin

Tout ce que font les autres rôles, plus l'**Administration** : utilisateurs, rôles, projets, équipes, environnements, paramètres généraux, et le **Journal d'audit** (qui a fait quoi et quand ; rien n'y est jamais supprimé). Il est le seul à pouvoir supprimer des éléments.

---

## Récapitulatif : à chaque fois que vous voulez relancer le site

1. XAMPP : **MySQL**, puis **Start**.
2. Terminal dans le dossier du projet : `mvn -o wildfly:run`
3. Navigateur : http://127.0.0.1:8080/qa-reporting-j2ee/

## Récupérer les dernières modifications

```bash
git pull
```

Relancez ensuite l'application (étape 4). Si de nouvelles dépendances ont été ajoutées, faites un lancement **sans** `-o` la première fois.

---

## Problèmes fréquents

| Symptôme | Cause et solution |
|---|---|
| `'mvn' n'est pas reconnu…` | Maven n'est pas dans le PATH (voir l'étape 0.3). Rouvrez ensuite le terminal. |
| `'java' n'est pas reconnu…` ou une erreur de version | Installez le JDK 17 ou plus, en cochant « Add to PATH ». |
| `Unknown database 'qa_reporting_j2ee'` | La base n'a pas été créée (voir l'étape 3). |
| `Communications link failure` ou WildFly reste bloqué | MySQL n'est pas démarré dans XAMPP. |
| MySQL ne démarre pas dans XAMPP (port 3306) | Un autre MySQL tourne déjà sur le PC. Arrêtez-le dans les Services Windows. |
| `Address already in use` ou port 8080 occupé | Un autre serveur (Tomcat, un autre WildFly, etc.) utilise le port 8080. Arrêtez-le. |
| Erreur de téléchargement avec `-o` | Au tout premier lancement, retirez `-o` : il faut internet. |
| La page affiche 404 | Vérifiez l'URL : elle doit se terminer par `/qa-reporting-j2ee/`. |
| « Accès refusé » (403) sur une page | Votre rôle n'y a pas droit (voir « Utiliser l'application selon son rôle »). |
| Un QA Lead ne voit aucun projet, test ou campagne | Le projet n'est rattaché à aucune équipe, ou le lead n'est pas membre de l'équipe (voir l'étape 6). |
| Un QA ne voit pas un test | Le test ne lui est pas assigné : demandez au QA Lead de l'assigner. |
| Un QA ne voit pas un projet (campagnes, liste des projets de l'activité) | Il n'est ni dans l'équipe du projet ni affecté au projet : **Administration → Projets → Modifier → Testeurs affectés**. |
| « Quelqu'un a modifié cet élément pendant que vous l'éditiez » | Un collègue a enregistré le même élément juste avant vous : la page est rechargée avec sa version, refaites votre modification. |
| Mot de passe oublié | Lien **Mot de passe oublié ?** sur la page de connexion. Sans serveur d'e-mails configuré (voir « Mise en production »), le lien est écrit dans `target\server\standalone\log\server.log`. |
| Au démarrage : « Le schéma de la base ne correspond pas aux entités Java » | Une entité a été modifiée sans migration : ajoutez un fichier `V<n>__description.sql` (voir « Pour aller plus loin »). |
| « Ce test ne peut pas être exécuté maintenant » | Sa campagne n'est pas **en cours** : le QA Lead doit la démarrer (ou la reprendre si elle est bloquée). Une campagne terminée est figée. |
| « Fermer » affiche un message au lieu de fermer | Fermer sans revérification demande un motif dans le commentaire. |
| « Trop de tentatives de connexion » | 5 mots de passe faux de suite bloquent le compte 15 minutes (20 échecs bloquent l'adresse IP). Attendez, ou redémarrez le serveur en dépannage. |
| Toujours renvoyé vers « Paramètres » après connexion | Votre mot de passe est provisoire : changez-le dans la section Sécurité. |
| Un compte ne peut plus se connecter (« désactivé ») | Un admin l'a désactivé : **Administration → Utilisateurs → Réactiver**. Les comptes ne sont jamais supprimés, pour garder l'historique. |
| Erreur 500 avec `Unresolved compilation problem` dans le terminal | L'éditeur (VS Code, Eclipse) a compilé une version cassée d'un fichier. Supprimez **uniquement** le dossier `target/classes`, puis relancez. |

---

## Mise en production (serveur d'entreprise)

L'installation ci-dessus convient à un PC de développement ou à une démonstration. Sur un vrai serveur :

### 1. Identifiants de la base : variables d'environnement

Ne mettez aucun mot de passe dans le code. Créez un utilisateur MariaDB dédié (pas `root`), avec les seuls droits sur la base `qa_reporting_j2ee`, puis définissez ces variables avant de lancer WildFly :

| Variable | Rôle | Valeur par défaut (développement) |
|---|---|---|
| `QA_DB_HOST` / `QA_DB_PORT` / `QA_DB_NAME` | Adresse de la base | `127.0.0.1` / `3306` / `qa_reporting_j2ee` |
| `QA_DB_USER` / `QA_DB_PASSWORD` | Compte de la base | `root` / vide |
| `QA_FORCE_HTTPS` | `true` : toute requête HTTP est redirigée vers HTTPS | désactivé |
| `QA_SMTP_HOST` / `QA_SMTP_PORT` | Serveur d'e-mails (mot de passe oublié, rappels) | aucun : l'e-mail est écrit dans le journal / `587` |
| `QA_SMTP_USER` / `QA_SMTP_PASSWORD` | Compte d'envoi | aucun |
| `QA_SMTP_FROM` / `QA_SMTP_TLS` | Expéditeur / chiffrement STARTTLS | le compte d'envoi / `true` |
| `QA_PUBLIC_URL` | Adresse publique utilisée dans les liens des e-mails (derrière un proxy) | déduite de la requête |
| `QA_HTTPS_PORT` | Port HTTPS pour la redirection | `8443` |

### 2. HTTPS

WildFly écoute déjà en HTTPS sur le port **8443**, avec un certificat auto-signé généré au premier accès : https://127.0.0.1:8443/qa-reporting-j2ee/ (le navigateur affiche un avertissement, c'est normal). En production :
1. installez le **certificat de votre organisation** dans le magasin de clés `applicationKS` de WildFly (ou placez l'application derrière un reverse proxy — IIS, Nginx, Apache — qui porte le certificat) ;
2. définissez `QA_FORCE_HTTPS=true`.

Le cookie de session est déjà protégé : `HttpOnly`, `SameSite=Lax`, et `Secure` dès que la connexion est en HTTPS. Les en-têtes de sécurité (anti-clickjacking, CSP, HSTS en HTTPS…) sont envoyés sur toutes les pages.

### 3. Sauvegardes automatiques

Le script `scripts\sauvegarde.ps1` sauvegarde la base **et** les pièces jointes dans une archive zip datée, et garde les 14 dernières. Pour le lancer chaque nuit :
1. **Planificateur de tâches** → *Créer une tâche de base* → quotidienne, 2 h du matin ;
2. action : *Démarrer un programme* → `powershell.exe`, arguments : `-ExecutionPolicy Bypass -File "C:\chemin\du\projet\scripts\sauvegarde.ps1"`.

Copiez régulièrement le dossier `backups` **hors du serveur** (autre disque, stockage réseau) : une sauvegarde sur la même machine ne protège pas d'une panne de disque. Testez une restauration de temps en temps.

### 4. Ce que fait déjà l'application

- Mots de passe hachés (BCrypt) ; 10 caractères minimum avec lettre et chiffre ; mot de passe **provisoire** à changer à la première connexion.
- Blocage de 15 minutes après 5 échecs de connexion sur un compte (ou 20 depuis une même adresse IP).
- Nouvel identifiant de session à chaque connexion ; déconnexion automatique après 30 minutes d'inactivité.
- Jetons d'API valables 12 heures au plus, 2 heures sans utilisation ; la déconnexion les révoque.
- Comptes désactivés (jamais supprimés) : connexion refusée, sessions et jetons coupés immédiatement.
- Chaque modification est tracée dans le journal d'audit.

### 5. Reste à la charge de l'exploitation

- **Évolutions du schéma de base** : gérées par des migrations Flyway versionnées, appliquées automatiquement au démarrage (voir « Pour aller plus loin »). **Sauvegardez quand même avant chaque mise à jour de l'application** : une migration MariaDB ne s'annule pas.
- **Supervision** : surveiller le journal `standalone\log\server.log`, l'espace disque et la mémoire.
- **Lancer WildFly en service Windows** plutôt que par `mvn wildfly:run` : déployer le fichier `target\qa-reporting-j2ee.war` sur un WildFly installé comme service.

---

## Pour aller plus loin

- **Tests unitaires** : `mvn test` (48 tests : mots de passe, jetons, droits par rôle, portée des données et affectations multi-projets, règles de campagne, d'anomalie et d'activité, recherche…)
- **Sauvegarder la base** avant une opération risquée : `C:\xampp\mysql\bin\mysqldump -u root qa_reporting_j2ee > backups\sauvegarde.sql` (le dossier `backups/` est ignoré par Git). Pour restaurer : `mysql -u root qa_reporting_j2ee < backups\sauvegarde.sql`.
- **Modifier la structure de la base (migrations Flyway)** : Hibernate ne touche plus au schéma. Pour ajouter une colonne, une table, etc. :
  1. modifiez l'entité Java ;
  2. créez `src/main/resources/db/migration/V<n>__description.sql` (numéro suivant le dernier, ex. `V4__ajout_champ_navigateur.sql`) avec l'instruction SQL ;
  3. relancez : Flyway applique le script **une seule fois**, puis l'application vérifie que les tables correspondent aux entités (sinon elle refuse de démarrer, avec le nom de la colonne en cause).

  **Ne modifiez jamais une migration déjà appliquée** : ajoutez-en une nouvelle. L'historique est dans la table `flyway_schema_history`.
- **Ne lancez pas `mvn clean`** sans raison : il efface aussi le serveur WildFly installé dans `target/server`, qui sera retéléchargé (il faut alors internet et plusieurs minutes).
- **Traductions** : tous les textes de l'interface sont dans `src/main/resources/com/qareporting/i18n/messages_fr.properties` et `messages_en.properties`. Pour corriger un libellé, modifiez la même clé dans les deux fichiers.
- **Produire le fichier `.war`** : `mvn package`. Le fichier est créé dans `target/qa-reporting-j2ee.war`.
- **API REST** : endpoints sous `http://127.0.0.1:8080/qa-reporting-j2ee/api/…` (ex. `/api/projects`). Ce n'est pas une page web : `/api/` seul renvoie 404, et les endpoints demandent un jeton (obtenu via `POST /api/login`).
- **Cahier des charges** : `docs/Cahier-des-charges-QA-Reporting-J2EE-NOUVEAU.docx`
