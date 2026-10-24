# Changelog

Les changements notables du projet sont documentés ici. Le format suit les principes de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le versionnement [SemVer](https://semver.org/lang/fr/).

## [0.13.0] - 2026-10-24

### Ajouté
- CameraX : ajoute le panoramique par déplacement à deux doigts en mode « Full », en complément du zoom par pincement ; le double tap réinitialise le zoom et le panoramique.

## [0.12.0] - 2026-10-24

### Ajouté
- CameraX : ajoute le zoom par pincement à deux doigts lorsque l’option « Full » est activée, avec respect des limites de zoom de la caméra ; un double tap réinitialise le zoom à 1×.

## [0.11.0] - 2026-10-24

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
- Interface : déplace le choix entre CameraX et RootEncoder dans le menu ; le mode actif est grisé et CameraX est sélectionné par défaut au démarrage.


## [0.3.0] - 2026-10-23

### Modifié
- Paramètres : précise l’origine logicielle des résolutions, distingue la taille de l’image affichée de l’espace disponible pour l’aperçu, et affiche la résolution de cet espace.
- Paramètres : affiche aussi la résolution native du capteur avant, lue via Camera2 (`SENSOR_INFO_PIXEL_ARRAY_SIZE`).
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
- Logcat : suppression de tous les appels de journalisation de l’application, y compris les messages `MiroirCameraDiag`.
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
- CI GitHub Actions, badges, documentation et automatisation des releases.
