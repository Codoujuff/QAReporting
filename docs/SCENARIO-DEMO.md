# Scénario de démonstration — QA Reporting (édition J2EE)

Durée visée : **12 minutes**, puis questions. Le fil conducteur : *une équipe QA teste le paiement d'une boutique en ligne ; on suit une anomalie de sa découverte à sa fermeture, et chaque rôle voit exactement ce dont il a besoin.*

---

## 1. Préparer la démo (le jour même, 30 minutes avant)

Les dates des données de démo sont calculées **à partir du jour du chargement**. Chargez-les donc le jour de la présentation, sur une base propre.

1. **Libérer de la mémoire** : fermez le navigateur (sauf un onglet), Android Studio, les émulateurs, etc. WildFly a besoin d'environ 1,5 Go libre.
2. **Repartir d'une base propre** (XAMPP → MySQL → Start, puis **Shell**) :
   ```sql
   mysql -u root
   DROP DATABASE qa_reporting_j2ee;
   CREATE DATABASE qa_reporting_j2ee CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   exit;
   ```
3. **Lancer l'application** dans le dossier du projet : `mvn -o wildfly:run`, puis attendre `WFLYSRV0025 ... started`.
4. **Charger les données** : se connecter en admin (`admin@qa-reporting-j2ee.local` / `password`). L'application exige d'abord un **nouveau mot de passe** : choisissez-le et **notez-le**, il servira pendant la démo. Ensuite, aller dans **Administration → Paramètres généraux**, puis cliquer sur **Charger les données de démonstration**.
5. **Préparer une session par rôle**, pour ne pas avoir à se déconnecter pendant la démo. Attention : dans un même navigateur, les onglets **et toutes les fenêtres privées** partagent la même session. Il faut donc un navigateur (ou un profil de navigateur) par personne :
   - Chrome : Awa (QA) ;
   - Edge : Fatou (QA Lead) ;
   - fenêtre privée Chrome : Ibrahima (Manager) ;
   - fenêtre privée Edge : Admin.
6. **Avoir une capture d'écran prête** sur le Bureau (n'importe quelle image PNG) pour la pièce jointe.

### Comptes de démo (mot de passe commun : `demo2026`)

