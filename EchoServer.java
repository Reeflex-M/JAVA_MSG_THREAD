import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.nio.file.*;

public class EchoServer {
    private static final int PORT = 1234;
    private static final String MESSAGES_FILE = "messages_offline.txt";

    // Utilisateurs connectés : nom -> ClientHandler
    static ConcurrentHashMap<String, ClientHandler> utilisateursConnectes = new ConcurrentHashMap<>();

    // Messages pour utilisateurs hors ligne : nom -> liste de messages
    private static ConcurrentHashMap<String, ArrayList<String>> messagesHorsLigne = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        chargerMessagesHorsLigne();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);
            System.out.println("En attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion de " + clientSocket.getInetAddress());

                // Nouveau thread pour chaque client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    // Ajouter un utilisateur connecté
    public static void ajouterUtilisateur(String nom, ClientHandler handler) {
        utilisateursConnectes.put(nom, handler);
        System.out.println("Connecté : " + nom + " (Total: " + utilisateursConnectes.size() + ")");

        // Informer les autres utilisateurs
        diffuserMessage("SYSTÈME", nom + " s'est connecté", nom);

        // Envoyer messages hors ligne s'il y en a
        envoyerMessagesHorsLigne(nom);
    }

    // Retirer un utilisateur connecté
    public static void retirerUtilisateur(String nom) {
        utilisateursConnectes.remove(nom);
        System.out.println("Déconnecté : " + nom + " (Total: " + utilisateursConnectes.size() + ")");

        // Informer les autres utilisateurs
        diffuserMessage("SYSTÈME", nom + " s'est déconnecté", nom);
    }

    // Envoyer message à tous sauf l'expéditeur
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
    }

    // Envoyer message privé
    public static void envoyerMessagePrive(String expediteur, String destinataire, String message) {
        String messageComplet = "[PRIVÉ] " + expediteur + " → " + destinataire + " : " + message;

        ClientHandler handlerDestinataire = utilisateursConnectes.get(destinataire);
        ClientHandler handlerExpediteur = utilisateursConnectes.get(expediteur);

        if (handlerDestinataire != null) {
            // Utilisateur connecté
            handlerDestinataire.envoyerMessage(messageComplet);
            if (handlerExpediteur != null) {
                handlerExpediteur.envoyerMessage(messageComplet);
            }
        } else {
            // Utilisateur hors ligne
            stockerMessageHorsLigne(destinataire, messageComplet);
            if (handlerExpediteur != null) {
                handlerExpediteur.envoyerMessage("Message stocké pour " + destinataire + " (hors ligne)");
            }
        }
    }

    // Stocker message pour utilisateur hors ligne
    private static void stockerMessageHorsLigne(String destinataire, String message) {
        messagesHorsLigne.computeIfAbsent(destinataire, k -> new ArrayList<>()).add(message);
        sauvegarderMessagesHorsLigne();
    }

    // Envoyer messages hors ligne à l'utilisateur qui se connecte
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

                // Supprimer après envoi
                messagesHorsLigne.remove(nom);
                sauvegarderMessagesHorsLigne();
            }
        }
    }

    // Liste des utilisateurs connectés
    public static String obtenirListeUtilisateurs() {
        if (utilisateursConnectes.isEmpty()) {
            return "Aucun utilisateur connecté";
        }

        StringBuilder liste = new StringBuilder("Utilisateurs connectés : ");
        for (String nom : utilisateursConnectes.keySet()) {
            liste.append(nom).append(", ");
        }
        return liste.substring(0, liste.length() - 2);
    }

    // Charger messages depuis fichier
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
            System.err.println("Erreur chargement messages : " + e.getMessage());
        }
    }

    // Sauvegarder messages dans fichier
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
            System.err.println("Erreur sauvegarde messages : " + e.getMessage());
        }
    }
}

// Gestion d'un client dans un thread séparé
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

            // Demander nom utilisateur
            out.println("Entrez votre nom d'utilisateur :");
            nomUtilisateur = in.readLine();

            if (nomUtilisateur == null || nomUtilisateur.trim().isEmpty()) {
                out.println("Nom invalide. Connexion fermée.");
                return;
            }

            // Vérifier si déjà connecté
            if (EchoServer.utilisateursConnectes.containsKey(nomUtilisateur)) {
                out.println("Utilisateur déjà connecté. Connexion refusée.");
                return;
            }

            // Ajouter utilisateur et afficher aide
            EchoServer.ajouterUtilisateur(nomUtilisateur, this);
            out.println("Bienvenue " + nomUtilisateur + " !");
            out.println("Commandes :");
            out.println("- /list : voir utilisateurs connectés");
            out.println("- /msg <utilisateur> <message> : message privé");
            out.println("- /bye : quitter");
            out.println("Tapez votre message pour l'envoyer à tous.");

            // Écouter messages
            String message;
            while ((message = in.readLine()) != null) {
                traiterMessage(message);
            }

        } catch (IOException e) {
            System.out.println("Erreur client " + nomUtilisateur + " : " + e.getMessage());
        } finally {
            if (nomUtilisateur != null) {
                EchoServer.retirerUtilisateur(nomUtilisateur);
            }
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Erreur fermeture : " + e.getMessage());
            }
        }
    }

    // Traiter message reçu
    private void traiterMessage(String message) {
        if (message.startsWith("/")) {
            traiterCommande(message);
        } else {
            EchoServer.diffuserMessage(nomUtilisateur, message, nomUtilisateur);
        }
    }

    // Traiter commandes spéciales
    private void traiterCommande(String commande) {
        String[] parties = commande.split(" ", 3);

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
                    System.err.println("Erreur fermeture : " + e.getMessage());
                }
                break;

            default:
                out.println("Commande inconnue : " + parties[0]);
                break;
        }
    }

    // Envoyer message à ce client
    public void envoyerMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
