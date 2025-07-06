# 💬 Application de Chat Multi-Utilisateurs

Une application de chat moderne développée en Java avec des interfaces graphiques Swing, permettant la communication en temps réel entre plusieurs utilisateurs.

## 🚀 Fonctionnalités

### 📱 Interface Utilisateur

- **Interface graphique moderne** avec thème Material Design
- **Lanceur graphique** pour choisir entre serveur et client
- **Interface d'administration** du serveur avec monitoring en temps réel
- **Interface client** intuitive avec onglets et notifications

### 💬 Messagerie

- **Chat général** pour tous les utilisateurs connectés
- **Messages privés** entre utilisateurs
- **Messages hors ligne** automatiquement délivrés à la reconnexion
- **Historique des conversations** par onglets séparés

### 👥 Gestion des Utilisateurs

- **Profils utilisateurs** persistants
- **Changement de profil** en cours de session
- **Liste des utilisateurs connectés** en temps réel
- **Statut de connexion** visible pour tous

### 🗂️ Groupes de Chat

- **Création de groupes** avec nom et description
- **Gestion des membres** (ajout/suppression)
- **Système de modération** avec modérateurs
- **Messages de groupe** avec diffusion aux membres

### 🔧 Administration

- **Interface serveur** avec monitoring des clients
- **Logs en temps réel** des activités
- **Statistiques de connexion**
- **Gestion des messages hors ligne**

## 🛠️ Prérequis Techniques

### Versions Java

- **Java 11** ou supérieur
- **Maven 3.6+** pour la compilation

### Dépendances

```xml
<dependencies>
    <!-- Swing (inclus dans le JDK) -->
    <!-- JUnit pour les tests -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 📁 Arborescence des Fichiers

```
JAVA_MSG_THREAD/
├── src/main/java/com/chat/
│   ├── client/
│   │   ├── ChatClientGUI.java      # Interface graphique client
│   │   └── EchoClient.java         # Client en ligne de commande
│   ├── server/
│   │   ├── EchoServer.java         # Serveur principal
│   │   ├── ServerGUI.java          # Interface graphique serveur
│   │   └── ClientHandler.java      # Gestionnaire de clients
│   ├── launcher/
│   │   └── ChatLauncher.java       # Lanceur principal
│   └── model/
│       ├── Utilisateur.java        # Modèle utilisateur
│       ├── GroupeChatManager.java  # Gestionnaire de groupes
│       └── MessageStorage.java     # Stockage des messages
├── data/
│   ├── utilisateurs.txt            # Profils utilisateurs
│   ├── messages_offline.txt        # Messages hors ligne
│   ├── groupes_chat.txt           # Configuration des groupes
│   └── messages_history/          # Historique des messages
├── logs/                          # Logs du serveur
├── target/                        # Fichiers compilés
├── pom.xml                        # Configuration Maven
└── README.md                      # Ce fichier
```

## 🚀 Installation et Lancement

### 1. La ncement via l'interface de l'IDE

RECOMMANDEE
ServerGUI.java -> execute le serveur version graphique
PS : Il est nécéssaire de cliquer sur "demarrer", une fois la page du serveur afficher

- ChatClientGUI.java -> lance une instance d'un client

EchoServer.java -> execute le serveur dans la console
EchoClient.java -> lance une instance d'un client dans la console

## 📖 Guide d'Utilisation

### 🖥️ Côté Serveur

1. **Démarrer le serveur**

   - Lancer l'interface serveur
   - Cliquer sur "Démarrer" pour activer le serveur sur le port 1234
   - Surveiller les connexions dans l'interface

2. **Monitoring**
   - Visualiser les clients connectés en temps réel
   - Consulter les logs d'activité
   - Voir les statistiques de connexion

### 👤 Côté Client

1. **Connexion**

   - Choisir un profil utilisateur existant ou en créer un nouveau
   - Se connecter automatiquement au serveur localhost:1234

2. **Chat Général**

   - Taper un message et appuyer sur Entrée
   - Voir les messages de tous les utilisateurs connectés

3. **Messages Privés**

   - Utiliser la commande : `/msg <utilisateur> <message>`
   - Double-cliquer sur un utilisateur dans la liste pour ouvrir un onglet privé

4. **Commandes Disponibles**
   - `/list` : Afficher la liste des utilisateurs connectés
   - `/msg <utilisateur> <message>` : Envoyer un message privé
   - `/change` : Changer de profil utilisateur
   - `/bye` : Quitter le chat

### 👥 Gestion des Groupes

1. **Créer un Groupe**

   - Aller dans l'onglet "Groupes"
   - Remplir le nom et la description
   - Cliquer sur "Créer Groupe"

2. **Gérer les Membres**

   - Sélectionner un groupe
   - Ajouter/supprimer des membres
   - Promouvoir des modérateurs

3. **Commandes Groupe**
   - `/groupe <nom>` : Rejoindre un groupe
   - `/groupe_msg <nom> <message>` : Envoyer un message au groupe
   - `/groupe_list` : Lister tous les groupes

## 🔧 Comment ça Fonctionne

### Architecture Client-Serveur

1. **Serveur Central** : Gère toutes les connexions et messages
2. **Clients Multiples** : Se connectent via socket TCP/IP
3. **Communication Bidirectionnelle** : Messages temps réel
4. **Persistence** : Sauvegarde des profils et messages

### Gestion des Connexions

- **Socket TCP** sur le port 1234
- **Threads séparés** pour chaque client
- **Gestion des déconnexions** automatique
- **Reconnexion** avec récupération des messages

### Stockage des Données

- **Fichiers texte** pour la persistance
- **Format délimité** pour les données structurées
- **Création automatique** des dossiers nécessaires
- **Sauvegarde automatique** des changements
