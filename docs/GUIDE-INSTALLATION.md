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
mvn wildfly:run -Dwildfly.startupTimeout=300
```

- **Le premier lancement est long (5 à 15 minutes)** : Maven télécharge le serveur WildFly et toutes les dépendances. Il faut une connexion internet.
- Attendez que le terminal affiche une ligne qui contient `Deployed "qa-reporting-j2ee.war"` ou `WildFly ... started`.
- **Ne fermez pas ce terminal** : le site fonctionne tant qu'il reste ouvert. Pour arrêter le site, appuyez sur `Ctrl + C`.

Pour les lancements suivants, vous pouvez travailler sans internet, ce qui est plus rapide :

```bash
mvn -o wildfly:run -Dwildfly.startupTimeout=300
```

---

## Étape 5 : ouvrir le site

Ouvrez votre navigateur à l'adresse **http://127.0.0.1:8080/qa-reporting-j2ee/**

Connectez-vous avec le compte administrateur créé automatiquement :

| Email | Mot de passe |
|---|---|
| `admin@qa-reporting-j2ee.local` | `password` |

Depuis ce compte, vous pouvez créer d'autres utilisateurs (QA, QA Lead, Manager) pour tester les différents rôles.

---

## Récapitulatif : à chaque fois que vous voulez relancer le site

1. XAMPP : **MySQL**, puis **Start**.
2. Terminal dans le dossier du projet : `mvn -o wildfly:run -Dwildfly.startupTimeout=300`
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
| Le serveur s'arrête tout seul au bout d'environ 60 s | Vous avez oublié `-Dwildfly.startupTimeout=300`. |
| `Address already in use` ou port 8080 occupé | Un autre serveur (Tomcat, un autre WildFly, etc.) utilise le port 8080. Arrêtez-le. |
| Erreur de téléchargement avec `-o` | Au tout premier lancement, retirez `-o` : il faut internet. |
| La page affiche 404 | Vérifiez l'URL : elle doit se terminer par `/qa-reporting-j2ee/`. |

---

## Pour aller plus loin

- **Tests unitaires** : `mvn test`
- **Produire le fichier `.war`** : `mvn package`. Le fichier est créé dans `target/qa-reporting-j2ee.war`.
- **API REST** : disponible sous http://127.0.0.1:8080/qa-reporting-j2ee/api/
- **Cahier des charges** : `docs/Cahier-des-charges-QA-Reporting-J2EE-NOUVEAU.docx`
