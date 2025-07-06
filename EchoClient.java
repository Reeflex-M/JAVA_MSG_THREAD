import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.nio.file.*;

public class EchoClient {
    private static PrintWriter out;
    private static String utilisateurActuel;
    private static ArrayList<Utilisateur> utilisateurs;

    public static void main(String[] args) throws Exception {
        String serverAddress = "localhost";
        int port = 1234;

        // Charger liste des utilisateurs
        utilisateurs = chargerUtilisateurs();
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // Sélectionner ou créer utilisateur
        Utilisateur utilisateurSelectionne = selectionnerUtilisateur(utilisateurs, userInput);
        utilisateurActuel = utilisateurSelectionne.getNom();

        System.out.println("Connexion au serveur de chat...");

        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connecté au serveur !");

            // Flux de communication
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer nom d'utilisateur au serveur
            String serverMessage = in.readLine(); // "Entrez votre nom d'utilisateur :"
            System.out.println(serverMessage);
            out.println(utilisateurActuel);

            System.out.println("Connecté en tant que : " + utilisateurActuel);
            System.out.println("Commandes :");
            System.out.println("- /list : voir utilisateurs connectés");
            System.out.println("- /msg <utilisateur> <message> : message privé");
            System.out.println("- /change : changer d'utilisateur");
            System.out.println("- /bye : quitter");
            System.out.println("Tapez votre message pour l'envoyer à tous.");

            // Thread de réception des messages
            demarrerThreadReception(in);

            // Boucle principale pour envoi des messages
            traiterEntreeUtilisateur(userInput, socket);

        } catch (IOException e) {
            System.err.println("Erreur connexion serveur : " + e.getMessage());
        }
    }

    // Charger utilisateurs depuis fichier
    private static ArrayList<Utilisateur> chargerUtilisateurs() throws IOException {
        ArrayList<Utilisateur> utilisateurs = new ArrayList<>();
        if (Files.exists(Paths.get("utilisateurs.txt"))) {
            for (String ligne : Files.readAllLines(Paths.get("utilisateurs.txt"))) {
                if (!ligne.trim().isEmpty()) {
                    String[] parties = ligne.split(",");
                    if (parties.length >= 1) {
                        Utilisateur user = new Utilisateur(parties[0]);
                        if (parties.length >= 2) {
                            user.setAdresseIP(parties[1]);
                        }
                        utilisateurs.add(user);
                    }
                }
            }
        }
        return utilisateurs;
    }

    // Choisir profil ou créer nouveau
    private static Utilisateur selectionnerUtilisateur(ArrayList<Utilisateur> utilisateurs, BufferedReader userInput)
            throws IOException {
        System.out.println("=== Sélection utilisateur ===");

        if (utilisateurs.isEmpty()) {
            System.out.println("Aucun utilisateur enregistré.");
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }

        // Options disponibles
        System.out.println("Choisissez votre profil :");
        for (int i = 0; i < utilisateurs.size(); i++) {
            System.out.println((i + 1) + ". " + utilisateurs.get(i).getNom());
        }
        System.out.println((utilisateurs.size() + 1) + ". Nouveau profil");
        System.out.print("Votre choix : ");

        // Traitement du choix
        int choix = Integer.parseInt(userInput.readLine()) - 1;
        if (choix == utilisateurs.size()) {
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }

        if (choix >= 0 && choix < utilisateurs.size()) {
            return utilisateurs.get(choix);
        } else {
            System.out.println("Choix invalide, nouveau profil...");
            return creerNouvelUtilisateur(utilisateurs, userInput);
        }
    }

    // Créer nouvel utilisateur
    private static Utilisateur creerNouvelUtilisateur(ArrayList<Utilisateur> utilisateurs, BufferedReader userInput)
            throws IOException {
        System.out.print("Nom d'utilisateur : ");
        String nom = userInput.readLine().trim();

        // Vérifier nom non vide
        while (nom.isEmpty()) {
            System.out.print("Le nom ne peut pas être vide. Nom : ");
            nom = userInput.readLine().trim();
        }

        // Vérifier nom unique
        for (Utilisateur u : utilisateurs) {
            if (u.getNom().equalsIgnoreCase(nom)) {
                System.out.println("Ce nom existe déjà. Choisissez-en un autre.");
                return creerNouvelUtilisateur(utilisateurs, userInput);
            }
        }

        Utilisateur nouvelUtilisateur = new Utilisateur(nom);
        nouvelUtilisateur.setAdresseIP("localhost");
        utilisateurs.add(nouvelUtilisateur);

        // Sauvegarde
        sauvegarderUtilisateurs(utilisateurs);
        System.out.println("Nouveau profil créé : " + nom);

        return nouvelUtilisateur;
    }

    // Sauvegarder utilisateurs dans fichier
    private static void sauvegarderUtilisateurs(ArrayList<Utilisateur> utilisateurs) throws IOException {
        ArrayList<String> lignes = new ArrayList<>();
        for (Utilisateur u : utilisateurs) {
            lignes.add(u.getNom() + "," + (u.getAdresseIP() != null ? u.getAdresseIP() : "localhost"));
        }
        Files.write(Paths.get("utilisateurs.txt"), lignes);
    }

    // Thread qui écoute messages du serveur
    private static void demarrerThreadReception(BufferedReader in) {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // Ignorer message initial de demande nom
                    if (!message.equals("Entrez votre nom d'utilisateur :")) {
                        System.out.println(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connexion serveur perdue.");
                System.exit(0);
            }
        }).start();
    }

    // Gérer entrée utilisateur et commandes
    private static void traiterEntreeUtilisateur(BufferedReader userInput, Socket socket) throws IOException {
        String input;
        while ((input = userInput.readLine()) != null) {
            if (input.equals("/change")) {
                // Changer d'utilisateur
                changerUtilisateur(userInput, socket);
            } else if (input.equals("/bye")) {
                // Quitter
                out.println("/bye");
                System.out.println("Déconnexion...");
                break;
            } else {
                // Envoyer message au serveur
                out.println(input);
            }
        }
    }

    // Changer de profil en cours de session
    private static void changerUtilisateur(BufferedReader userInput, Socket socket) throws IOException {
        System.out.println("=== Changement utilisateur ===");

        // Recharger liste utilisateurs
        utilisateurs = chargerUtilisateurs();

        // Sélectionner nouvel utilisateur
        Utilisateur nouvelUtilisateur = selectionnerUtilisateur(utilisateurs, userInput);
        String ancienUtilisateur = utilisateurActuel;
        utilisateurActuel = nouvelUtilisateur.getNom();

        // Fermer connexion actuelle
        socket.close();

        try {
            // Nouvelle connexion
            Socket newSocket = new Socket("localhost", 1234);
            BufferedReader newIn = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));
            out = new PrintWriter(newSocket.getOutputStream(), true);

            // Authentification avec nouveau nom
            String serverMessage = newIn.readLine(); // "Entrez votre nom d'utilisateur :"
            out.println(utilisateurActuel);

            System.out.println("Changement réussi ! Maintenant connecté : " + utilisateurActuel);
            System.out.println("(Précédemment : " + ancienUtilisateur + ")");

            // Redémarrer threads
            demarrerThreadReception(newIn);
            traiterEntreeUtilisateur(userInput, newSocket);

        } catch (IOException e) {
            System.err.println("Erreur changement utilisateur : " + e.getMessage());
            System.exit(1);
        }
    }
}
