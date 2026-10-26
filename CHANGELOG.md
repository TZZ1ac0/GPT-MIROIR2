# Changelog

## [1.1.0] - 2026-10-26

### Ajouté
- Ajoute au menu une commande « Arrière » / « Avant » pour basculer entre les caméras avant et arrière dans CameraX et RootEncoder.
- Conserve l’instance RootEncoder pendant le changement de caméra ; les tests sur appareil confirment que la continuité de la session RTSP est assurée.


## [1.0.7] - 2026-10-26

### Modifié
- Centre verticalement les trois éléments de la barre de titre et affiche « no RTSP » lorsque le serveur RTSP n’est pas actif.

## [1.0.6] - 2026-10-25

### Corrigé
- Supprime le callback de statistiques indisponible dans RootEncoder 2.7.2 afin de rétablir la compilation.

## [1.0.5] - 2026-10-25

### Corrigé
- Aligne RootEncoder sur la version 2.7.2 utilisée par RTSP-Server 1.4.1 afin d’éviter le `NoSuchMethodError` lors de la connexion d’un client RTSP.

## [1.0.4] - 2026-10-25

### Corrigé
- Évite de recréer l’aperçu RootEncoder lors du simple basculement RTSP et empêche les appels répétés à `startStream()` lorsque le flux est déjà actif.

## [1.0.3] - 2026-10-25

### Corrigé
- Prépare explicitement l’encodeur audio sans source audio afin d’éviter l’échec `AudioEncoder not prepared yet` au démarrage RTSP.
- Sépare l’activation RTSP du cycle de vie de l’aperçu pour éviter de libérer puis réutiliser la même instance RootEncoder lors du changement de l’option.

## [1.0.2] - 2026-10-25

### Corrigé
- Maintient l’aperçu local RootEncoder disponible si le démarrage du serveur RTSP échoue, journalise les erreurs de démarrage et synchronise la surface d’aperçu avec le rendu GL.

## [1.0.1] - 2026-10-25

### Corrigé
- Remplace RTSP-Server 1.4.2 par 1.4.1, car la version 1.4.2 exige compileSdk 37 alors que le projet utilise compileSdk 36 ; conserve ainsi la compatibilité avec la configuration Android actuelle.

## [1.0.0] - 2026-10-25

### Ajouté
- Ajoute un serveur RTSP facultatif en mode RootEncoder, avec port configurable, commande VLC indicative et état visible dans la barre de titre lorsque le serveur démarre.

