# PSTL_25_visualisation_graphes

Dependances Maven, Makefile, Java (openjdk-21-jdk) JavaFX:
Pour compiler et exécuter, il faut utiliser les commandes suivantes:
- cd graph-application
- make all

Pour installer la bonne version de Java sur linux:
    - sudo apt update
    - sudo apt install openjdk-21-jdk

Pour installer la bonner version de Java sur MacOs voir:
    télecharger le .dmg: https://www.oracle.com/java/technologies/downloads/#jdk21-mac
ou avec HomeBrew:
    brew install openjdk@21

Une fois le programme lancé, pour visualiser la fenêtre avec le graphe, il faut:
- Appuyer sur Ouvrir un fichier de graphe
- Sélectionner le fichier iris.csv
- Appuyer sur next
- Sélectionner une mesure de similarité (corrélation, ...), puis appuyer sur next
- Appuyer a nouveau sur next une fois que la nouvelle scène est apparue
- Choisir un algorithme de communauté ou appuyer directement sur next
- Appuyer sur démarrer

# Graph Application – Guide technique de build et d’exécution

Ce document décrit **l’architecture du projet**, les **dépendances nécessaires**, ainsi que les **étapes complètes pour compiler et exécuter l’application** (JavaFX + JOGL + JNI).
---

## 1. Vue d’ensemble du projet

Le projet est une application de **visualisation de graphes** combinant :
- **Java / JavaFX** pour l’interface graphique,
- **JOGL (OpenGL)** pour le rendu graphique accéléré,
- **C (JNI)** pour les calculs lourds (clustering, communautés, layout).

### Choix d’architecture

Le projet est volontairement découpé en **deux modules indépendants** :

- `graph-native` : compilation du code C (JNI)
- `graph-ui`     : application Java (JavaFX + JOGL)

Cette séparation permet :
- une meilleure portabilité,
- une maintenance simplifiée,
- une responsabilité claire de chaque outil (Make / Maven).

---

## 2. Structure du projet

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
│   │   └── linux/
│   │       └── libnative.so
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

---

## 3. Prérequis système

### 3.1 Système d’exploitation

- Linux (testé et validé)
- Windows : supporté (avec MinGW / MSVC, non détaillé ici)

### 3.2 Logiciels requis

| Outil | Version recommandée | Rôle |
|-----|---------------------|------|
| Java JDK | **21** | JavaFX + modules |
| Maven | **3.9+** | Build Java |
| GCC | **>= 9** | Compilation C |
| Make | standard | Build natif |

Vérification rapide :

```bash
java -version
mvn -version
gcc --version
make --version
```

---

## 4. Dépendances logicielles

### 4.1 Java / Maven

Les dépendances Java sont **exclusivement gérées par Maven** :

- JavaFX 21 (`javafx-controls`, `javafx-fxml`)
- JOGL 2.4.0
- GlueGen 2.4.0

Les artefacts JOGL ne sont **pas** disponibles sur Maven Central.
Le dépôt suivant est utilisé :

```
https://jogamp.org/deployment/maven
```

---

### 4.2 Code natif (JNI)

Le code C est compilé en une bibliothèque partagée :

- Linux   : `libnative.so`
- Windows : `libnative.dll`

Le lien JNI est réalisé via :

```java
System.loadLibrary("native");
```

La JVM est informée du chemin via :

```
-Djava.library.path=../graph-native/out/linux
```

---

## 5. Compilation du projet

### 5.1 Étape 1 — Compilation du code natif (C / JNI)

À effectuer **une seule fois**, ou après modification du code C.

```bash
cd graph-native
make clean
make
```

Vérification attendue :

```bash
ls out/linux/libnative.so
```

---

### 5.2 Étape 2 — Compilation Java

La compilation Java est entièrement gérée par Maven.

```bash
cd graph-ui
mvn clean compile
```

---

## 6. Exécution du projet

### Commande unique recommandée

```bash
cd graph-ui
mvn javafx:run
```

Cette commande :
1. Compile le code Java
2. Résout JavaFX et JOGL
3. Charge la bibliothèque JNI
4. Lance l’interface graphique

---

## 7. Gestion JavaFX + JOGL (important)

JOGL utilise des **APIs internes de JavaFX** (réflexion + pipeline Quantum).

Avec Java 21, il est nécessaire d’ouvrir explicitement certains modules.
Ces options sont déjà intégrées dans le `pom.xml` :

- `com.sun.javafx.tk`
- `com.sun.javafx.tk.quantum`
- `com.sun.glass.ui`
- `javafx.stage`

Sans ces options, l’erreur suivante apparaît :

```
Error getting Window handle
```

---

## 8. Fichiers d’entrée (CSV)

Les fichiers de données sont placés dans :

```
graph-ui/samples/
```

Exemples fournis :
- `iris.csv`
- `predicancerNUadd9239.csv`

Ces fichiers peuvent être sélectionnés depuis l’interface graphique.

---

## 9. Problèmes connus et solutions

### 9.1 Erreur `UnsatisfiedLinkError`

- Vérifier que `libnative.so` existe
- Vérifier `java.library.path`

### 9.2 Erreur JOGL / JavaFX

- Vérifier Java = 21
- Ne pas lancer l’application avec `java` directement
- Toujours utiliser `mvn javafx:run`

---

## 10. Résumé

- Make → compilation C / JNI
- Maven → Java, JavaFX, JOGL
- Architecture modulaire
- Exécution fiable sous Java 21

Ce découpage garantit :
- portabilité,
- stabilité,
- clarté pour la maintenance et l’évaluation.

---


