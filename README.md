# QA Reporting — J2EE

Portage Jakarta EE 10 de l'application **QA Reporting** (suivi d'activité QA, tests, campagnes, anomalies et reporting). Il reprend les règles métier et le périmètre fonctionnel de la version Laravel/React.

- **Interface web** : JSF (Facelets `.xhtml`)
- **API REST** : JAX-RS, sous `/api`
- **Persistance** : JPA / Hibernate 6, sur MariaDB ou MySQL
- **Serveur** : WildFly 31, installé automatiquement par le `wildfly-maven-plugin`

## Prérequis

| Outil | Version |
|---|---|
| JDK | 17 ou plus |
| Maven | 3.8 ou plus |
| MariaDB (par ex. via XAMPP) ou MySQL | écoute sur `127.0.0.1:3306` |

Vous n'avez pas besoin d'installer WildFly : Maven le télécharge et le configure dans `target/server`.

## Démarrage

### 1. Démarrer la base de données

Lancez MariaDB, par exemple avec le panneau XAMPP → *MySQL* → *Start*. Attendez que le serveur soit prêt (`ready for connections` dans `C:\xampp\mysql\data\mysql_error.log`) avant de passer à la suite. Sinon, WildFly se bloque sur la datasource.

### 2. Créer la base (première fois seulement)

```sql
CREATE DATABASE qa_reporting_j2ee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Les tables sont créées et mises à jour automatiquement au démarrage à partir des entités JPA (`hibernate.hbm2ddl.auto=update`). Vous n'avez aucun script de migration à lancer.

Par défaut, l'application se connecte avec l'utilisateur `root` sans mot de passe (configuration XAMPP par défaut). Pour changer ces identifiants, modifiez [`src/main/webapp/WEB-INF/qa-reporting-ds.xml`](src/main/webapp/WEB-INF/qa-reporting-ds.xml).

### 3. Lancer l'application

```bash
mvn wildfly:run -Dwildfly.startupTimeout=300
```

Le premier lancement télécharge WildFly et les dépendances, ce qui prend plusieurs minutes. Ensuite, vous pouvez travailler hors ligne :

```bash
mvn -o wildfly:run -Dwildfly.startupTimeout=300
```

> **Pourquoi `startupTimeout=300` ?** Par défaut, le plugin attend le serveur 60 s. L'initialisation d'Hibernate et la mise à jour du schéma peuvent prendre plus longtemps. Maven arrête alors le serveur alors que le déploiement était en train de réussir.

### 4. Ouvrir l'application

| | URL |
|---|---|
| Interface web | http://127.0.0.1:8080/qa-reporting-j2ee/ |
| API REST | http://127.0.0.1:8080/qa-reporting-j2ee/api/ |

Au premier démarrage, la classe `StartupSeeder` crée les rôles, les environnements, les paramètres par défaut et un compte administrateur :

- **Email** : `admin@qa-reporting-j2ee.local`
- **Mot de passe** : `password`

⚠️ Changez ce mot de passe dès la première connexion si l'application doit être exposée.

## Autres commandes

```bash
mvn test       # tests unitaires (JUnit 5)
mvn package    # produit target/qa-reporting-j2ee.war, déployable sur un WildFly existant
```

Pour déployer le `.war` sur un WildFly que vous gérez vous-même, copiez-le dans `standalone/deployments/`. Le fichier `qa-reporting-ds.xml` inclus dans le WAR déclare la datasource `java:/QaReportingJ2eeDS`.

## Structure du projet

```
src/main/java/com/qareporting/
├── entity/      Entités JPA (User, Project, Test, Defect, Campaign…)
├── service/     Logique métier (services CDI)
├── resource/    Endpoints REST JAX-RS (/api/…)
├── security/    Authentification par jeton, hachage BCrypt, contrôle des rôles
├── web/         Beans JSF de l'interface web
├── dto/         Objets d'entrée et de sortie de l'API
└── StartupSeeder.java   Données initiales
src/main/webapp/
├── login.xhtml, app/…   Pages JSF
└── WEB-INF/             web.xml, datasource, templates
docs/                    Cahier des charges
```

## Rôles

| Rôle | Périmètre |
|---|---|
| QA | Saisie de l'activité, tests, anomalies et reporting personnel |
| QA Lead | Suivi de l'équipe, campagnes et anomalies du périmètre |
| Manager | Vision synthétique de l'avancement et des risques |
| Admin | Gestion des utilisateurs, projets, équipes et paramètres |

## Dépannage

- **WildFly reste bloqué au démarrage** : MariaDB n'est pas lancé, ou il n'est pas encore prêt. Vérifiez le port 3306.
- **`Unknown database 'qa_reporting_j2ee'`** : la base n'existe pas encore. Voir l'étape 2.
- **Maven arrête le serveur après environ 60 s** : ajoutez `-Dwildfly.startupTimeout=300`.
- **Port 8080 déjà utilisé** : arrêtez l'autre serveur (Tomcat, un autre WildFly…) qui utilise ce port.