Les changements notables du projet sont documentés ici. Le format suit les principes de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le versionnement [SemVer](https://semver.org/lang/fr/).

## [0.21.0] - 2026-10-25

### Amélioré
- Centre au milieu de l’écran le message initial d’autorisation de la caméra et son bouton.

## [0.20.0] - 2026-10-25

### Amélioré
- Réduit la hauteur de la barre de titre tout en conservant le titre et le menu visibles.
- Agrandit la zone d’aperçu en réduisant les marges et en faisant occuper à RootEncoder tout l’espace disponible.

## [0.19.0] - 2026-10-25

### Corrigé
- CameraX et RootEncoder : mémorisent le niveau de zoom et le centre du recadrage lors d’une recréation de l’activité, notamment pendant la rotation du smartphone. CameraX conserve le panoramique sous forme normalisée pour l’adapter aux nouvelles dimensions de l’aperçu.

## [0.18.0] - 2026-10-25

### Corrigé
- RootEncoder : mémorise le niveau de zoom et le centre du recadrage lors d’une recréation de l’activité, notamment pendant la rotation du smartphone.

## [0.17.0] - 2026-10-25

### Corrigé
- RootEncoder : déplace zoom et panoramique dans le même filtre OpenGL ; le recadrage UV reste dans une fenêtre valide pour éviter les bandes striées aux bords.

## [0.16.0] - 2026-10-25

### Ajouté
- RootEncoder : ajoute le panoramique gestuel dans le pipeline OpenGL de rendu, en amont de l’aperçu et de l’encodage, avec limites liées au zoom ; le double tap réinitialise le zoom et le panoramique.

## [0.15.0] - 2026-10-24

### Modifié
- RootEncoder : le pincement et le double tap pilotent désormais le zoom de la caméra via CameraX, au lieu de transformer la vue d’aperçu ; le zoom s’applique ainsi à l’image capturée et au flux encodé. Le panoramique gestuel est volontairement laissé de côté dans cette première étape afin de valider séparément le zoom sans transformation de l’aperçu.

## [0.14.0] - 2026-10-24

### Modifié
- CameraX : rend le zoom par pincement, le panoramique à deux doigts et le double tap de réinitialisation disponibles que l’option « Full » soit activée ou non.

## [0.13.0] - 2026-10-24

### Ajouté
- CameraX : ajoute le panoramique par déplacement à deux doigts en mode « Full », en complément du zoom par pincement ; le double tap réinitialise le zoom et le panoramique.

## [0.12.0] - 2026-10-24

### Ajouté
- CameraX : ajoute le zoom par pincement à deux doigts lorsque l’option « Full » est activée, avec respect des limites de zoom de la caméra ; un double tap réinitialise le zoom à 1×.

## [0.11.0] - 2026-10-25

### Ajouté
- RootEncoder : ajoute l’option « Full » pour demander la résolution caméra disponible la plus élevée et afficher l’aperçu sans déformation, tout en conservant le cadrage initial lorsque l’option est désactivée.

## [0.10.0] - 2026-10-24

### Ajouté
- CameraX : ajoute l’option « Full » pour afficher l’intégralité du flux d’aperçu sans recadrage, en conservant les proportions et l’exposition automatique ; le zoom revient à 1× lors du changement de mode.

## [0.9.0] - 2026-10-24

### Modifié
- Paramètres : organise les informations existantes dans l’onglet « Flux » et ajoute les onglets « CameraX » et « RootEncoder », laissés vides pour l’instant.

## [0.8.0] - 2026-10-24

### Corrigé
- Caméra : conserve le mode CameraX ou RootEncoder sélectionné lors du passage du portrait au paysage et inversement.

## [0.7.0] - 2026-10-23

### Tests
- Ajoute des tests instrumentés pour l’absence d’autorisation caméra, le refus de la demande et l’acceptation de l’autorisation.
- Ajoute UI Automator pour interagir avec la boîte de dialogue système d’autorisation.

## [0.6.0] - 2026-10-23

### Tests
- Ajoute des tests d’interface Compose pour le changement de mode caméra dans le menu et l’ouverture/fermeture des paramètres.
- Ajoute les dépendances AndroidX Test nécessaires aux tests instrumentés.

## [0.5.0] - 2026-10-23

### Tests
- Ajoute des tests unitaires pour le mode CameraX par défaut, les règles d’activation du menu et les libellés des modes.

## [0.4.0] - 2026-10-23

### Modifié
- Paramètres : déplace le choix entre CameraX et RootEncoder dans le menu ; le mode actif est grisé et CameraX est sélectionné par défaut au démarrage.

## [0.3.0] - 2026-10-23

### Modifié
- Paramètres : précise l’origine logicielle des résolutions, distingue la taille de l’image affichée de l’espace disponible pour l’aperçu, et affiche la résolution de cet espace.
- Paramètres : affiche aussi la résolution native du capteur avant, lue via Camera2 (SENSOR_INFO_PIXEL_ARRAY_SIZE).
- Paramètres : présente les informations sous forme de tableau à deux colonnes, avec les rapports d’image entre crochets et un défilement si nécessaire.
- Barre supérieure : indique le mode actif (CameraX ou RootEncoder) en italique et actualise le titre lors du changement de mode.

## [0.2.3] - 2026-10-22

### Ajouté
- Interface : barre de titre contrastée, y compris l’icône d’accès au menu.
- Paramètres : affiche séparément la résolution réelle du flux vidéo et les dimensions en pixels de la zone d’image affichée.

## [0.2.2] - 2026-10-22

### Modifié
- Interface : suppression des compteurs d’images, statuts caméra et textes de diagnostic affichés dans la fenêtre principale.
- Caméra : conservation du parcours d’aperçu RootEncoder ; retrait uniquement de ses statuts et messages de diagnostic affichés.
- Logcat : conservation uniquement d’une trace au niveau erreur en cas d’échec de démarrage de la caméra.

## [0.2.1] - 2026-10-22

### Modifié
- Logcat : suppression de tous les appels de journalisation de l’application, y compris les messages MiroirCameraDiag.
- Barre supérieure : titre « Miroir » et menu à trois points verticaux avec l’action « Quitter ».

## [0.2.0] - 2026-10-14

### Ajouté
- Barre de titre permanente « Miroir Android. ».
- Menu à trois points verticaux avec l'action « Quitter ».
- Publication de l'APK installable dans les artefacts de la GitHub Release.

## [0.1.0] - 2026-10-14

### Ajouté
- Fondation Android Kotlin / Jetpack Compose.
- Écran « Hello World! » et tests unitaires.
