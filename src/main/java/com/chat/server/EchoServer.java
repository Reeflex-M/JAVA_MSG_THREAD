package com.chat.server;

import com.chat.model.GroupeChatManager;
import com.chat.model.MessageStorage;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.nio.file.*;

public class EchoServer {
    private static final int PORT = 1234;
    private static final String MESSAGES_FILE = "data/messages_offline.txt";

    // On stocke les utilisateurs connectés : nom -> ClientHandler
    static ConcurrentHashMap<String, ClientHandler> utilisateursConnectes = new ConcurrentHashMap<>();

    // Messages pour les gens qui sont pas connectés : nom -> liste de messages
    private static ConcurrentHashMap<String, ArrayList<String>> messagesHorsLigne = new ConcurrentHashMap<>();

    // Référence vers l'interface graphique pour la mettre à jour
    private static ServerGUI serverGUI;

    public static void main(String[] args) {
        chargerMessagesHorsLigne();

        // Charger les groupes de chat au démarrage
        GroupeChatManager.chargerGroupes();
        System.out.println("Groupes de chat chargés");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);
            System.out.println("J'attends des connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion depuis " + clientSocket.getInetAddress());

                // Chaque client a son propre thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Problème avec le serveur : " + e.getMessage());
        }
    }

    // Méthode pour associer l'interface graphique
    public static void setServerGUI(ServerGUI gui) {
        serverGUI = gui;
    }

    // Quand quelqu'un se connecte
    public static void ajouterUtilisateur(String nom, ClientHandler handler) {
        utilisateursConnectes.put(nom, handler);
        System.out.println("Connecté : " + nom + " (Total: " + utilisateursConnectes.size() + ")");

        // On prévient les autres
        diffuserMessage("SYSTÈME", nom + " vient de se connecter", nom);

        // S'il y a des messages en attente, on les envoie
        envoyerMessagesHorsLigne(nom);

        // Notifier l'interface graphique
        if (serverGUI != null) {
            serverGUI.notifierClientConnecte(nom);
        }
    }

    // Quand quelqu'un se déconnecte
    public static void retirerUtilisateur(String nom) {
        utilisateursConnectes.remove(nom);
        System.out.println("Déconnecté : " + nom + " (Total: " + utilisateursConnectes.size() + ")");

        // On prévient les autres
        diffuserMessage("SYSTÈME", nom + " s'est déconnecté", nom);

        // Notifier l'interface graphique
        if (serverGUI != null) {
            serverGUI.notifierClientDeconnecte(nom);
        }
    }

    // Envoie un message à tout le monde sauf à celui qui l'a envoyé
    public static void diffuserMessage(String expediteur, String message, String excluUtilisateur) {
        String messageComplet = expediteur + " : " + message;

        for (String nom : utilisateursConnectes.keySet()) {
            if (!nom.equals(excluUtilisateur)) {
                ClientHandler handler = utilisateursConnectes.get(nom);
                if (handler != null) {
                    handler.envoyerMessage(messageComplet);
                }
            }
        }

        // Notifier l'interface graphique du message
        if (serverGUI != null) {
            serverGUI.notifierMessage(expediteur, message);
        }
    }

    // Envoie un message privé entre deux personnes
    public static void envoyerMessagePrive(String expediteur, String destinataire, String message) {
        String messageComplet = "[PRIVÉ] " + expediteur + " → " + destinataire + " : " + message;

        ClientHandler handlerDestinataire = utilisateursConnectes.get(destinataire);
        ClientHandler handlerExpediteur = utilisateursConnectes.get(expediteur);

        if (handlerDestinataire != null) {
            // La personne est connectée
            handlerDestinataire.envoyerMessage(messageComplet);
            if (handlerExpediteur != null) {
                handlerExpediteur.envoyerMessage(messageComplet);
            }
        } else {
            // La personne est pas là, on stocke le message
            stockerMessageHorsLigne(destinataire, messageComplet);
            if (handlerExpediteur != null) {
                handlerExpediteur.envoyerMessage("Message stocké pour " + destinataire + " (hors ligne)");
            }
        }

        // Notifier l'interface graphique du message privé
        if (serverGUI != null) {
            serverGUI.notifierMessagePrive(expediteur, destinataire, message);
        }
    }

    // Stocke un message pour quelqu'un qui n'est pas connecté
    private static void stockerMessageHorsLigne(String destinataire, String message) {
        messagesHorsLigne.computeIfAbsent(destinataire, k -> new ArrayList<>()).add(message);
        sauvegarderMessagesHorsLigne();
    }

    // Envoie les messages en attente quand quelqu'un se connecte
    private static void envoyerMessagesHorsLigne(String nom) {
        ArrayList<String> messages = messagesHorsLigne.get(nom);
        if (messages != null && !messages.isEmpty()) {
            ClientHandler handler = utilisateursConnectes.get(nom);
            if (handler != null) {
                handler.envoyerMessage("=== " + messages.size() + " message(s) en attente ===");
                for (String message : messages) {
                    handler.envoyerMessage(message);
                }
                handler.envoyerMessage("=== Fin des messages ===");

                // Une fois envoyés, on les supprime
                messagesHorsLigne.remove(nom);
                sauvegarderMessagesHorsLigne();
            }
        }
    }

    // Donne la liste des gens connectés
    public static String obtenirListeUtilisateurs() {
        if (utilisateursConnectes.isEmpty()) {
            return "Personne n'est connecté pour le moment";
        }

        StringBuilder liste = new StringBuilder("Utilisateurs connectés : ");
        for (String nom : utilisateursConnectes.keySet()) {
            liste.append(nom).append(", ");
        }
        return liste.substring(0, liste.length() - 2);
    }

    // Charge les messages depuis le fichier au démarrage
    private static void chargerMessagesHorsLigne() {
        try {
            if (Files.exists(Paths.get(MESSAGES_FILE))) {
                for (String ligne : Files.readAllLines(Paths.get(MESSAGES_FILE))) {
                    if (!ligne.trim().isEmpty()) {
                        String[] parties = ligne.split("\\|", 2);
                        if (parties.length == 2) {
                            String destinataire = parties[0];
                            String message = parties[1];
                            messagesHorsLigne.computeIfAbsent(destinataire, k -> new ArrayList<>()).add(message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Problème pour charger les messages : " + e.getMessage());
        }
    }

    // Sauvegarde les messages dans le fichier
    private static void sauvegarderMessagesHorsLigne() {
        try {
            ArrayList<String> lignes = new ArrayList<>();
            for (String destinataire : messagesHorsLigne.keySet()) {
                for (String message : messagesHorsLigne.get(destinataire)) {
                    lignes.add(destinataire + "|" + message);
                }
            }
            Files.write(Paths.get(MESSAGES_FILE), lignes);
        } catch (IOException e) {
            System.err.println("Problème pour sauvegarder les messages : " + e.getMessage());
        }
    }
}

// Gère un client dans son propre thread
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nomUtilisateur;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // On demande le nom d'utilisateur
            out.println("Entrez votre nom d'utilisateur :");
            nomUtilisateur = in.readLine();

            if (nomUtilisateur == null || nomUtilisateur.trim().isEmpty()) {
                out.println("Nom pas valide. Connexion fermée.");
                return;
            }

            // On vérifie si le nom est déjà pris
            if (EchoServer.utilisateursConnectes.containsKey(nomUtilisateur)) {
                out.println("Ce nom d'utilisateur est déjà pris. Connexion refusée.");
                return;
            }

            // On ajoute l'utilisateur et on lui montre les commandes
            EchoServer.ajouterUtilisateur(nomUtilisateur, this);
            out.println("Bienvenue " + nomUtilisateur + " !");
            out.println("Commandes disponibles :");
            out.println("- /list : voir qui est connecté");
            out.println("- /msg <utilisateur> <message> : envoyer un message privé");
            out.println("- /bye : quitter le chat");
            out.println("COMMANDES GROUPES :");
            out.println("- /groupe-creer <nom> <description> : créer un groupe");
            out.println("- /groupe-liste : voir tous les groupes");
            out.println("- /groupe-membres <nom> : voir les membres d'un groupe");
            out.println("- /groupe-ajouter <nom> <utilisateur> : ajouter un utilisateur au groupe");
            out.println("- /groupe-supprimer <nom> <utilisateur> : supprimer un utilisateur du groupe");
            out.println("- /groupe-msg <nom> <message> : envoyer un message au groupe");
            out.println("- /mes-groupes : voir mes groupes");
            out.println("Tapez simplement votre message pour l'envoyer à tout le monde.");

            // On écoute les messages
            String message;
            while ((message = in.readLine()) != null) {
                traiterMessage(message);
            }

        } catch (IOException e) {
            System.out.println("Problème avec le client " + nomUtilisateur + " : " + e.getMessage());
        } finally {
            if (nomUtilisateur != null) {
                EchoServer.retirerUtilisateur(nomUtilisateur);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Problème pour fermer la connexion : " + e.getMessage());
            }
        }
    }

    // Traite un message reçu
    private void traiterMessage(String message) {
        if (message.startsWith("/")) {
            traiterCommande(message);
        } else {
            EchoServer.diffuserMessage(nomUtilisateur, message, nomUtilisateur);
        }
    }

    // Traite les commandes spéciales
    private void traiterCommande(String commande) {
        String[] parties = commande.split(" ", 4);

        switch (parties[0].toLowerCase()) {
            case "/list":
                out.println(EchoServer.obtenirListeUtilisateurs());
                break;

            case "/msg":
                if (parties.length >= 3) {
                    String destinataire = parties[1];
                    String message = parties[2];
                    EchoServer.envoyerMessagePrive(nomUtilisateur, destinataire, message);
                } else {
                    out.println("Usage : /msg <utilisateur> <message>");
                }
                break;

            case "/bye":
                out.println("Au revoir " + nomUtilisateur + " !");
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Problème pour fermer la connexion : " + e.getMessage());
                }
                break;

            // Commandes pour les groupes
            case "/groupe-creer":
                if (parties.length >= 3) {
                    String nomGroupe = parties[1];
                    String description = parties.length >= 4 ? parties[2] + " " + parties[3] : parties[2];
                    boolean succes = GroupeChatManager.creerGroupe(nomGroupe, nomUtilisateur, description);
                    if (succes) {
                        out.println("Groupe '" + nomGroupe
                                + "' créé avec succès ! Vous êtes maintenant le créateur et modérateur.");
                    } else {
                        out.println("Erreur : Un groupe avec ce nom existe déjà.");
                    }
                } else {
                    out.println("Usage : /groupe-creer <nom> <description>");
                }
                break;

            case "/groupe-liste":
                if (GroupeChatManager.obtenirTousGroupes().isEmpty()) {
                    out.println("Aucun groupe n'a été créé pour le moment.");
                } else {
                    out.println("=== LISTE DES GROUPES ===");
                    for (String nomGroupe : GroupeChatManager.obtenirTousGroupes().keySet()) {
                        GroupeChatManager.GroupeChat groupe = GroupeChatManager.obtenirGroupe(nomGroupe);
                        out.println("- " + nomGroupe + " (" + groupe.getMembres().size() + " membres) - "
                                + groupe.getDescription());
                    }
                }
                break;

            case "/groupe-membres":
                if (parties.length >= 2) {
                    String nomGroupe = parties[1];
                    GroupeChatManager.GroupeChat groupe = GroupeChatManager.obtenirGroupe(nomGroupe);
                    if (groupe == null) {
                        out.println("Le groupe '" + nomGroupe + "' n'existe pas.");
                    } else {
                        out.println("=== MEMBRES DU GROUPE '" + nomGroupe + "' ===");
                        out.println("Créateur : " + groupe.getCreateur());
                        out.println("Modérateurs : " + String.join(", ", groupe.getModerateurs()));
                        out.println("Membres : " + String.join(", ", groupe.getMembres()));
                        out.println("Total : " + groupe.getMembres().size() + " membres");
                    }
                } else {
                    out.println("Usage : /groupe-membres <nom>");
                }
                break;

            case "/groupe-ajouter":
                if (parties.length >= 3) {
                    String nomGroupe = parties[1];
                    String utilisateur = parties[2];
                    String resultat = GroupeChatManager.ajouterUtilisateurAuGroupe(nomGroupe, utilisateur,
                            nomUtilisateur);
                    out.println(resultat);
                } else {
                    out.println("Usage : /groupe-ajouter <nom> <utilisateur>");
                }
                break;

            case "/groupe-supprimer":
                if (parties.length >= 3) {
                    String nomGroupe = parties[1];
                    String utilisateur = parties[2];
                    String resultat = GroupeChatManager.supprimerUtilisateurDuGroupe(nomGroupe, utilisateur,
                            nomUtilisateur);
                    out.println(resultat);
                } else {
                    out.println("Usage : /groupe-supprimer <nom> <utilisateur>");
                }
                break;

            case "/groupe-msg":
                if (parties.length >= 3) {
                    String nomGroupe = parties[1];
                    String message = parties[2];
                    if (parties.length >= 4) {
                        message += " " + parties[3];
                    }
                    GroupeChatManager.diffuserMessageGroupe(nomGroupe, nomUtilisateur, message, EchoServer.class);
                } else {
                    out.println("Usage : /groupe-msg <nom> <message>");
                }
                break;

            case "/mes-groupes":
                java.util.List<String> mesGroupes = GroupeChatManager.obtenirGroupesUtilisateur(nomUtilisateur);
                if (mesGroupes.isEmpty()) {
                    out.println("Vous n'êtes membre d'aucun groupe.");
                } else {
                    out.println("=== VOS GROUPES ===");
                    for (String nomGroupe : mesGroupes) {
                        GroupeChatManager.GroupeChat groupe = GroupeChatManager.obtenirGroupe(nomGroupe);
                        if (groupe != null) {
                            String role = groupe.estModerateur(nomUtilisateur) ? "(Modérateur)" : "(Membre)";
                            out.println("- " + nomGroupe + " " + role + " - " + groupe.getDescription());
                        }
                    }
                }
                break;

            default:
                out.println("Commande inconnue : " + parties[0]);
                break;
        }
    }

    // Envoie un message à ce client
    public void envoyerMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
