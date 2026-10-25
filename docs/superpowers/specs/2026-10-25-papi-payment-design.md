# Intégration Papi en parallèle — conception

## Intention

Ajouter Papi comme nouveau fournisseur de paiement de Vaikaparts Pecunia, en conservant les intégrations MVola et Airtel Money et leurs routes actuelles. Papi apporte une API backend pour initier des paiements MVola, Airtel Money ou Orange Money.

## Objectifs

- Exposer `POST /v1/payments/papi` et `GET /v1/payments/papi/{reference}`. Le POST reprend les champs de paiement Pecunia, ajoute `paymentMethod`, `reference` stable de commande (obligatoire, 255 caractères maximum) et requiert un `payer.name`; `temporaryCode` est une chaîne obligatoire et non vide uniquement pour Orange Money.
- Initier et relire un paiement via `POST /v1/payments` et `GET /v1/payments/{reference}` sur `https://app.papi.mg/engine/api`, en envoyant la clé dans l’en-tête `Token`.
- Persister la référence marchande, la référence de paiement Papi, le `notificationToken`, le moyen Papi et l’état dans le modèle existant.
- Traiter `PENDING`, `SUCCESS` et `FAILED`, les erreurs HTTP ainsi que les conflits d’idempotence sans créer un second débit.
- Recevoir les notifications sur une route dédiée, valider `X-Papi-Signature` sur les octets bruts (HMAC-SHA256, tolérance de 300 secondes, comparaison à temps constant), puis vérifier référence et `notificationToken` avant mise à jour.
- Configurer clé API, secret de signature, base URL et URL de notification par configuration serveur; garder tous les secrets côté serveur.
- Documenter les nouvelles requêtes et réponses dans `doc/api.yaml`.

## Hors périmètre

- Modifier ou retirer les routes, callbacks, clients, schémas ou configurations MVola/Airtel Money existants.
- Utiliser le Hosted Checkout ou gérer les cartes bancaires.
- Ajouter une interface de paiement frontend ou un polling client. Le callback est le mécanisme principal; la relecture Papi est le recours disponible via l’API.
- Déployer ou activer des identifiants Papi réels.

## Approches considérées

1. **Ajouter un fournisseur PAPI dans l’architecture actuelle (retenue).** Ajouter enum, client HTTP, passerelle, service, persistance, contrôleur et tests en suivant les conventions existantes; garder les routes MVola/Airtel inchangées. Cela isole le nouveau fournisseur et respecte la demande de coexistence.
2. **Remplacer MVola et Airtel par Papi.** Cela simplifierait les appels sortants mais modifierait des intégrations que l’utilisateur veut conserver; rejetée.
3. **Utiliser Hosted Checkout.** Cela redirigerait le payeur vers les écrans Papi et introduirait un autre parcours que l’API de paiement indiquée; rejetée pour ce périmètre.

## Architecture et décisions

- Le client choisit un `paymentMethod` Papi parmi `MVOLA`, `AIRTEL_MONEY`, `ORANGE_MONEY`; le fournisseur de l’application est `PAPI`.
- La nouvelle route reçoit une référence de commande stable depuis l’appelant; elle ne la génère pas. Cette référence est persistée telle quelle et sert d’idempotency key auprès de Papi et de clé de rapprochement pour la notification. Une nouvelle tentative après `FAILED` conserve la référence; une répétition avec une charge différente est refusée comme conflit et le statut courant est relu avant de proposer une autre tentative.
- Le corps du POST comprend les champs existants (`amount`, `description`, `type`, `payer`), `paymentMethod`, `reference`, et `temporaryCode` pour Orange. La devise Papi est toujours `MGA`; la route valide les montants entiers supérieurs ou égaux à 300 et le nom du payeur, puis fixe la devise interne à `MGA`.
- Le `notificationToken` est sauvegardé à partir de la réponse d’initiation. Le callback retrouve le paiement via `merchantPaymentReference`, puis compare son `notificationToken`; référence inconnue ou jeton incorrect est rejeté en non-2xx. Pour une notification authentique, inconnue ou déjà traitée, répondre non-2xx à la première occurrence invalide/inconnue et 2xx aux doublons déjà enregistrés.
- Papi accepte des montants entiers MGA avec minimum 300. Valider ces contraintes avant l’appel distant; ne pas convertir silencieusement les montants.
- Réutiliser le `PaymentGatewayFactory` pour l’accès fournisseur. Ajouter les configurations Papi dans une classe de configuration dédiée et utiliser le client HTTP Spring déjà présent.
- Ajouter une entité jointe et une migration Flyway additive pour les métadonnées Papi. Ajouter la valeur `PAPI` au type Postgres `payment_provider` par une migration séparée et non destructive.
- Élargir `payments.transaction_id` à 255 caractères pour conserver la référence marchande Papi de longueur maximale sans troncature. Fournir l’identité interne du bénéficiaire via `PAPI_PARTNER_NAME` et `PAPI_PARTNER_MSISDN`, car le modèle actuel exige un bénéficiaire en base.
- La référence de commande est fournie par l’appelant, persistée avant l’appel distant et réutilisée sans modification pour les répétitions réseau et les tentatives après échec. Papi retourne `409 PAYMENT_CONFLICT` si cette référence est réutilisée alors que le paiement existant est `PENDING`/`SUCCESS` avec d’autres paramètres; le service relaie une erreur de conflit claire et n’initie pas une nouvelle tentative.
- Mapper les statuts Papi vers le statut interne (`PENDING`, `SUCCESS`, `FAILED`) sans interpréter une réponse HTTP 200 comme un succès automatique. `SUCCESS` est terminal; `FAILED` autorise une nouvelle tentative conformément à la règle Papi.
- Le callback prend le corps brut en `byte[]`, vérifie l’en-tête signé avant le parsing JSON, rejette les signatures invalides/anciennes en non-2xx et traite les doublons sans effet secondaire.
- `GET /v1/payments/papi/{reference}` relit auprès de Papi les paiements locaux toujours `PENDING` et persiste un statut terminal; si l’API Papi est indisponible, la route retourne l’état local inchangé.
- Configurer les valeurs par variables d’environnement et les inclure avec des valeurs vides/commentées dans `.env.template`, jamais des secrets réels.

## Vérification attendue

- Tests unitaires de sérialisation des requêtes/réponses Papi, correspondance des statuts, erreurs, idempotence et validation de signature (signature valide, invalide, malformée et horodatage hors tolérance).
- Tests d’intégration avec WireMock pour initiation, lecture, erreurs Papi et callback, plus persistance/migration.
- Régression des tests MVola/Airtel et validation de la documentation OpenAPI.

## Sources

- [Guide de l’API de paiement Papi](https://docs.papi.mg/fr/docs/developper-guide/payment-api/)
- [Référence — initier un paiement](https://docs.papi.mg/fr/docs/api/direct-mobile-money/initiate-payment/)
- [Référence — lire un paiement](https://docs.papi.mg/fr/docs/api/direct-mobile-money/get-payment/)
- [Sécuriser les notifications Papi](https://docs.papi.mg/fr/docs/developper-guide/securing-notifications/)
