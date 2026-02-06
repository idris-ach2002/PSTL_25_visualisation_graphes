# PSTL_25_visualisation_graphes


Application de **visualisation de graphes** utilisant :

- **Java / JavaFX** : interface graphique  
- **JOGL (OpenGL)** : rendu graphique accéléré  
- **C / JNI** : calculs lourds (clustering, layout)

Le projet est découpé en deux modules :

- `graph-native` : code C / JNI compilé en bibliothèque partagée  
- `graph-ui`     : application Java (JavaFX + JOGL)

## Prérequis

- Java JDK 21
- Maven 3.9+
- GCC >= 9
- Make

## Installation de Java JDK 21

### Installation de Java JDK 21

- **Linux** :
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

- **macOS** : télécharger le .dmg Oracle ou utiliser Homebrew
```bash
brew install openjdk@21
```

## Compilation
1. Compiler le code natif (C / JNI) :

```bash
cd graph-native
make clean
make
```

2. Compiler le code Java :

```bash
cd ../graph-ui
mvn clean compile
```

## Exécution
Pour lancer l’application placer vous dans le dossier `graph-ui` et exécuter :

```bash
mvn javafx:run
```

Une fois le programme lancé, pour visualiser la fenêtre avec le graphe, il faut:
- Appuyer sur Ouvrir un fichier de graphe
- Sélectionner le fichier iris.csv
- Appuyer sur next
- Sélectionner une mesure de similarité (corrélation, ...), puis appuyer sur next
- Appuyer à nouveau sur next une fois que la nouvelle scène est apparue
- Choisir un algorithme de communauté ou appuyer directement sur next
- Appuyer sur démarrer

## Structure du projet

# ATTENTION: LE PROJET EST EN COURS DE MODIFICATION, LA STRUCTURE VA GRANDEMENT CHANGER !

```
graph-application/
├── graph-native/          # Code natif (C / JNI)
│   ├── c/
│   │   ├── c_graph/
│   │   ├── concurrent/
│   │   ├── debug/
│   │   ├── pretraitement/
│   │   └── forceatlasV4_CSV.c
│   ├── out/
│   │   └── libnative.so ou libnative.dll ou libnative.dylib
│   └── Makefile
│
├── graph-ui/              # Application Java
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   ├── samples/           # Fichiers CSV de test
│   ├── pom.xml
│   └── target/
│
├── tools/                 # Outils fournis (Maven local si besoin)
└── README.md
```

## Dépendances

- **Java / Maven** : gérées uniquement via Maven
- **JavaFX 21** : modules `javafx-controls` et `javafx-fxml`
- **JOGL 2.4.0** + **GlueGen 2.4.0**

> Les artefacts JOGL ne sont pas disponibles sur Maven Central.  
> Dépôt utilisé : [https://jogamp.org/deployment/maven](https://jogamp.org/deployment/maven)

- **Code natif (JNI)** : compilé en bibliothèque partagée  
  - Linux : `libnative.so`  
  - Windows : `libnative.dll`  
  - macOS : `libnative.dylib`

Chargement dans Java via :

```java
System.loadLibrary("native");
```

Et la JVM doit connaître le chemin :

```
-Djava.library.path=../graph-native/out
```

## JavaFX + JOGL

JOGL utilise des API internes de JavaFX. Avec Java 21, il faut ouvrir explicitement certains modules dans le pom.xml :

```
--add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED
--add-opens javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED
--add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED
--add-opens javafx.graphics/javafx.stage=ALL-UNNAMED
```

Sans ces options, l’erreur suivante apparaît :

```
Error getting Window handle
```

## Fichiers d’entrée (CSV)

- `graph-ui/samples/iris.csv`

- `graph-ui/samples/predicancerNUadd9239.csv`

Ces fichiers peuvent être sélectionnés depuis l’interface graphique.

## Problèmes connus

### 1. `UnsatisfiedLinkError`  
Problème de chargement de la bibliothèque native (JNI) :

- Vérifier que `libnative.so` (Linux) / `libnative.dll` (Windows) / `libnative.dylib` (macOS) existe  
- Vérifier que `java.library.path` pointe vers le dossier contenant la bibliothèque

### 2. Erreurs JOGL / JavaFX

- Vérifier que Java >= 21  
- **Toujours utiliser `mvn javafx:run`**  
- **Ne pas lancer l’application directement avec `java`**

## Auteurs
A COMPLÉTER

---