| Rôle | Personne | E-mail |
|---|---|---|
| QA Lead | Fatou Sarr | `fatou.sarr@demo.qa-reporting.local` |
| QA | Awa Ndiaye | `awa.ndiaye@demo.qa-reporting.local` |
| QA | Moussa Fall | `moussa.fall@demo.qa-reporting.local` |
| QA | Khadija Ba | `khadija.ba@demo.qa-reporting.local` |
| Manager | Ibrahima Diallo | `ibrahima.diallo@demo.qa-reporting.local` |
| Admin | Admin | `admin@qa-reporting-j2ee.local` (le mot de passe choisi à l'étape 4) |

### Ce que contiennent les données

- **Équipe Paiement** : Fatou (lead), Awa, Moussa, Khadija. Ibrahima est manager, hors équipe.
- **Projets** : *Boutique en ligne* et *Application mobile*.
- **Campagnes** :
  - *Régression v2.2* est **terminée** ;
  - *Recette v2.3 – Paiement* est **en cours** ;
  - *Recette mobile v1.0* est **planifiée**.
- **Tests** : 16 cas de test avec leur historique d'exécution. On y trouve des réussites, des échecs, un test bloqué et des tests non exécutés.
- **Anomalies** : 7, une par étape du cycle de vie.

  | Anomalie | Statut |
  |---|---|
  | *Carte expirée : écran blanc…* | Ouverte |
  | *Code promo appliqué deux fois* | En cours (critique) |
  | *Orange Money : délai d'attente…* | Corrigée |
  | *Libellé « Payer » tronqué* | À revérifier |
  | *Remboursement : bouton inactif* | Rouverte |
  | *Recherche : les accents…* | Fermée après revérification |
  | Doublon | Fermé avec motif |

- **Activité** : deux semaines de déclarations. Les plus anciennes sont validées ; celles des deux derniers jours sont **à valider** par Fatou. Khadija n'a pas encore déclaré sa journée.

---

## 2. Déroulé

### Acte 1 — Le problème et la solution (1 min, page de connexion)

> « Dans beaucoup d'équipes, le suivi des tests se fait dans des fichiers Excel et des messages. QA Reporting centralise les campagnes, les tests, les anomalies et l'activité, avec des rapports calculés automatiquement. C'est une application Jakarta EE : JSF pour l'interface, JAX-RS pour l'API, JPA pour les données, sur WildFly. »

- Montrez le sélecteur **FR | EN** : l'interface est bilingue.

### Acte 2 — La testeuse (Awa, 4 min)

1. **Tableau de bord** : ses chiffres, ses anomalies récentes, les actions rapides.
2. **Tests** : elle ne voit **que ses tests**. Ouvrez *Paiement Wave* (non exécuté) :
   - Résultat **Échoué** ;
   - Résultat obtenu : « Le QR code ne s'affiche pas » ;
   - **Enregistrer le résultat**.
3. L'encadré rouge **« Le test a échoué »** apparaît. Cliquez sur **Déclarer l'anomalie** :
   > « Le formulaire est pré-rempli : projet, étapes numérotées, résultat attendu, résultat obtenu. Le testeur ne ressaisit rien. »

   Cliquez sur **Créer**, puis ouvrez l'anomalie.
4. **Pièces jointes** : ajoutez la capture d'écran du Bureau, puis **Envoyer**.
5. Ouvrez *Paiement Orange Money*. Le test est **Échoué**, et l'anomalie liée est **Corrigée** :
   > « Le développeur a corrigé ; c'est au testeur de revérifier. »

   Sur l'anomalie :
   - **Commencer la revérification** ;
   - puis **Revérification : réussie**.

   L'anomalie passe à **Fermée**, et l'historique garde chaque étape.
6. Ouvrez un test de *Recette mobile v1.0* (liste des Tests, recherche « android ») :
   > « La campagne n'est pas démarrée : l'application refuse l'exécution. Une campagne terminée est figée de la même façon. »
7. **Mon activité → Nouvelle activité** : montrez la règle métier. Si la somme réussis + échoués + bloqués + non exécutés est différente du nombre de tests exécutés, l'enregistrement est refusé.

### Acte 3 — La cheffe d'équipe (Fatou, 3 min)

1. **Tableau de bord équipe** : la tuile jaune **« Activités à valider »**. Cliquez dessus pour arriver sur la liste filtrée. Cliquez sur **Valider** sur une ligne :
   > « Une déclaration validée n'est plus modifiable par son auteur. »
2. **Anomalies** : recherchez « promo ». Ouvrez l'anomalie critique, puis la liste **Assigner à** :
   > « On ne peut assigner qu'aux testeurs de l'équipe du projet : ni le manager, ni une autre équipe. »
3. Essayez **Fermer** sans commentaire : le motif est obligatoire.
4. **Rapports** : l'onglet **Semaine**, la répartition jour par jour, le tableau **Par testeur**. Passez sur **Mois**, puis **Exporter en CSV**.
5. **Campagnes** : c'est la cheffe d'équipe qui démarre, bloque ou termine une campagne, et l'équipe est notifiée.

### Acte 4 — Le manager (Ibrahima, 1 min)

- **Vue d'ensemble** : les indicateurs et graphiques de tous les projets.
  > « Il consulte, il ne modifie rien : aucun bouton de création. »
- Tapez l'URL `…/app/campaigns/form.xhtml` : la page **Accès refusé** s'affiche.
  > « Les droits sont vérifiés côté serveur, pas seulement cachés dans l'interface. »

### Acte 5 — L'administrateur (Admin, 1,5 min)

1. **Journal d'audit** : chaque action de la démo y est, avec son auteur (Awa, Fatou…), l'objet et le détail « avant → après ».
   > « Rien n'y est jamais modifié ni supprimé. »
2. **Utilisateurs, Projets, Équipes** : la configuration.
3. *(Si le temps le permet)* **Paramètres** d'un compte : le **rappel de saisie** envoie une notification à l'heure choisie, si rien n'a été déclaré dans la journée.

### Conclusion (30 s)

> « Quatre rôles, une même matrice de droits pour l'interface et l'API, un cycle de vie d'anomalie complet et tracé, des rapports calculés à partir des données réelles. 40 tests automatisés vérifient les règles métier. »

---

## 3. Questions probables du jury

| Question | Réponse courte |
|---|---|
| Comment les droits sont-ils garantis ? | Une matrice unique (`security/Permissions.java`) est lue par l'intercepteur `@RequiresRole` de l'API et par les beans JSF, et chaque action est revérifiée côté serveur. La portée des données (`canView`) renvoie une erreur 404 hors périmètre. |
| Et si on appelle l'API directement ? | Même règle : un jeton QA ne donne accès qu'aux données d'un QA. |
| Comment prouvez-vous la traçabilité ? | `DefectHistory` et `TestExecution` ne sont jamais écrasés. Chaque écriture ajoute une ligne `AuditLog` dans la même transaction, visible sur l'écran Journal d'audit. |
| Pourquoi JSF et pas un framework JavaScript ? | L'objectif était de démontrer la pile Jakarta EE standard, sans Spring ni framework front. L'API REST existe aussi pour un futur client. |
| Comment le rappel est-il envoyé ? | Un timer EJB (`@Schedule`) s'exécute chaque minute. Il notifie les testeurs dont l'heure de rappel est passée et qui n'ont rien déclaré dans la journée, une fois par jour. |
| Comment l'application est-elle testée ? | 40 tests JUnit (mots de passe, jetons, droits, portée, règles de campagne et d'anomalie, recherche) et des parcours de bout en bout dans un navigateur automatisé. |
| Et la sécurité ? | Mots de passe BCrypt avec règle de robustesse, mot de passe provisoire à changer, blocage après 5 échecs, session renouvelée à la connexion, jetons d'API qui expirent, en-têtes de sécurité, HTTPS activable (`QA_FORCE_HTTPS`), comptes désactivés plutôt que supprimés. |
| Limites ? | Voir le chapitre 10 du cahier des charges : export PDF, e-mails réels, temps réel (WebSocket), intégration Jira. |

---

## 4. En cas de problème pendant la démo

| Problème | Que faire |
|---|---|
| La page tourne sans fin | La mémoire est saturée : fermez d'autres programmes. Au pire, relancez `mvn -o wildfly:run` (environ 1 minute). |
| Données abîmées par une répétition | Refaites l'étape 1.2 (base propre), relancez, puis rechargez les données. |
| Déconnexion en plein acte | Chaque navigateur garde sa propre session : reconnectez-vous seulement dans celui-là. |
| « Trop de tentatives de connexion » | 5 mots de passe faux de suite bloquent le compte 15 minutes : prenez un autre compte de démo, ou relancez le serveur. Tapez les mots de passe avec soin ! |
