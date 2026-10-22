# Changelog

Les changements notables du projet sont documentés ici. Le format suit les principes de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le versionnement [SemVer](https://semver.org/lang/fr/).

## [Non publié]

## [0.2.2] - 2026-10-22

### Modifié
- Interface : suppression des compteurs d’images, statuts caméra et textes de diagnostic affichés dans la fenêtre principale.
- Caméra : retrait du parcours d’aperçu expérimental RootEncoder de l’interface principale.
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
