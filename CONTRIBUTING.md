# Contribuer

## Flux de travail

1. Créer une branche dédiée : `feat/...`, `fix/...`, `test/...` ou `docs/...`.
2. Réaliser une modification ciblée et ajouter/adapter les tests.
3. Utiliser des commits [Conventional Commits](https://www.conventionalcommits.org/).
4. Ouvrir une pull request vers `main` avec le but, les changements et les vérifications.
5. Attendre la réussite de la CI, puis demander la revue fonctionnelle. La fusion est réservée au mainteneur.
6. Chaque PR qui modifie l’application ou sa configuration de build doit augmenter `VERSION_NAME` dans `gradle.properties` et ajouter une section datée correspondante dans `CHANGELOG.md`. La CI vérifie ces deux points et bloque la PR si la version n'augmente pas. Les changements purement documentaires ou CI ne nécessitent pas de nouvelle release.
7. Après fusion, le workflow de publication crée le tag, la GitHub Release et l'APK installable. Si la version existe déjà pour un autre commit, la publication est ignorée ou échoue selon le cas ; il faut alors corriger le numéro de version, pas réutiliser le tag.

## Vérifications locales

Avec JDK 17 et Gradle 8.9 :

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

## Versionnement

Suivre SemVer : correctif pour les corrections compatibles, mineure pour les nouvelles fonctionnalités compatibles, majeure pour les changements incompatibles. Pour toute PR modifiant l’application ou sa configuration de build, choisir un numéro strictement supérieur à la version de `main` et ajouter une section datée correspondante dans `CHANGELOG.md`. La CI contrôle ce contrat avant la fusion. Les PR purement documentaires ou CI n’ont pas à incrémenter la version.
