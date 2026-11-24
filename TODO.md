# TODO Freetalk

État au 24 novembre 2026. Le code est sur la branche `modernize` (dernier commit de code : `d44df440`),
en local et sur le fork `dagga/plugin-Freetalk`. La copie de travail est restée sur `master`.

## En cours : essais sur le nœud local

Freetalk (`build0013-853-g4687`, même code que `d44df440`) est chargé depuis `dist/Freetalk.jar` et enregistré dans
`freenet.ini`.

- [ ] Se connecter sur `http://127.0.0.1:8888/Freetalk/` et vérifier que les identités arrivent. WoT
      en connaît 4 245 avec le contexte Freetalk, dont 4 140 de score positif ou nul.
- [ ] Regarder s'il reste de vrais anciens messages sur le réseau (peut prendre des heures ou des jours).
- [ ] Publier un message de test. **Public** : le premier message ajoute le contexte « Freetalk » à
      l'identité dans WoT.
- [ ] Vérifier l'envoi avec les délais normaux (5 à 15 min ou plus avant l'envoi) : jamais prouvé.
- [ ] Essayer Préférences > « Télécharger les messages des auteurs non liés à la toile de confiance
      (WoT) ? ». Désactivé par défaut. Une recherche par heure, la première après quelques minutes.
- [ ] Vérifier que le menu Freetalk apparaît dans l'interface fred-ng.
- [ ] Surveiller la charge : environ 4 140 abonnements USK de plus.

### Précautions

- `./gradlew clean` dans ce dossier supprime `dist/`, donc le JAR que le nœud recharge au démarrage.
  Un build depuis `master` l'écraserait par une ancienne version. Copier le JAR ailleurs ou construire
  dans un autre dossier.
- Ne pas recharger WoT pendant que Freetalk tourne : blocage possible jusqu'à 10 min (voir lot 2).
- La base est `~/Freenet/Freetalk/Freetalk.db4o`, au format 4. Un JAR de `master` la refusera.
- Pour revenir en arrière : décharger Freetalk depuis la page Plugins.

## Lot 2 : nouvelle API FCP (3 à 5 jours)

- [ ] Remplacer `PluginTalkerBlocking` par `FCPPluginConnection.sendSynchronous()`. Il peut livrer une
      réponse au mauvais appelant, et attend 10 min si WoT est rechargé (12 threads bloqués, arrêt du
      nœud en 102 s lors de l'essai).
- [ ] Passer le serveur FCP de Freetalk de `FredPluginFCP` à `ServerSideFCPMessageHandler`.

## Lot 3 : notifications WoT (3 à 6 semaines)

Contacter xor avant de commencer : c'est le chantier annoncé dans son README.

- [ ] Reprendre `FCPClientReferenceImplementation` de WoT, s'abonner à Identities et Scores.
- [ ] Garder scores et confiances en local (aujourd'hui deux appels FCP bloquants par message affiché).
- [ ] Mesurer la synchronisation initiale : `Subscribe` ne filtre pas par contexte.
- [ ] Remplacer l'interrogation horaire de WoT. Dans `GetIdentitiesByScore`, se fier à `Amount` et non à
      `Identities.Amount`, qui vaut 1.
- Ne pas fusionner la branche `event-notifications` (2011).

## Bugs connus, non corrigés

FCP :
- [ ] `PutMessage` : `ParentThreadID` n'est pas documenté, et `ParentID` seul crée un nouveau fil sans
      erreur, alors que le commentaire du code annonce une exception.
- [ ] `PutMessage` ne peut pas répondre dans un fil dont la racine manque (le web le peut).
- [ ] `SubscribeToBoard` et `UnsubscribeFromBoard` ne répondent pas en cas de succès.
- [ ] `ListThreadMessages` échoue sans `ThreadID`, pourtant documenté comme optionnel.
- [ ] `GetMessage` avec un index inconnu renvoie une description vide.
- [ ] `FCPInterface.handle()` : `return` manquant après « Plugin is terminated » (lu, non testé).

Web et affichage :
- [ ] GET `/ChangeTrust` et `/IntroduceIdentity` sans `TaskID` : HTTP 500.
- [ ] La boîte d'envoi montre les messages de toutes les identités locales.
- [ ] `Quoting.java` : un `[quote author="x"]` écrit à la main, sans `message`, avale du texte.

Autres :
- [ ] `SubscribedBoard.mDescription` masque `Board.mDescription` : une requête dessus échoue.
- [ ] `WoTIdentityManager.createOwnIdentity` (avec URI) annule sans relancer l'exception (lu, non testé).
- [ ] Port NNTP 1199 codé en dur.
- [ ] À l'arrêt du nœud, erreurs `DatabaseClosedException` si WoT se ferme avant (bruit).
- [ ] Une réponse datée strictement avant son parent (horloge décalée) s'affiche avant lui.

Auteurs non liés à WoT :
- [ ] Leur pseudo est toujours « Unknown ». Il faudrait le lire dans leur XML WoT.
- [ ] On ne peut pas les noter depuis Freetalk : WoT ne les connaît pas.
- [ ] La règle d'exclusion n'a été vérifiée que sur nœuds isolés : pas de test unitaire avec de faux
      échanges WoT. Idem pour l'ajout du contexte au premier message.

## Non vérifié

- [ ] JDK 8 (aucun disponible ici), Windows, macOS, Eclipse.
- [ ] CI GitHub : jamais exécutée. Java 25 y a été ajouté sans vérifier que fred y compile.
- [ ] Échange de messages entre un nœud 1507 et un nœud fred-ng.
- [ ] Puzzles d'introduction de WoT (aucun disponible pendant les essais).
- [ ] Comportement sur JDK 26 et plus (db4o écrit dans des champs `final` par réflexion).

## Plus tard

- Passerelle ou import FMS : mis de côté.
- Extracteur pour d'anciennes bases `Freetalk.db4o` : seulement s'il en existe une sauvegarde.
- Remplacer db4o : déconseillé tant que WoT en dépend.
- Lot 5 : réduire les abonnements USK, moderniser les pages FProxy, API REST pour l'interface fred-ng.
- Proposer les correctifs en amont, à hyphanet ou à xor : rien n'a été proposé.

## Nœuds de test isolés

Scripts et dépendances dans `~/IdeaProjects/freetalk-test-nodes` (voir son `README.md`). Les nœuds sont à
recréer : les anciens, dans `/tmp`, n'ont pas été gardés.

## Après un redémarrage

Les worktrees des branches sont dans un dossier temporaire de `/tmp`. Les commits sont en sécurité dans
`.git` (et `modernize` sur le fork). Si `/tmp` a été vidé : `git worktree prune`.
