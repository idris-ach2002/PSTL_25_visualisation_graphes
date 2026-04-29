# PSTL_25_visualisation_graphes

Application de visualisation de graphes avec interface JavaFX, rendu OpenGL et calculs natifs en C.

## À quoi sert le projet

L'application permet de :
- charger un graphe depuis un fichier CSV ou DOT ;
- visualiser le graphe de manière interactive ;
- appliquer des layouts et des algorithmes de communautés ;
- exporter des rendus graphiques.

Le projet est composé de deux parties :
- `graph-ui` : interface graphique Java ;
- `graph-native` : moteur natif en C chargé via JNI.

## Prérequis

À installer sur la machine :
- **Java JDK 21**
- **GCC**
- **Make**
- une connexion Internet au premier lancement Maven pour télécharger les dépendances Java

## Lancement rapide

Depuis la racine du projet :

```bash
cd graph-application
bash run-app.sh
```

Ce script :
1. compile la bibliothèque native C ;
2. lance l'application JavaFX avec Maven.

## Premier test

Pour vérifier que tout fonctionne :

1. lancer l'application
2. selectionner un fichier csv proposer
3. lancer le rendu du graphe

## Lancement manuel

Si vous préférez lancer le projet étape par étape :

### 1. Compiler la partie native

```bash
cd graph-application/graph-native
make clean
make
```

### 2. Lancer l'interface Java

```bash
cd ../graph-ui
../tools/apache-maven-3.9.6/bin/mvn javafx:run
```

## Structure utile du projet

```text
graph-application/
├── graph-native/     # code C / JNI
├── graph-ui/         # interface JavaFX
│   └── samples/      # exemples de graphes
├── tools/            # Maven fourni avec le projet
└── run-app.sh        # script de lancement
```

## Fichiers d'exemple

Des fichiers de test sont déjà fournis dans `graph-ui/samples/`.

Exemples :
- `iris.csv`
- `one.dot`
- `predicancerNUadd9239.csv`

## Problèmes fréquents

### `java not found in PATH`
Java 21 n'est pas installé ou n'est pas accessible depuis le terminal.

### Erreur liée à `JAVA_HOME`
Définir la variable d'environnement `JAVA_HOME` si nécessaire.

Exemple Linux :

```bash
export JAVA_HOME=/chemin/vers/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

### `UnsatisfiedLinkError`
La bibliothèque native n'a pas été compilée correctement. Relancer :

```bash
cd graph-application/graph-native
make clean
make
```

## Conseils

- utiliser de préférence **le script `run-app.sh`** ;
- lancer le projet depuis un terminal ;
- tester d'abord avec un fichier du dossier `samples` avant d'utiliser vos propres données.

## Auteurs

Ce fork a été réalisé dans le cadre du PSTL par Idris Achabou et Bilal Chetouani. Le code est un fork de https://github.com/damrib/PSTL_25_visualisation_graphes 