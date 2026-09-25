# QA Reporting — J2EE

Portage Jakarta EE 10 de l'application **QA Reporting** (suivi d'activité QA, tests, campagnes, anomalies et reporting). Il reprend les règles métier et le périmètre fonctionnel de la version Laravel/React.

- **Interface web** : JSF (Facelets `.xhtml`)
- **API REST** : JAX-RS, sous `/api`
- **Persistance** : JPA / Hibernate 6, sur MariaDB ou MySQL
- **Serveur** : WildFly 31, installé automatiquement par le `wildfly-maven-plugin`

> 📘 **Première installation ?** Suivez le guide pas à pas : [docs/GUIDE-INSTALLATION.md](docs/GUIDE-INSTALLATION.md)
>
> 🎬 **Présentation ?** Données de démonstration et déroulé minuté : [docs/SCENARIO-DEMO.md](docs/SCENARIO-DEMO.md)

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
mvn wildfly:run
```

Le premier lancement télécharge WildFly et les dépendances, ce qui prend plusieurs minutes. Ensuite, vous pouvez travailler hors ligne :

```bash
mvn -o wildfly:run
```

> Le délai de démarrage du serveur est fixé à 300 s dans le `pom.xml` (le défaut du plugin, 60 s, est trop court : Maven arrêterait le serveur en plein déploiement).

### 4. Ouvrir l'application

| | URL |
|---|---|
| Interface web | http://127.0.0.1:8080/qa-reporting-j2ee/ |
| API REST | préfixe `http://127.0.0.1:8080/qa-reporting-j2ee/api/…` (ex. `/api/projects`, jeton requis — `/api/` seul renvoie 404) |

Au premier démarrage, la classe `StartupSeeder` crée les rôles, les environnements, les paramètres par défaut et un compte administrateur :

- **Email** : `admin@qa-reporting-j2ee.local`
- **Mot de passe** : `password`

Ce mot de passe est **provisoire** : l'application impose d'en choisir un nouveau à la première connexion. Pour un serveur réel (HTTPS, identifiants de la base, sauvegardes), voir la section « Mise en production » du [guide](docs/GUIDE-INSTALLATION.md#mise-en-production-serveur-dentreprise).

## Autres commandes

```bash
mvn test       # 40 tests unitaires (JUnit 5)
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

## Langues (français / anglais)

L'interface est disponible en français (par défaut) et en anglais. Le sélecteur **FR | EN** se trouve dans l'en-tête et sur la page de connexion. Le choix est mémorisé dans un cookie.

- Textes des pages : `src/main/resources/com/qareporting/i18n/messages_fr.properties` et `messages_en.properties`, utilisés dans les pages avec `#{msg['cle']}`.
- Textes produits en Java (erreurs, libellés des graphiques) : `I18n.t("cle")`.
- Les données saisies (noms de projets, descriptions…) ne sont pas traduites.

## Rôles

| Rôle | Périmètre |
|---|---|
| QA | Rédige et exécute ses tests, déclare son activité, crée des anomalies (avec pièces jointes) |
| QA Lead | Son équipe : campagnes, répartition des tests, assignation des anomalies, validation de l'activité, rapports par testeur |
| Manager | Consultation de tous les projets : vue d'ensemble et rapports |
| Admin | Tout, plus l'administration (utilisateurs, projets, équipes, paramètres) et le journal d'audit |

Les droits sont définis en un seul endroit, `security/Permissions.java`, et appliqués à la fois par l'API REST et par l'interface. Le guide d'utilisation par rôle est dans [docs/GUIDE-INSTALLATION.md](docs/GUIDE-INSTALLATION.md#utiliser-lapplication-selon-son-rôle).

## Dépannage

- **WildFly reste bloqué au démarrage** : MariaDB n'est pas lancé, ou il n'est pas encore prêt. Vérifiez le port 3306.
- **`Unknown database 'qa_reporting_j2ee'`** : la base n'existe pas encore. Voir l'étape 2.
- **Port 8080 déjà utilisé** : arrêtez l'autre serveur (Tomcat, un autre WildFly…) qui utilise ce port.



mvn -o wildfly:run
http://127.0.0.1:8080/qa-reporting-j2ee/api/