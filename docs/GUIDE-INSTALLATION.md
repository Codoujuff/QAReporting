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

Vous n'avez **rien d'autre à faire** : les tables et les données de départ sont créées automatiquement au premier lancement de l'application.

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

Depuis ce compte, vous pouvez créer d'autres utilisateurs (QA, QA Lead, Manager) pour tester les différents rôles.

La langue se change avec le sélecteur **FR | EN**, en haut à droite (et sur la page de connexion). Le choix est mémorisé.

---

## Étape 6 : préparer l'application (première utilisation)

L'application démarre vide, avec seulement le compte admin. Pour la rendre utilisable par une équipe, faites les étapes suivantes **dans cet ordre** (les suivantes ont besoin des précédentes) :

| # | Qui | Où | Quoi |
|---|---|---|---|
| 1 | Admin | Administration → **Équipes** | Créer l'équipe QA (ex. « Équipe Paiement »). |
| 2 | Admin | Administration → **Utilisateurs** | Créer les comptes : un **QA Lead**, un ou plusieurs **QA**, un **Manager**. Rattacher le lead et les QA à l'équipe. |
| 3 | Admin | Administration → **Équipes** | Modifier l'équipe pour lui donner son responsable (le QA Lead). |
| 4 | Admin | Administration → **Projets** | Créer le projet et le rattacher à l'équipe. **Sans équipe, le projet n'apparaît pas chez le QA Lead.** |
| 5 | QA Lead | **Campagnes** → Nouvelle campagne | Créer la campagne du sprint / de la version, puis **Démarrer**. |
| 6 | QA Lead | **Tests** → Nouveau test | Créer les cas de test et les **assigner** aux testeurs. |

Les environnements (DEV, QA, PREPROD, PROD) et les paramètres généraux sont créés automatiquement au premier lancement.

---

## Utiliser l'application selon son rôle

Chaque rôle ne voit que les menus et les boutons qu'il a le droit d'utiliser. Une page interdite affiche « Accès refusé » (403) ; un élément qui n'est pas dans votre périmètre affiche « Introuvable » (404).

### QA (testeur)

Voit ses tests, ses anomalies et son activité.

1. **Tests** : ouvrir un test qui vous est assigné, choisir le **Résultat** (réussi, échoué, bloqué, non exécuté), puis **Enregistrer le résultat**.
2. **Si le test échoue** : un encadré rouge propose **Déclarer l'anomalie**. Le formulaire est déjà rempli (projet, étapes, résultat attendu, résultat obtenu) : complétez puis **Créer**.
3. **Anomalies** → ouvrir l'anomalie → **Pièces jointes** : ajouter une capture, une vidéo ou un journal (10 Mo maximum).
4. **Nouveau test** : vous pouvez rédiger vos propres cas de test. Ils vous sont assignés automatiquement.
5. En fin de journée : **Mon activité** → **Nouvelle activité**. La somme réussis + échoués + bloqués + non exécutés doit être égale au nombre de tests exécutés, et un motif est obligatoire s'il y a des tests bloqués.
6. Quand le développeur a corrigé : sur l'anomalie, **Marquer corrigée**, puis après revérification **Revérification : réussie** (l'anomalie est fermée) ou **échouée** (elle est rouverte).

### QA Lead (chef d'équipe)

Voit tout ce qui concerne son équipe.

- **Campagnes** : créer, démarrer, bloquer, reprendre, terminer. L'équipe reçoit une notification à chaque changement.
- **Tests** : créer les cas de test et les répartir entre testeurs (champ « Assigné à »).
- **Anomalies** : les assigner à un testeur (bloc **Assigner à** sur la fiche).
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
| Erreur 500 avec `Unresolved compilation problem` dans le terminal | L'éditeur (VS Code, Eclipse) a compilé une version cassée d'un fichier. Supprimez **uniquement** le dossier `target/classes`, puis relancez. |

---

## Pour aller plus loin

- **Tests unitaires** : `mvn test` (32 tests : mots de passe, jetons, droits par rôle, portée des données, règles métier…)
- **Ne lancez pas `mvn clean`** sans raison : il efface aussi le serveur WildFly installé dans `target/server`, qui sera retéléchargé (il faut alors internet et plusieurs minutes).
- **Traductions** : tous les textes de l'interface sont dans `src/main/resources/com/qareporting/i18n/messages_fr.properties` et `messages_en.properties`. Pour corriger un libellé, modifiez la même clé dans les deux fichiers.
- **Produire le fichier `.war`** : `mvn package`. Le fichier est créé dans `target/qa-reporting-j2ee.war`.
- **API REST** : endpoints sous `http://127.0.0.1:8080/qa-reporting-j2ee/api/…` (ex. `/api/projects`). Ce n'est pas une page web : `/api/` seul renvoie 404, et les endpoints demandent un jeton (obtenu via `POST /api/login`).
- **Cahier des charges** : `docs/Cahier-des-charges-QA-Reporting-J2EE-NOUVEAU.docx`
